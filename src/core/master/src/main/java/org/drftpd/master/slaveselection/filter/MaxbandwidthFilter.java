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
package org.drftpd.master.slaveselection.filter;

import org.drftpd.common.util.Bytes;
import org.drftpd.common.util.PropertyHelper;
import org.drftpd.common.vfs.InodeHandleInterface;
import org.drftpd.master.exceptions.NoAvailableSlaveException;
import org.drftpd.master.exceptions.SlaveUnavailableException;
import org.drftpd.master.slavemanagement.RemoteSlave;
import org.drftpd.master.slavemanagement.SlaveStatus;
import org.drftpd.master.usermanager.User;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Properties;

/**
 * @author zubov
 * @version $Id$
 */
public class MaxbandwidthFilter extends Filter {
    private final long _maxBandwidth;

    public MaxbandwidthFilter(int i, Properties p) {
        super(i, p);
        String max = PropertyHelper.getProperty(p, i + ".maxbandwidth");
        int slashIndex = max.indexOf('/'); // parse kb/s too.

        if (slashIndex != -1) {
            max = max.substring(0, slashIndex - 1);
        }

        _maxBandwidth = Bytes.parseBytes(max);
    }

    public void process(ScoreChart scorechart, User user, InetAddress peer,
                        char direction, InodeHandleInterface dir, RemoteSlave sourceSlave)
            throws NoAvailableSlaveException {
        for (Iterator<ScoreChart.SlaveScore> iter = scorechart.getSlaveScores().iterator(); iter
                .hasNext(); ) {
            ScoreChart.SlaveScore slavescore = iter.next();
            SlaveStatus status;

            try {
                status = slavescore.getRSlave().getSlaveStatusAvailable();
            } catch (SlaveUnavailableException e) {
                // how come the slave is offline? it was just online.
                iter.remove();
                continue;
            }

            if (status.getThroughputDirection(direction) > _maxBandwidth) {
                iter.remove();
            }
        }
    }
}
