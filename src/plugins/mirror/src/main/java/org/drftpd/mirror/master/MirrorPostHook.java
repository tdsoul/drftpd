/*
 * This file is part of DrFTPD, Distributed FTP Daemon.
 *
 * DrFTPD is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * DrFTPD is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DrFTPD; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.drftpd.mirror.master;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.drftpd.common.extensibility.CommandHook;
import org.drftpd.common.extensibility.HookType;
import org.drftpd.common.extensibility.PluginInterface;
import org.drftpd.common.util.ConfigLoader;
import org.drftpd.jobs.master.Job;
import org.drftpd.jobs.master.JobManager;
import org.drftpd.master.GlobalContext;
import org.drftpd.master.commands.CommandRequest;
import org.drftpd.master.commands.CommandResponse;
import org.drftpd.master.event.ReloadEvent;
import org.drftpd.master.exceptions.NoAvailableSlaveException;
import org.drftpd.master.slavemanagement.RemoteSlave;
import org.drftpd.master.vfs.FileHandle;
import org.drftpd.slave.exceptions.ObjectNotFoundException;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

/**
 * @author lh
 */
public class MirrorPostHook {
    private static final Logger logger = LogManager.getLogger(MirrorPostHook.class);

    private final ArrayList<MirrorSetting> _settings;

    public MirrorPostHook() {
        _settings = new ArrayList<>();
        loadConf();
        // Subscribe to events
        AnnotationProcessor.process(this);
    }

    private void loadConf() {
        Properties cfg = ConfigLoader.loadPluginConfig("mirror.conf");
        _settings.clear();
        for (int i = 1; ; i++) {
            String nbrOfMirrors = cfg.getProperty(i + ".nbrOfMirrors");
            if (nbrOfMirrors == null) break;
            MirrorSetting setting = new MirrorSetting();
            try {
                setting.setNbrOfMirrors(Integer.parseInt(nbrOfMirrors));
                if (setting.getNbrOfMirrors() < 2) {
                    logger.error("Invalid setting for {}.nbrOfMirrors, must be greater than 2", i);
                    continue;
                }
                setting.setPriority(Integer.parseInt(cfg.getProperty(i + ".priority", "3")));
                if (setting.getPriority() < 1) {
                    logger.error("Invalid setting for {}.priority, must be greater than 0", i);
                    continue;
                }
            } catch (NumberFormatException e) {
                logger.error("Invalid setting for {}.nbrOfMirrors, not a number", i);
                continue;
            }
            ArrayList<String> paths = new ArrayList<>();
            for (int j = 1; ; j++) {
                String path = cfg.getProperty(i + ".path." + j);
                if (path == null || path.trim().isEmpty()) break;
                paths.add(path);
            }
            setting.setPaths(paths);
            ArrayList<String> excludedPaths = new ArrayList<>();
            for (int j = 1; ; j++) {
                String excludePath = cfg.getProperty(i + ".excludePath." + j);
                if (excludePath == null || excludePath.trim().isEmpty()) break;
                excludedPaths.add(excludePath);
            }
            setting.setExcludedPaths(excludedPaths);
            String slaves = cfg.getProperty(i + ".slaves");
            if (slaves != null && !slaves.trim().isEmpty()) {
                HashSet<RemoteSlave> slaveList = new HashSet<>();
                for (String slaveName : slaves.split(" ")) {
                    try {
                        slaveList.add(GlobalContext.getGlobalContext().getSlaveManager().getRemoteSlave(slaveName));
                    } catch (ObjectNotFoundException e) {
                        logger.error("Slave name invalid in mirror config ({}): {}", i, slaveName);
                    }
                }
                setting.setSlaves(slaveList);
            }
            String excludeSlaves = cfg.getProperty(i + ".excludeSlaves");
            if (excludeSlaves != null && !excludeSlaves.trim().isEmpty()) {
                HashSet<RemoteSlave> slaveList = new HashSet<>();
                for (String slaveName : excludeSlaves.split(" ")) {
                    try {
                        slaveList.add(GlobalContext.getGlobalContext().getSlaveManager().getRemoteSlave(slaveName));
                    } catch (ObjectNotFoundException e) {
                        logger.error("Slave name invalid in mirror config ({}): {}", i, slaveName);
                    }
                }
                setting.setExcludedSlaves(slaveList);
            }
            _settings.add(setting);
        }
    }

    @CommandHook(commands = "doSTOR", priority = 100, type = HookType.POST)
    public void doSTORPostHook(CommandRequest request, CommandResponse response) {
        if (response.getCode() != 226) {
            // STOR Failed, skip
            return;
        }
        FileHandle file;
        try {
            file = request.getCurrentDirectory().getFileUnchecked(request.getArgument());
        } catch (Exception e) {
            // File not available any more, transfer must have been aborted
            return;
        }

        // Check if there is a valid configuration for this path
        MirrorSetting activeSetting = null;
        for (MirrorSetting setting : _settings) {
            for (String pattern : setting.getPaths()) {
                if (file.getPath().matches(pattern)) {
                    activeSetting = setting;
                    break;
                }
            }
            if (activeSetting != null) {
                // Setting valid but check excluded paths also
                for (String pattern : setting.getExcludedPaths()) {
                    if (file.getPath().matches(pattern)) {
                        activeSetting = null;
                        break;
                    }
                }
            }
            // Setting still valid?
            // If so no need to continue, only one mirror configuration can be valid
            if (activeSetting != null) break;
        }
        if (activeSetting == null) return;

        HashSet<String> mirrorSlaves = new HashSet<>();
        try {
            // Add slave(s) file already exist on
            for (String existingSlave : file.getSlaveNames()) {
                mirrorSlaves.add(existingSlave);
            }
        } catch (FileNotFoundException e) {
            // file deleted, no problem, just exit
            return;
        }
        if (activeSetting.getSlaves() != null) {
            for (RemoteSlave slave : activeSetting.getSlaves()) {
                mirrorSlaves.add(slave.getName());
            }
        } else {
            try {
                for (RemoteSlave slave : GlobalContext.getGlobalContext().getSlaveManager().getAvailableSlaves()) {
                    mirrorSlaves.add(slave.getName());
                }
            } catch (NoAvailableSlaveException e) {
                // No need to continue
                return;
            }
            if (activeSetting.getExcludedSlaves() != null) {
                // Remove excluded slaves
                for (RemoteSlave slave : activeSetting.getExcludedSlaves()) {
                    mirrorSlaves.remove(slave.getName());
                }
            }
        }

        if (activeSetting.getNbrOfMirrors() <= mirrorSlaves.size()) {
            // We got enough slaves, proceed and add job to queue
            getJobManager().addJobToQueue(new Job(file, mirrorSlaves, activeSetting.getPriority(),
                    activeSetting.getNbrOfMirrors()));
        } else {
            logger.debug("Not adding {} to job queue, not enough slaves available.", file.getPath());
        }
    }

    /*
     * Gets the jobmananger, hopefully its loaded.
     */
    public JobManager getJobManager() {
        for (PluginInterface plugin : GlobalContext.getGlobalContext().getPlugins()) {
            if (plugin instanceof JobManager) {
                return (JobManager) plugin;
            }
        }
        throw new RuntimeException("JobManager is not loaded");
    }

    @EventSubscriber
    public void onReloadEvent(ReloadEvent event) {
        loadConf();
    }
}