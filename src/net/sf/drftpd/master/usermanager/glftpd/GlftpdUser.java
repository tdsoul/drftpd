package net.sf.drftpd.master.usermanager.glftpd;

import java.util.Collection;
import java.util.Vector;

import net.sf.drftpd.DuplicateElementException;
import net.sf.drftpd.master.usermanager.AbstractUser;
import net.sf.drftpd.master.usermanager.UnixPassword;
import net.sf.drftpd.util.Crypt;

/**
 * 
 * @author mog
 * @author zubov
 * @version $Id: GlftpdUser.java,v 1.6 2004/01/14 18:20:01 mog Exp $
 */
public class GlftpdUser extends AbstractUser implements UnixPassword {
	private String password;
	//private String flags;
	private Vector privateGroups = new Vector();
	//private long weeklyAllotment;
	//private GlftpdUserManager usermanager;
	/**
	 * Constructor for GlftpdUser.
	 */
	public GlftpdUser(String username) {
		super(username);
		//this.usermanager = usermanager;
	}
	public void addPrivateGroup(String group)
		throws DuplicateElementException {
		addGroup(group);
		privateGroups.add(group);
	}
	public boolean checkPassword(String userPassword) {
		String userhash =
			Crypt.crypt(this.password.substring(0, 2), userPassword);
		if (password.equals(userhash)) {
			login();
			return true;
		} else {
			System.out.println(password + " != " + userhash);
		}
		return false;
	}
	public void commit() {
		throw new UnsupportedOperationException();
	}
	/* 
	 * no such thing in glftpd userfiles
	 */
	public long getCreated() {
		return 0;
	}

	public long getLastReset() {
		return System.currentTimeMillis();
	}
	/**
	* Sets the flags.
	* @param flags The flags to set
	*/
	//public void setFlags(String flags) {
	//	this.flags = flags;
	//}
	/**
	* Returns the privateGroups.
	* @return Vector
	*/
	public Collection getPrivateGroups() {
		return privateGroups;
	}

	public String getUnixPassword() {
		return password;
	}
	public long getWeeklyAllotment() {
		return weeklyAllotment;
	}
	public void purge() {
		throw new UnsupportedOperationException();
	}
	public void rename(String name) {
		throw new UnsupportedOperationException();
	}
	/**
	* Sets the password.
	* @param password The password to set
	*/
	public void setPassword(String password) {
		throw new UnsupportedOperationException();
		//must be encrypted...
		//this.password = password;
	}

	public void setUnixPassword(String password) {
		this.password = password;
	}
	public void setWeeklyAllotment(long weeklyAllotment) {
		this.weeklyAllotment = weeklyAllotment;
	}
	public void update() {
		throw new UnsupportedOperationException();
	}

	public void updateCredits(long credits) {
		throw new UnsupportedOperationException();
	}
	/**
	 * @see net.sf.drftpd.master.usermanager.User#updateDownloadedBytes(long)
	 */
	public void updateDownloadedBytes(long bytes) {
		throw new UnsupportedOperationException();
	}
	/**
	* @see net.sf.drftpd.master.usermanager.User#updateUploadedBytes(long)
	*/
	public void updateUploadedBytes(long bytes) {
		throw new UnsupportedOperationException();
	}

}