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
package org.drftpd.master.usermanager;

import org.drftpd.common.dynamicdata.Key;
import org.drftpd.common.dynamicdata.KeyedMap;
import org.drftpd.common.exceptions.DuplicateElementException;

import java.util.Date;
import java.util.List;

/**
 * @author mikevg
 * @version $Id$
 */
public abstract class Group implements Entity {
    public abstract UserManager getUserManager();

    protected abstract KeyedMap<Key<?>, Object> getKeyedMap();

    /**
     * Commit changes.
     */
    public abstract void commit();

    public abstract String getName();

    public abstract void rename(String groupname) throws GroupExistsException, GroupFileException;

    public abstract List<User> getAdmins();

    public abstract boolean isAdmin(User u);

    // useless for group objects
    public boolean isMemberOf(String ignore) {
        return false;
    }

    public abstract void purge();

    public abstract void addAdmin(User u) throws DuplicateElementException;

    public abstract void removeAdmin(User u) throws NoSuchFieldException;

    public abstract float getMinRatio();

    public abstract void setMinRatio(float minRatio);

    public abstract float getMaxRatio();

    public abstract void setMaxRatio(float MaxRatio);

    public abstract int getGroupSlots();

    public abstract void setGroupSlots(int slots);

    public abstract int getLeechSlots();

    public abstract void setLeechSlots(int leechslots);

    public abstract Date getCreated();

    public abstract void setCreated(Date created);

}
