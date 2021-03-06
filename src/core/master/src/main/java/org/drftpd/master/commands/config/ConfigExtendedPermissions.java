package org.drftpd.master.commands.config;

import org.drftpd.master.commands.config.hooks.DefaultConfigHandler;
import org.drftpd.master.permissions.ExtendedPermissions;
import org.drftpd.master.permissions.PermissionDefinition;

import java.util.Arrays;
import java.util.List;

public class ConfigExtendedPermissions implements ExtendedPermissions {

    @Override
    public List<PermissionDefinition> permissions() {
        PermissionDefinition hideInWho = new PermissionDefinition("hideinwho",
                DefaultConfigHandler.class, "handlePathPerm");
        PermissionDefinition rejectSecure = new PermissionDefinition("userrejectsecure",
                DefaultConfigHandler.class, "handlePerm");
        PermissionDefinition rejectInsecure = new PermissionDefinition("userrejectinsecure",
                DefaultConfigHandler.class, "handlePerm");
        PermissionDefinition denyDirUncrypted = new PermissionDefinition("denydiruncrypted",
                DefaultConfigHandler.class, "handlePerm");
        PermissionDefinition denyDataUncrypted = new PermissionDefinition("denydatauncrypted",
                DefaultConfigHandler.class, "handlePerm");
        PermissionDefinition msgPath = new PermissionDefinition("msgpath",
                DefaultConfigHandler.class, "handleMsgPath");
        return Arrays.asList(hideInWho, rejectSecure, rejectInsecure, denyDataUncrypted, denyDirUncrypted, msgPath);
    }
}
