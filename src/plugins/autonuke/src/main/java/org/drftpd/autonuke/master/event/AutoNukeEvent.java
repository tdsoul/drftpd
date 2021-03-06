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
package org.drftpd.autonuke.master.event;

import org.drftpd.autonuke.master.NukeItem;

import java.util.LinkedList;

/**
 * @author scitz0
 */
public class AutoNukeEvent {

    private final NukeItem _nukeItem;
    private final String _ircString;
    private final LinkedList<String> _data;

    public AutoNukeEvent(NukeItem nukeItem, String ircString, LinkedList<String> data) {
        _nukeItem = nukeItem;
        _ircString = ircString;
        _data = data;
    }

    public NukeItem getNukeItem() {
        return _nukeItem;
    }

    public String getIRCString() {
        return _ircString;
    }

    public LinkedList<String> getData() {
        return _data;
    }

}
