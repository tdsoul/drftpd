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
package org.drftpd.master.vfs;

import org.drftpd.common.vfs.InodeHandleInterface;
import org.drftpd.master.slavemanagement.RemoteSlave;
import org.drftpd.slave.exceptions.FileExistsException;

import java.io.FileNotFoundException;


/**
 * Lowest level of DirectoryHandles.<br>
 * This class provides more flexibility and organization to the VFS.
 *
 * @author zubov
 * @version $Id$
 */
public interface DirectoryHandleInterface extends InodeHandleInterface {

    DirectoryHandleInterface createDirectorySystem(String string) throws FileExistsException, FileNotFoundException;

    FileHandle createFileUnchecked(String string, String user, String group, RemoteSlave rslave) throws FileExistsException, FileNotFoundException;

}
