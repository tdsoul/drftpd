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
package org.drftpd.master.slaveselection;

import org.drftpd.master.GlobalContext;
import org.drftpd.master.exceptions.NoAvailableSlaveException;
import org.drftpd.master.network.BaseFtpConnection;
import org.drftpd.master.slavemanagement.RemoteSlave;
import org.drftpd.master.slavemanagement.SlaveManager;
import org.drftpd.master.vfs.FileHandle;
import org.drftpd.master.vfs.InodeHandle;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

/**
 * @author mog
 * @version $Id: SlaveSelectionManagerInterface.java 1443 2006-03-18 00:57:58Z
 * zubov $
 */
public abstract class SlaveSelectionManagerInterface {
    public static GlobalContext getGlobalContext() {
        return GlobalContext.getGlobalContext();
    }

    public abstract void reload() throws IOException;

    public abstract RemoteSlave getASlave(BaseFtpConnection conn, char direction, InodeHandle file)
            throws NoAvailableSlaveException;

    public abstract RemoteSlave getASlaveForJobDownload(FileHandle file, Collection<RemoteSlave> destinationSlaves)
            throws NoAvailableSlaveException, FileNotFoundException;

    public abstract RemoteSlave getASlaveForJobUpload(FileHandle file, Collection<RemoteSlave> destinationSlaves, RemoteSlave sourceSlave)
            throws NoAvailableSlaveException, FileNotFoundException;

    public Collection<RemoteSlave> getAvailableSlaves() throws NoAvailableSlaveException {
        return getSlaveManager().getAvailableSlaves();
    }

    public SlaveManager getSlaveManager() {
        return getGlobalContext().getSlaveManager();
    }
}
