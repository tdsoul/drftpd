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

import org.drftpd.master.exceptions.NoAvailableSlaveException;
import org.drftpd.master.slavemanagement.DummyRemoteSlave;
import org.drftpd.master.slavemanagement.RemoteSlave;
import org.drftpd.master.util.Time;
import org.drftpd.slave.network.Transfer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author mog
 * @version $Id$
 */
public class MintimeonlineFilterTest {

    @Test
    public void testSimple() throws NoAvailableSlaveException {
        Properties p = new Properties();
        p.put("1.multiplier", "1");
        p.put("1.mintime", "2m");

        long time = System.currentTimeMillis();
        RemoteSlave[] rslaves = {new RS("slave1", time)};
        ScoreChart sc = new ScoreChart(Arrays.asList(rslaves));
        MintimeonlineFilter f = new MintimeonlineFilter(1, p);
        f.process(sc, null, null, Transfer.TRANSFER_UNKNOWN, null, time);
        assertEquals(-Time.parseTime("1m"), sc.getBestSlaveScore().getScore());
    }

    public static class RS extends DummyRemoteSlave {
        private final long _time;

        public RS(String name, long time) {
            super(name);
            _time = time;
        }

        public long getLastTransferForDirection(char dir) {
            return _time - Time.parseTime("1m");
        }
    }
}
