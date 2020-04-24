/*
 * This file is part of DrFTPD, Distributed FTP Daemon.
 *
 * DrFTPD is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * DrFTPD is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * DrFTPD; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston, MA 02111-1307 USA
 */
package org.drftpd.slave.protocol;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.drftpd.common.exceptions.AsyncResponseException;
import org.drftpd.common.exceptions.TransferDeniedException;
import org.drftpd.common.io.PermissionDeniedException;
import org.drftpd.common.io.PhysicalFile;
import org.drftpd.common.network.AsyncCommandArgument;
import org.drftpd.common.network.AsyncResponse;
import org.drftpd.common.network.PassiveConnection;
import org.drftpd.common.slave.ConnectInfo;
import org.drftpd.common.slave.LightRemoteInode;
import org.drftpd.common.slave.TransferIndex;
import org.drftpd.common.slave.TransferStatus;
import org.drftpd.slave.Slave;
import org.drftpd.slave.network.*;
import org.drftpd.slave.vfs.Root;
import org.drftpd.slave.vfs.RootCollection;
import org.drftpd.slave.vfs.RootPathContents;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Basic operations handling.
 *
 * @author fr0w
 * @author zubov
 * @author mog
 * @version $Id$
 */
public class BasicHandler extends AbstractHandler {
    private static final Logger logger = LogManager.getLogger(BasicHandler.class);
    // Vars for threaded remerge
    private static final int maxMergeThreads = 10;
    // The following variables are static as they are used to signal between
    // remerging and the pause/resume functions, due to the way the handler
    // map works these are run against separate object instances.
    private static final AtomicBoolean remergePaused = new AtomicBoolean();
    private static final Object remergeWaitObj = new Object();
    private static final Object threadMergeWaitObj = new Object();
    private static final Object threadMergeDepthWaitObj = new Object();
    private final AtomicInteger threadMergeCount = new AtomicInteger(0);
    private final ArrayList<String> threadMergeDepth = new ArrayList<>();
    private HandleRemergeRecursiveThread[] mergeThreads;
    private int remergeDepth = 0;
    private int remergeConcurrentDepth = 0;

    public BasicHandler(SlaveProtocolCentral central) {
        super(central);
    }

    @Override
    public String getProtocolName() {
        return "BasicProtocol";
    }

    /**
     * Simply delegates the request to the Slave object.
     *
     * @param path
     */
    private String mapPathToRenameQueue(String path) {
        return getSlaveObject().mapPathToRenameQueue(path);
    }

    // TODO check this.
    public AsyncResponse handleAbort(AsyncCommandArgument ac) {
        TransferIndex ti = new TransferIndex(Integer.parseInt(ac.getArgsArray()[0]));

        Map<TransferIndex, Transfer> transfers = getSlaveObject().getTransferMap();

        if (!transfers.containsKey(ti)) {
            return null;
        }

        Transfer t = transfers.get(ti);
        t.abort(ac.getArgsArray()[1]);
        return new AsyncResponse(ac.getIndex());
    }

    public AsyncResponse handleConnect(AsyncCommandArgument ac) {
        String[] data = ac.getArgsArray()[0].split(":");
        boolean encrypted = ac.getArgsArray()[1].equals("true");
        boolean useSSLClientHandshake = ac.getArgsArray()[2].equals("true");
        InetAddress address;

        try {
            address = InetAddress.getByName(data[0]);
        } catch (UnknownHostException e1) {
            return new AsyncResponseException(ac.getIndex(), e1);
        }

        int port = Integer.parseInt(data[1]);
        Transfer t = new Transfer(new ActiveConnection(encrypted ? getSlaveObject().getSSLContext() : null,
                new InetSocketAddress(address, port), useSSLClientHandshake, getSlaveObject().getBindIP()),
                getSlaveObject(), new TransferIndex());

        getSlaveObject().addTransfer(t);

        return new AsyncResponseTransfer(ac.getIndex(), new ConnectInfo(port, t
                .getTransferIndex(), t.getTransferStatus()));
    }

    public AsyncResponse handleDelete(AsyncCommandArgument ac) {
        try {
            try {
                getSlaveObject().delete(mapPathToRenameQueue(ac.getArgs()));
            } catch (PermissionDeniedException e) {
                if (Slave.isWin32) {
                    getSlaveObject().getRenameQueue()
                            .add(new QueuedOperation(ac.getArgs(), null));
                } else {
                    throw e;
                }
            }
            sendResponse(new AsyncResponseDiskStatus(getSlaveObject().getDiskStatus()));
            return new AsyncResponse(ac.getIndex());
        } catch (IOException e) {
            return new AsyncResponseException(ac.getIndex(), e);
        }
    }

    public AsyncResponse handleListen(AsyncCommandArgument ac) {
        String[] data = ac.getArgs().split(":");
        boolean encrypted = data[0].equals("true");
        boolean useSSLClientMode = data[1].equals("true");
        PassiveConnection c = null;

        try {
            c = new PassiveConnection(encrypted ? getSlaveObject().getSSLContext() : null,
                    getSlaveObject().getPortRange(), useSSLClientMode, getSlaveObject().getBindIP());

        } catch (IOException e) {
            return new AsyncResponseException(ac.getIndex(), e);
        }

        Transfer t = new Transfer(c, getSlaveObject(), new TransferIndex());
        getSlaveObject().addTransfer(t);

        return new AsyncResponseTransfer(ac.getIndex(), new ConnectInfo(c
                .getLocalPort(), t.getTransferIndex(), t.getTransferStatus()));
    }

    public AsyncResponse handleMaxpath(AsyncCommandArgument ac) {
        return new AsyncResponseMaxPath(ac.getIndex(), Slave.isWin32 ? 255 : Integer.MAX_VALUE);
    }

    public AsyncResponse handlePing(AsyncCommandArgument ac) {
        return new AsyncResponse(ac.getIndex());
    }

    public AsyncResponse handleReceive(AsyncCommandArgument ac) {
        char type = ac.getArgsArray()[0].charAt(0);
        long position = Long.parseLong(ac.getArgsArray()[1]);
        TransferIndex transferIndex = new TransferIndex(Integer.parseInt(ac.getArgsArray()[2]));
        String inetAddress = ac.getArgsArray()[3];
        String path = mapPathToRenameQueue(ac.getArgsArray()[4]);
        String fileName = path.substring(path.lastIndexOf("/") + 1);
        String dirName = path.substring(0, path.lastIndexOf("/"));
        long minSpeed = Long.parseLong(ac.getArgsArray()[5]);
        long maxSpeed = Long.parseLong(ac.getArgsArray()[6]);
        Transfer t = getSlaveObject().getTransfer(transferIndex);
        t.setMinSpeed(minSpeed);
        t.setMaxSpeed(maxSpeed);
        getSlaveObject().sendResponse(new AsyncResponse(ac.getIndex())); // return calling thread
        // on master
        try {
            return new AsyncResponseTransferStatus(t.receiveFile(dirName, type,
                    fileName, position, inetAddress));
        } catch (IOException e) {
            return (new AsyncResponseTransferStatus(new TransferStatus(transferIndex, e)));
        } catch (TransferDeniedException e) {
            return (new AsyncResponseTransferStatus(new TransferStatus(transferIndex, e)));
        }
    }

    public AsyncResponse handleRemergePause(AsyncCommandArgument ac) {
        remergePaused.set(true);
        return new AsyncResponse(ac.getIndex());
    }

    public AsyncResponse handleRemergeResume(AsyncCommandArgument ac) {
        remergePaused.set(false);
        synchronized (remergeWaitObj) {
            remergeWaitObj.notifyAll();
        }
        return new AsyncResponse(ac.getIndex());
    }

    final int chunkSize = 1000;
    private Collection<List<NodePath>> processDirectory(String rootPath) throws IOException {
        final AtomicInteger counter2 = new AtomicInteger();
        long start = System.currentTimeMillis();
        Path p = Paths.get(rootPath);
        //try (Stream<Path> stream = Files.walk(p, Integer.MAX_VALUE)) {
        //    Collection<List<NodePath>> values = stream.parallel()
        //            .filter(f -> !f.toAbsolutePath().toString().equals(rootPath))
        //            .map(d -> new NodePath(d, rootPath))
        //            .collect(Collectors.groupingBy(it -> counter2.getAndIncrement() / chunkSize))
        //            .forEach((integer, nodePaths) -> {
        //                NodePath nodePath = nodePaths.get(integer);
        //                //sendResponse(new AsyncResponseRemerge(nodePath.getPath(), nodePath.get, pathLastModified));
        //            });
        //    System.out.println("Listing with java8 stream path done in " + (System.currentTimeMillis() - start) + "ms");
        //    return values;
        //}
        return null;
    }

    public AsyncResponse handleRemergeNew(AsyncCommandArgument ac) {
        try {
            ArrayList<Root> roots = getSlaveObject().getRoots().getRootList();
            roots.parallelStream().forEach(s -> {
                try {
                    processDirectory(s.getPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            return new AsyncResponse(ac.getIndex());
        } catch (Throwable e) {
            logger.error("Exception during merging", e);
            sendResponse(new AsyncResponseSiteBotMessage("Exception during merging"));
            return new AsyncResponseException(ac.getIndex(), e);
        }
    }

    public AsyncResponse handleRemerge(AsyncCommandArgument ac) {
        try {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            String[] argsArray = ac.getArgsArray();
            long skipAgeCutoff = 0L;
            boolean partialRemerge = Boolean.parseBoolean(argsArray[1]) && !getSlaveObject().ignorePartialRemerge() && !Boolean.parseBoolean(argsArray[4]);
            boolean instantOnline = Boolean.parseBoolean(argsArray[4]);
            if (partialRemerge) {
                skipAgeCutoff = Long.parseLong(argsArray[2]);
                long masterTime = Long.parseLong(argsArray[3]);
                if (skipAgeCutoff != Long.MIN_VALUE) {
                    skipAgeCutoff += System.currentTimeMillis() - masterTime;
                }
                Date cutoffDate = new Date(skipAgeCutoff);
                logger.info("Partial remerge enabled, skipping all files last modified before {}", cutoffDate.toString());
                sendResponse(new AsyncResponseSiteBotMessage("Partial remerge enabled, skipping all files last modified before " + cutoffDate.toString()));
            } else if (instantOnline) {
                logger.info("Instant online enabled, performing full remerge in background");
                sendResponse(new AsyncResponseSiteBotMessage("Instant online enabled, performing full remerge in background"));
            } else {
                logger.info("Partial remerge disabled, performing full remerge");
                sendResponse(new AsyncResponseSiteBotMessage("Partial remerge disabled, performing full remerge"));
            }

            if (getSlaveObject().threadedRemerge()) {
                if (threadMergeCount.get() == 0) {
                    // Create worker threads
                    logger.info("Starting to merge with threads");
                    mergeThreads = new HandleRemergeRecursiveThread[maxMergeThreads];
                    for (int i = 0; i < maxMergeThreads; i++) {
                        mergeThreads[i] = new HandleRemergeRecursiveThread(getSlaveObject().getRoots(), partialRemerge, skipAgeCutoff, i + 1);
                        mergeThreads[i].start();
                    }
                    sendResponse(new AsyncResponseSiteBotMessage("Starting to merge with threads"));
                    HandleRemergeRecursiveThread thread = getRemergeThread();
                    thread.setPathAndRun(argsArray[0]);
                } else {
                    sendResponse(new AsyncResponseSiteBotMessage("Merge already running, wait for it to finish"));
                }
            } else if (getSlaveObject().concurrentRootIteration()) {
                logger.info("Starting to merge with roots concurrently with handleRemergeRecursiveConcurrent");
                sendResponse(new AsyncResponseSiteBotMessage("Starting to merge with roots concurrently"));
                handleRemergeRecursiveConcurrent(getSlaveObject().getRoots(), argsArray[0], partialRemerge, skipAgeCutoff);
            } else {
                logger.info("Starting to merge with handleRemergeRecursive2");
                sendResponse(new AsyncResponseSiteBotMessage("Starting to merge"));
                handleRemergeRecursive2(getSlaveObject().getRoots(), argsArray[0], partialRemerge, skipAgeCutoff);
            }

            // Make sure we dont make the slave available online until the whole filesystem is sent
            if (getSlaveObject().threadedRemerge()) {
                while (threadMergeCount.get() != 0) {
                    if (!getSlaveObject().isOnline()) {
                        // Slave has shut down, no need to continue with remerge
                        return null;
                    }
                    synchronized (threadMergeWaitObj) {
                        try {
                            threadMergeWaitObj.wait(5000);
                        } catch (InterruptedException e) {
                            // Either we have been woken properly in which case we will exit the
                            // loop or we have not in which case we will wait again.
                        }
                    }

                    if (threadMergeCount.get() == 0) {
                        // Tell all worker threads to exit, where done with the merge.
                        for (int i = 0; i < maxMergeThreads; i++) {
                            mergeThreads[i].exit();
                        }
                        break;
                    }
                }
            }

            return new AsyncResponse(ac.getIndex());
        } catch (Throwable e) {
            logger.error("Exception during merging", e);
            sendResponse(new AsyncResponseSiteBotMessage("Exception during merging"));

            return new AsyncResponseException(ac.getIndex(), e);
        }
    }

    private HandleRemergeRecursiveThread getRemergeThread() {
        synchronized (mergeThreads) {
            for (int i = 0; i < maxMergeThreads; i++) {
                if (mergeThreads[i].isAvailable()) {
                    mergeThreads[i].unavailable();
                    return mergeThreads[i];
                }
            }
        }
        return null;
    }

    private void updateDepth(String path) {
        boolean add = true;
        synchronized (threadMergeDepth) {
            for (String dir : threadMergeDepth) {
                if (path.startsWith(dir)) {
                    add = false;
                    break;
                }

                if (dir.startsWith(path)) {
                    add = false;
                    break;
                }
            }

            if (add) {
                threadMergeDepth.add(path);
            }
        }

        synchronized (threadMergeDepthWaitObj) {
            threadMergeDepthWaitObj.notifyAll();
        }
    }

    private void waitForDepth(String path) {
        while (true) {
            if (!getSlaveObject().isOnline()) {
                // Slave has shut down, no need to continue with remerge
                return;
            }
            synchronized (threadMergeDepth) {
                for (String dir : threadMergeDepth) {
                    if (dir.startsWith(path)) {
                        return;
                    }
                }
            }

            synchronized (threadMergeDepthWaitObj) {
                try {
                    threadMergeDepthWaitObj.wait(2000);
                } catch (InterruptedException e) {
                    // We have been woken up, check if the path is there now.
                }
            }
        }
    }

    private void handleRemergeRecursive2(RootCollection rootCollection,
                                         String path, boolean partialRemerge, long skipAgeCutoff) {
        remergeDepth++;
        if (!getSlaveObject().isOnline()) {
            // Slave has shut down, no need to continue with remerge
            return;
        }
        while (remergePaused.get() && getSlaveObject().isOnline()) {
            synchronized (remergeWaitObj) {
                try {
                    remergeWaitObj.wait(5000);
                } catch (InterruptedException e) {
                    // Either we have been woken properly in which case we will exit the
                    // loop or we have not in which case we will wait again.
                }
            }
        }
        TreeSet<String> inodes = rootCollection.getLocalInodes(path);
        ArrayList<LightRemoteInode> fileList = new ArrayList<>();

        boolean inodesModified = false;
        long pathLastModified = rootCollection.getLastModifiedForPath(path);
        // Need to check the last modified of the parent itself to detect where
        // files have been deleted but none changed or added
        if (partialRemerge && pathLastModified > skipAgeCutoff) {
            inodesModified = true;
        }
        for (String inode : inodes) {
            String fullPath = path + "/" + inode;
            PhysicalFile file;
            try {
                file = rootCollection.getFile(fullPath);
            } catch (FileNotFoundException e) {
                // something is screwy, we just found the file, it has to exist
                // race condition i guess, stop deleting files outside drftpd!
                logger.error("Error getting file {} even though we just listed it, check permissions", fullPath, e);
                sendResponse(new AsyncResponseSiteBotMessage("Error getting file " + fullPath + " check permissions"));
                continue;
            }
            try {
                if (file.isSymbolicLink()) {
                    // ignore it, but log an error
                    logger.warn("You have a symbolic link {} -- these are ignored by drftpd", fullPath);
                    sendResponse(new AsyncResponseSiteBotMessage("You have a symbolic link " + fullPath + " -- these are ignored by drftpd"));
                    continue;
                }
            } catch (IOException e) {
                logger.warn("You have a symbolic link that couldn't be read at {} -- these are ignored by drftpd", fullPath);
                sendResponse(new AsyncResponseSiteBotMessage("You have a symbolic link thacouldn't be read at " + fullPath + " -- these are ignored by drftpd"));
                continue;
            }
            if (partialRemerge && file.lastModified() > skipAgeCutoff) {
                inodesModified = true;
            }
            if (file.isDirectory()) {
                handleRemergeRecursive2(rootCollection, fullPath, partialRemerge, skipAgeCutoff);
            }
            fileList.add(new LightRemoteInode(file));
        }
        if (!partialRemerge || inodesModified) {
            sendResponse(new AsyncResponseRemerge(path, fileList, pathLastModified));
            logger.debug("Sending {} to the master", path);
        } else {
            logger.debug("Skipping send of {} as no files changed since last merge", path);
        }

        if (--remergeDepth == 0) {
            sendResponse(new AsyncResponseSiteBotMessage("Merge done"));
        }
    }

    private void handleRemergeRecursiveConcurrent(RootCollection rootCollection,
                                                  String path, boolean partialRemerge, long skipAgeCutoff) {
        remergeConcurrentDepth++;
        if (!getSlaveObject().isOnline()) {
            // Slave has shut down, no need to continue with remerge
            return;
        }
        while (remergePaused.get() && getSlaveObject().isOnline()) {
            synchronized (remergeWaitObj) {
                try {
                    remergeWaitObj.wait(5000);
                } catch (InterruptedException e) {
                    // Either we have been woken properly in which case we will exit the
                    // loop or we have not in which case we will wait again.
                }
            }
        }

        RootPathContents rootContents = rootCollection.getLocalInodesConcurrent(path);
        ArrayList<LightRemoteInode> fileList = new ArrayList<>();

        boolean inodesModified = false;
        long pathLastModified = rootContents.getLastModified();
        // Need to check the last modified of the parent itself to detect where
        // files have been deleted but none changed or added
        if (partialRemerge && pathLastModified > skipAgeCutoff) {
            inodesModified = true;
        }
        for (Map.Entry<String, File> entry : rootContents.getInodes().entrySet()) {
            PhysicalFile file = new PhysicalFile(entry.getValue());
            String fullPath = path + "/" + entry.getKey();
            try {
                if (file.isSymbolicLink()) {
                    // ignore it, but log an error
                    logger.warn("You have a symbolic link {} -- these are ignored by drftpd", fullPath);
                    sendResponse(new AsyncResponseSiteBotMessage("You have a symbolic link " + fullPath + " -- these are ignored by drftpd"));
                    continue;
                }
            } catch (IOException e) {
                logger.warn("You have a symbolic link that couldn't be read at {} -- these are ignored by drftpd", fullPath);
                sendResponse(new AsyncResponseSiteBotMessage("You have a symbolic link that couldn't be read at " + fullPath + " -- these are ignored by drfptd"));
                continue;
            }
            if (partialRemerge && file.lastModified() > skipAgeCutoff) {
                inodesModified = true;
            }
            if (file.isDirectory()) {
                handleRemergeRecursiveConcurrent(rootCollection, fullPath, partialRemerge, skipAgeCutoff);
            }
            fileList.add(new LightRemoteInode(file));
        }
        if (!partialRemerge || inodesModified) {
            sendResponse(new AsyncResponseRemerge(path, fileList, pathLastModified));
            // logger.debug("Sending {} to the master", path);
        } else {
            logger.debug("Skipping send of {} as no files changed since last merge", path);
        }

        if (--remergeConcurrentDepth == 0) {
            sendResponse(new AsyncResponseSiteBotMessage("Merge done"));
        }
    }

    public AsyncResponse handleRename(AsyncCommandArgument ac) {
        String from = mapPathToRenameQueue(ac.getArgsArray()[0]);
        String toDir = ac.getArgsArray()[1];
        String toFile = ac.getArgsArray()[2];

        try {
            try {
                getSlaveObject().rename(from, toDir, toFile);
            } catch (PermissionDeniedException e) {
                if (Slave.isWin32) {
                    String simplePath = null;
                    if (toDir.endsWith("/")) {
                        simplePath = toDir + toFile;
                    } else {
                        simplePath = toDir + "/" + toFile;
                    }
                    getSlaveObject().getRenameQueue().add(new QueuedOperation(from, simplePath));
                } else {
                    throw e;
                }
            }

            return new AsyncResponse(ac.getIndex());
        } catch (IOException e) {
            return new AsyncResponseException(ac.getIndex(), e);
        }
    }

    public AsyncResponse handleSend(AsyncCommandArgument ac) {
        char type = ac.getArgsArray()[0].charAt(0);
        long position = Long.parseLong(ac.getArgsArray()[1]);
        TransferIndex transferIndex = new TransferIndex(Integer.parseInt(ac.getArgsArray()[2]));
        String inetAddress = ac.getArgsArray()[3];
        String path = mapPathToRenameQueue(ac.getArgsArray()[4]);
        long minSpeed = Long.parseLong(ac.getArgsArray()[5]);
        long maxSpeed = Long.parseLong(ac.getArgsArray()[6]);
        Transfer t = getSlaveObject().getTransfer(transferIndex);
        t.setMinSpeed(minSpeed);
        t.setMaxSpeed(maxSpeed);
        sendResponse(new AsyncResponse(ac.getIndex()));

        // calling thread on master
        try {
            return new AsyncResponseTransferStatus(t.sendFile(path, type,
                    position, inetAddress));
        } catch (IOException | TransferDeniedException e) {
            return new AsyncResponseTransferStatus(new TransferStatus(t
                    .getTransferIndex(), e));
        }
    }

    public AsyncResponse handleChecksum(AsyncCommandArgument ac) {
        try {
            return new AsyncResponseChecksum(ac.getIndex(), getSlaveObject().checkSum(ac.getArgs()));
        } catch (IOException e) {
            return new AsyncResponseException(ac.getIndex(), e);
        }
    }

    public AsyncResponse handleShutdown(AsyncCommandArgument ac) {
        logger.info("The master has requested that I shutdown");
        getSlaveObject().shutdown();
        System.exit(0);
        return null;
    }

    public AsyncResponse handleCheckSSL(AsyncCommandArgument ac) {
        return new AsyncResponseSSLCheck(ac.getIndex(), getSlaveObject().getSSLContext() != null);
    }

    private class HandleRemergeRecursiveThread extends Thread {
        private final Object localWaitObj = new Object();
        private RootCollection rootCollection = null;
        private String path = null;
        private boolean partialRemerge = false;
        private boolean localRun = false;
        private long skipAgeCutoff = 0L;
        private volatile boolean available = true;
        private volatile boolean exit = false;

        public HandleRemergeRecursiveThread(RootCollection rootCollection, boolean partialRemerge, long skipAgeCutoff, int threadNumber) {
            super("RemergeThread-" + threadNumber);
            this.rootCollection = rootCollection;
            this.partialRemerge = partialRemerge;
            this.skipAgeCutoff = skipAgeCutoff;
            this.setPriority(Thread.MIN_PRIORITY);
        }

        public void run() {
            while (true) {
                if (!getSlaveObject().isOnline()) {
                    // Slave has shut down, no need to continue with remerge
                    return;
                }
                while (remergePaused.get() && getSlaveObject().isOnline()) {
                    synchronized (remergeWaitObj) {
                        try {
                            remergeWaitObj.wait(5000);
                        } catch (InterruptedException e) {
                            // Either we have been woken properly in which case we will exit the
                            // loop or we have not in which case we will wait again.
                        }
                    }
                }

                while (path == null) {
                    if (exit || !getSlaveObject().isOnline()) {
                        return;
                    }
                    synchronized (localWaitObj) {
                        try {
                            localWaitObj.wait(2000);
                        } catch (InterruptedException e) {
                            // Either we have been woken properly in which case we will exit the
                            // loop or we have not in which case we will wait again.
                        }
                    }
                }

                TreeSet<String> inodes = rootCollection.getLocalInodes(path);
                ArrayList<LightRemoteInode> fileList = new ArrayList<>();
                ArrayList<String> dirList = new ArrayList<>();

                boolean inodesModified = false;
                long pathLastModified = rootCollection.getLastModifiedForPath(path);
                // Need to check the last modified of the parent itself to detect where
                // files have been deleted but none changed or added
                if (partialRemerge && pathLastModified > skipAgeCutoff) {
                    inodesModified = true;
                }

                for (String inode : inodes) {
                    String fullPath = path + "/" + inode;
                    PhysicalFile file;
                    try {
                        file = rootCollection.getFile(fullPath);
                    } catch (FileNotFoundException e) {
                        // something is screwy, we just found the file, it has to exist
                        // race condition i guess, stop deleting files outside drftpd!
                        logger.error("Error getting file {} even though we just listed it, check permissions", fullPath, e);
                        sendResponse(new AsyncResponseSiteBotMessage("Error getting file " + fullPath + " check permissions"));
                        continue;
                    }
                    try {
                        if (file.isSymbolicLink()) {
                            // ignore it, but log an error
                            logger.warn("You have a symbolic link {} -- these are ignored by drftpd", fullPath);
                            sendResponse(new AsyncResponseSiteBotMessage("You have a symbolic link " + fullPath + " -- these are ignored by drftpd"));
                            continue;
                        }
                    } catch (IOException e) {
                        logger.warn("You have a symbolic link that couldn't be read at {} -- these are ignored by drftpd", fullPath);
                        sendResponse(new AsyncResponseSiteBotMessage("You have a symbolic link that couldn't be read at " + fullPath + " -- these are ignored by drftpd"));
                        continue;
                    }
                    if (partialRemerge && file.lastModified() > skipAgeCutoff) {
                        inodesModified = true;
                    }
                    if (file.isDirectory()) {
                        dirList.add(fullPath + "/");

                        HandleRemergeRecursiveThread thread = null;
                        if (!inode.equalsIgnoreCase("sample") && !inode.equalsIgnoreCase("subs")) {
                            thread = getRemergeThread();
                            if (thread != null) {
                                thread.setPathAndRun(fullPath);
                            }
                        }

                        if (thread == null) {
                            // Local run, either there where no threads available or it matched sample or subs
                            // This emulates the recursive behavior of the "normal" remerge process
                            // See handleRemergeRecursive2
                            boolean restoreRun = localRun;
                            String oldPath = path;
                            path = fullPath;
                            if (!localRun) {
                                localRun = true;
                            }
                            run();
                            path = oldPath;
                            localRun = restoreRun;
                        }
                    }
                    // this collects files and sends it to master
                    //TODO check if file is in getMultipleRoots.
                    // if true, diff checksum
                    // if differ, throw MSGSLAVE with info and continue remerge
                    fileList.add(new LightRemoteInode(file));
                }

                if (!partialRemerge || inodesModified) {
                    for (String dir : dirList) {
                        waitForDepth(dir);
                    }
                    sendResponse(new AsyncResponseRemerge(path, fileList, pathLastModified));
                    // logger.debug("Sending {} - {} to the master", fileList.size(), path);
                } else {
                    logger.debug("Skipping send of {} as no files changed since last merge", path);
                }
                updateDepth(path + "/");

                if (!localRun) {
                    available();
                } else {
                    return;
                }

                if (threadMergeCount.get() == 0) {
                    sendResponse(new AsyncResponseSiteBotMessage("Merge done"));
                    synchronized (threadMergeWaitObj) {
                        threadMergeWaitObj.notifyAll();
                    }
                    threadMergeDepth.clear();
                }
            }
        }

        public boolean isAvailable() {
            return available;
        }

        private void available() {
            path = null;
            threadMergeCount.decrementAndGet();
            available = true;
        }

        public void unavailable() {
            available = false;
            threadMergeCount.incrementAndGet();
        }

        public void setPathAndRun(String path) {
            this.path = path;
            synchronized (localWaitObj) {
                localWaitObj.notify();
            }
        }

        public void exit() {
            exit = true;
            synchronized (localWaitObj) {
                localWaitObj.notify();
            }
        }
    }
}
