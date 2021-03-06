package org.drftpd.master.slavemanagement;

import org.drftpd.master.slavemanagement.RemoteSlave;

/**
 * @author zubov
 * @version $Id$
 */
public class DummyRemoteSlave extends RemoteSlave {
    public DummyRemoteSlave(String name) {
        super(name);
    }

    public int getPort() {
        return 10;
    }

    public void fakeConnect() {
        _errors = 0;
        _lastNetworkError = System.currentTimeMillis();
    }
}
