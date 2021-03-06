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
package org.drftpd.zipscript.master.flac;

import org.drftpd.common.network.AsyncCommandArgument;
import org.drftpd.common.protocol.AbstractIssuer;
import org.drftpd.master.exceptions.SlaveUnavailableException;
import org.drftpd.master.slavemanagement.RemoteSlave;

/**
 * @author norox
 */
public class ZipscriptFlacIssuer extends AbstractIssuer {
    @Override
    public String getProtocolName() {
        return "ZipscriptFlacProtocol";
    }

    public String issueFlacFileToSlave(RemoteSlave rslave, String path) throws SlaveUnavailableException {
        String index = rslave.fetchIndex();
        AsyncCommandArgument ac = new AsyncCommandArgument(index, "flacfile", path);
        rslave.sendCommand(ac);

        return index;
    }
}
