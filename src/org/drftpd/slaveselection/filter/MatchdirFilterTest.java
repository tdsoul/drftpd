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
package org.drftpd.slaveselection.filter;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sf.drftpd.NoAvailableSlaveException;
import net.sf.drftpd.ObjectNotFoundException;
import net.sf.drftpd.master.RemoteSlave;
import net.sf.drftpd.master.SlaveManagerImpl;
import net.sf.drftpd.slave.Transfer;

import org.drftpd.remotefile.AbstractLinkedRemoteFile;

/**
 * @author mog
 * @version $Id: MatchdirFilterTest.java,v 1.2 2004/02/26 21:11:08 mog Exp $
 */
public class MatchdirFilterTest extends TestCase {

	public static class LinkedRemoteFilePath extends AbstractLinkedRemoteFile {
		private String _path;
		public LinkedRemoteFilePath(String path) {
			_path = path;
		}
		public String getPath() {
			return _path;
		}
	}
	public MatchdirFilterTest(String fName) {
		super(fName);
	}

	public static TestSuite suite() {
		return new TestSuite(MatchdirFilterTest.class);
	}

	public class SSM extends SlaveSelectionManager {
		public SlaveManagerImpl getSlaveManager() {
			try {
				return new SM();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public class SM extends SlaveManagerImpl {
		public SM() throws RemoteException {
			super();
		}
		public RemoteSlave getSlave(String s) throws ObjectNotFoundException {
			assert s != null;
			assert rslaves[0] != null;
			if (s.equals(rslaves[0].getName()))
				return rslaves[0];
			if (s.equals(rslaves[1].getName()))
				return rslaves[1];
			throw new ObjectNotFoundException();
		}

	}

	RemoteSlave rslaves[] =
		{
			new RemoteSlave("slave1", Collections.EMPTY_LIST),
			new RemoteSlave("slave2", Collections.EMPTY_LIST),
			new RemoteSlave("slave3", Collections.EMPTY_LIST)};

	public void testSimple() throws ObjectNotFoundException, NoAvailableSlaveException {
		Properties p = new Properties();
		p.put("1.assignslave", "slave1+100,slave2-100");
		p.put("1.expr", "/path1/*");

		Filter f = new MatchdirFilter(new SSM(), 1, p);
		ScoreChart sc = new ScoreChart(Arrays.asList(rslaves));

		f.process(sc, null, null,Transfer.TRANSFER_SENDING_DOWNLOAD, new LinkedRemoteFilePath("/path2/dir/file.txt"));
		assertEquals(0, sc.getSlaveScore(rslaves[0]).getScore());
		assertEquals(0, sc.getSlaveScore(rslaves[1]).getScore());
		assertEquals(0, sc.getSlaveScore(rslaves[2]).getScore());

		f.process(sc, null, null,Transfer.TRANSFER_SENDING_DOWNLOAD, new LinkedRemoteFilePath("/"));
		assertEquals(0, sc.getSlaveScore(rslaves[0]).getScore());
		assertEquals(0, sc.getSlaveScore(rslaves[1]).getScore());
		assertEquals(0, sc.getSlaveScore(rslaves[2]).getScore());

		f.process(sc, null, null,Transfer.TRANSFER_SENDING_DOWNLOAD, new LinkedRemoteFilePath("/path1/dir/file.txt"));
		assertEquals(100, sc.getSlaveScore(rslaves[0]).getScore());
		assertEquals(-100, sc.getSlaveScore(rslaves[1]).getScore());
		assertEquals(0, sc.getSlaveScore(rslaves[2]).getScore());
	}
	public void testAll() throws ObjectNotFoundException, NoAvailableSlaveException {
		Properties p = new Properties();
		p.put("1.assign", "ALL+100");
		p.put("1.expr", "/path2/*");

		Filter f = new MatchdirFilter(new SSM(), 1, p);
		ScoreChart sc = new ScoreChart(Arrays.asList(rslaves));

		f.process(sc, null, null,Transfer.TRANSFER_SENDING_DOWNLOAD, new LinkedRemoteFilePath("/path1/dir/file.txt"));
		assertEquals(0, sc.getSlaveScore(rslaves[0]).getScore());
		assertEquals(0, sc.getSlaveScore(rslaves[1]).getScore());
		assertEquals(0, sc.getSlaveScore(rslaves[2]).getScore());

		f.process(sc, null, null,Transfer.TRANSFER_SENDING_DOWNLOAD, new LinkedRemoteFilePath("/path2/dir/file.txt"));
		assertEquals(100, sc.getSlaveScore(rslaves[0]).getScore());
		assertEquals(100, sc.getSlaveScore(rslaves[1]).getScore());
		assertEquals(100, sc.getSlaveScore(rslaves[2]).getScore());
	}
}