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
package org.drftpd.zipscript.master.sfv.hooks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.drftpd.common.extensibility.CommandHook;
import org.drftpd.common.extensibility.HookType;
import org.drftpd.common.util.ConfigLoader;
import org.drftpd.common.util.GlobPattern;
import org.drftpd.master.GlobalContext;
import org.drftpd.master.commands.CommandRequest;
import org.drftpd.master.commands.CommandRequestInterface;
import org.drftpd.master.commands.CommandResponse;
import org.drftpd.master.exceptions.NoAvailableSlaveException;
import org.drftpd.master.exceptions.SlaveUnavailableException;
import org.drftpd.master.permissions.GlobPathPermission;
import org.drftpd.master.permissions.Permission;
import org.drftpd.master.usermanager.User;
import org.drftpd.master.vfs.DirectoryHandle;
import org.drftpd.master.vfs.FileHandle;
import org.drftpd.zipscript.common.sfv.SFVInfo;
import org.drftpd.zipscript.master.sfv.ZipscriptVFSDataSFV;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.PatternSyntaxException;

/**
 * @author djb61
 * @version $Id$
 */
public class ZipscriptPreHook {

    private static final Logger logger = LogManager.getLogger(ZipscriptPreHook.class);

    private final boolean _sfvFirstRequired;

    private final boolean _sfvFirstAllowNoExt;

    private final boolean _sfvDenySubDir;

    private final String _sfvDenySubDirInclude;

    private final String _sfvDenySubDirExclude;

    public ZipscriptPreHook() {
        Properties cfg = ConfigLoader.loadPluginConfig("zipscript.conf");
        // SFV First PathPermissions
        String sfvFirstUsers = cfg.getProperty("sfvfirst.users", "");
        _sfvFirstRequired = cfg.getProperty("sfvfirst.required", "false").equals("true");
        _sfvFirstAllowNoExt = cfg.getProperty("sfvfirst.allownoext", "false").equals("true");
        _sfvDenySubDir = cfg.getProperty("sfvdeny.subdir", "false").equals("true");
        _sfvDenySubDirInclude = cfg.getProperty("sfvdeny.subdir.include", ".*");
        _sfvDenySubDirExclude = cfg.getProperty("sfvdeny.subdir.exclude", ".*");
        if (_sfvFirstRequired) {
            try {
                // this one gets perms defined in sfvfirst.users
                StringTokenizer st = new StringTokenizer(cfg
                        .getProperty("sfvfirst.pathcheck", ""));
                while (st.hasMoreTokens()) {
                    GlobalContext.getConfig().addPathPermission(
                            "sfvfirst.pathcheck",
                            new GlobPathPermission(GlobPattern.compile(st.nextToken()), Permission
                                    .makeUsers(new StringTokenizer(
                                            sfvFirstUsers, " "))));
                }
                st = new StringTokenizer(cfg
                        .getProperty("sfvfirst.pathignore", ""));
                while (st.hasMoreTokens()) {
                    GlobalContext.getConfig().addPathPermission(
                            "sfvfirst.pathignore",
                            new GlobPathPermission(GlobPattern.compile(st.nextToken()), Permission
                                    .makeUsers(new StringTokenizer("*", " "))));
                }
            } catch (PatternSyntaxException e) {
                logger.warn("Exception when reading config/plugins/zipscript.conf", e);
            }
        }
    }

    @CommandHook(commands = "doSTOR", priority = 10, type = HookType.PRE)
    public CommandRequestInterface doZipscriptSTORPreCheck(CommandRequest request) {

        if (!request.hasArgument()) {
            // Syntax error but we'll let the command itself deal with it
            return request;
        }
        FileHandle checkFile = request.getCurrentDirectory().getNonExistentFileHandle(request.getArgument());
        String checkName = checkFile.getName().toLowerCase();
        // Read config
        Properties cfg = ConfigLoader.loadPluginConfig("zipscript.conf");
        boolean restrictSfvEnabled = cfg.getProperty("sfv.restrict.files", "false").equals("true");
        boolean sfvFirstEnforcedPath = checkSfvFirstEnforcedPath(checkFile.getParent(),
                request.getSession().getUserNull(request.getUser()));
        if (checkSfvDenySubdir(checkFile)) {
            request.setAllowed(false);
            request.setDeniedResponse(new CommandResponse(533, "Requested action not taken. Cannot Upload Due To Subdir"));
            return request;
        }
        try {
            ZipscriptVFSDataSFV sfvData = new ZipscriptVFSDataSFV(checkFile.getParent());
            SFVInfo sfv = sfvData.getSFVInfo();
            if (checkName.endsWith(".sfv")) {
                request.setAllowed(false);
                request.setDeniedResponse(new CommandResponse(533,
                        "Requested action not taken. Multiple SFV files not allowed."));
            } else if (sfvFirstEnforcedPath && !checkAllowedExtension(checkName)) {
                // filename not explicitly permitted, check for sfv entry
                boolean allow = false;
                if (restrictSfvEnabled) {
                    for (String name : sfv.getEntries().keySet()) {
                        if (name.toLowerCase().equals(checkName)) {
                            allow = true;
                            break;
                        }
                    }
                    if (!allow) {
                        request.setAllowed(false);
                        request.setDeniedResponse(new CommandResponse(533,
                                "Requested action not taken. File not found in sfv."));
                    }
                }
            } else {
                return request;
            }
        } catch (FileNotFoundException e1) {
            // no sfv found in dir
            if (!checkAllowedExtension(checkName) &&
                    sfvFirstEnforcedPath) {
                // filename not explicitly permitted
                // ForceSfvFirst is on, and file is in an enforced path.
                request.setAllowed(false);
                request.setDeniedResponse(new CommandResponse(533,
                        "Requested action not taken. You must upload sfv first."));
            }
        } catch (IOException e1) {
            // sfv not readable, do nothing
        } catch (NoAvailableSlaveException e1) {
            //sfv not online, do nothing
        } catch (SlaveUnavailableException e1) {
            //sfv not online, do nothing
        }
        return request;
    }

    private boolean checkAllowedExtension(String file) {
        if (_sfvFirstAllowNoExt && !file.contains(".")) {
            return true;
        }
        Properties cfg = ConfigLoader.loadPluginConfig("zipscript.conf");
        String allowedExts = cfg.getProperty("allowedexts", "") + " sfv";
        StringTokenizer st = new StringTokenizer(allowedExts);
        while (st.hasMoreElements()) {
            String ext = "." + st.nextElement().toString().toLowerCase();
            if (file.toLowerCase().endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkSfvFirstEnforcedPath(DirectoryHandle dir, User user) {
        return _sfvFirstRequired
                && GlobalContext.getConfig().checkPathPermission("sfvfirst.pathcheck",
                user, dir)
                && !GlobalContext.getConfig().checkPathPermission(
                "sfvfirst.pathignore", user, dir);
    }

    private boolean checkSfvDenySubdir(FileHandle file) {
        if ((file.getName().endsWith(".sfv")) && (_sfvDenySubDir)) {
            try {
                if (file.getPath().matches(_sfvDenySubDirExclude)) {
                    return false;
                }
            } catch (PatternSyntaxException e) {
                logger.debug("Bad Regex Patter for 'sfvdeny.subdir.exclude' '{}'", _sfvDenySubDirExclude);
            }

            try {
                for (DirectoryHandle dir : file.getParent().getDirectoriesUnchecked()) {
                    if (dir.getName().matches(_sfvDenySubDirInclude)) {
                        return true;
                    }
                }
            } catch (PatternSyntaxException e) {
                logger.debug("Bad Regex Patter for 'sfvdeny.subdir.include' '{}'", _sfvDenySubDirInclude);
            } catch (FileNotFoundException e) {
                // parent no longer exists....ignore
            }
        }
        return false;
    }
}
