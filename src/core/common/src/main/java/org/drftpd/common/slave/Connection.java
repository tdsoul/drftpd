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
package org.drftpd.common.slave;

import java.io.IOException;
import java.net.Socket;

/**
 * @author mog
 * @version $Id$
 */
public abstract class Connection {
    public static final int TIMEOUT = 10000;

    public abstract Socket connect(String[] cipherSuites, String[] sslProtocols, int bufferSize) throws IOException;

    protected void setSockOpts(Socket sock) throws IOException {
        /*
         * IPTOS_LOWCOST (0x02) IPTOS_RELIABILITY (0x04) IPTOS_THROUGHPUT (0x08)
         * IPTOS_LOWDELAY (0x10)
         */
        sock.setTrafficClass(0x08);
        sock.setSoTimeout(TIMEOUT);
    }

    public abstract void abort();
}
