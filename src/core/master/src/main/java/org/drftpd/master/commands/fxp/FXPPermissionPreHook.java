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
package org.drftpd.master.commands.fxp;

import org.drftpd.common.extensibility.CommandHook;
import org.drftpd.common.extensibility.HookType;
import org.drftpd.master.GlobalContext;
import org.drftpd.master.commands.CommandRequest;
import org.drftpd.master.commands.CommandRequestInterface;
import org.drftpd.master.commands.dataconnection.DataConnectionHandler;
import org.drftpd.master.config.ConfigInterface;
import org.drftpd.master.network.BaseFtpConnection;
import org.drftpd.master.vfs.DirectoryHandle;
import org.drftpd.slave.network.Transfer;

import java.net.InetAddress;

/**
 * @author fr0w
 * @version $Id$
 */
public class FXPPermissionPreHook {

    @CommandHook(commands = "doRETR", priority = 3, type = HookType.PRE)
    public CommandRequestInterface checkDownloadFXPPerm(CommandRequest request) {
        return checkFXPPerm(request, Transfer.TRANSFER_SENDING_DOWNLOAD);
    }

    @CommandHook(commands = "doSTOR", priority = 3, type = HookType.PRE)
    public CommandRequestInterface checkUploadFXPPerm(CommandRequest request) {
        return checkFXPPerm(request, Transfer.TRANSFER_RECEIVING_UPLOAD);
    }

    public CommandRequestInterface checkFXPPerm(CommandRequest request, char direction) {
        DirectoryHandle fromDir = request.getCurrentDirectory();
        ConfigInterface config = GlobalContext.getConfig();
        String directive = direction == Transfer.TRANSFER_RECEIVING_UPLOAD ? "deny_upfxp" : "deny_dnfxp";
        String mask = "*@*"; // default initialization

        if (config.checkPathPermission(directive, request.getSession().getUserNull(request.getUser()), fromDir)) {
            // denied to make fxp.
            // let's set the ip that is going to be sent to the slave.
            InetAddress inetAdd = request.getSession().getObject(BaseFtpConnection.ADDRESS, null);
            mask = "*@" + inetAdd.getHostAddress();
        }

        request.getSession().setObject(DataConnectionHandler.INET_ADDRESS, mask);

        return request;
    }
}
