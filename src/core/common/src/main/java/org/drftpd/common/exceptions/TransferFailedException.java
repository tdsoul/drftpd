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
package org.drftpd.common.exceptions;

import org.drftpd.common.slave.TransferStatus;

import java.io.IOException;

/**
 * @author mog
 * @version $Id$
 */
@SuppressWarnings("serial")
public class TransferFailedException extends IOException {
    private final TransferStatus _status;

    public TransferFailedException(Exception e, TransferStatus status) {
        super(e.getMessage());
        initCause(e);
        _status = status;
    }

    public TransferFailedException(String message, TransferStatus status) {
        super(message);
        _status = status;
    }

    public TransferStatus getStatus() {
        return _status;
    }
}
