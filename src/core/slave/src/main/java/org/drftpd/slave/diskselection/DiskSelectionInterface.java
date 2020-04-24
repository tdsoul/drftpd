package org.drftpd.slave.diskselection;


import org.drftpd.slave.Slave;
import org.drftpd.slave.vfs.Root;
import org.drftpd.slave.vfs.RootCollection;

public abstract class DiskSelectionInterface {
    private final Slave _slave;
    private final RootCollection _rootCollection;

    public DiskSelectionInterface(Slave slave) {
        _slave = slave;
        _rootCollection = slave.getRoots();
    }

    public RootCollection getRootCollection() {
        return _rootCollection;
    }

    public Slave getSlaveObject() {
        return _slave;
    }

    public abstract Root getBestRoot(String dir);
}
