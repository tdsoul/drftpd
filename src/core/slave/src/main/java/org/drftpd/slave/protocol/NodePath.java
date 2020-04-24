package org.drftpd.slave.protocol;

import org.drftpd.common.slave.LightRemoteInode;

import java.io.File;
import java.nio.file.Path;

public class NodePath {
    private String path;
    private long lastModified;
    private LightRemoteInode node;

    public NodePath(Path path, String rootDir) {
        File file = path.toFile();
        if (file.isDirectory()) {
            this.path = path.toAbsolutePath().toString().substring(rootDir.length());
        } else {
            this.path = path.getParent().toAbsolutePath().toString().substring(rootDir.length());
        }
        if (this.path.isEmpty()) this.path = "/";
        if (File.separatorChar == '\\') { // stupid win32 hack
            this.path = this.path.replaceAll("\\\\", "/");
        }
        this.lastModified = file.lastModified();
        this.node = new LightRemoteInode(file);
    }

    public String getPath() {
        return path;
    }

    public LightRemoteInode getNode() {
        return node;
    }

    public long getLastModified() {
        return lastModified;
    }
}
