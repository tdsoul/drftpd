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

package org.drftpd.slave.diskselection.filter;

import org.drftpd.slave.vfs.Root;

import java.security.SecureRandom;
import java.util.Properties;

/**
 * This filter simply pick a random root and adds 1 point to the current
 * ScoreChart making files spread throught all roots.
 *
 * @author fr0w
 * @version $Id$
 */
public class RandomspreadFilter extends DiskFilter {

    private final SecureRandom _rand = new SecureRandom();

    public RandomspreadFilter(DiskSelectionFilter diskSelection, Properties p, Integer i) {
        super(diskSelection, p, i);
    }

    public void process(ScoreChart sc, String path) {
        int i = _rand.nextInt(getRootList().size());
        Root root = getRootList().get(i);
        sc.addScore(root, 1);
    }
}
