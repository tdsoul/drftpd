package net.sf.drftpd.remotefile;
import java.util.Iterator;

import net.sf.drftpd.slave.RemoteSlave;

import org.jdom.Element;

public class XMLSerialize {
	/*
	public static void main(String args[]) throws Exception {
		LinkedRemoteFile dir = new LinkedRemoteFile(null, new FileRemoteFile("/home/mog/dc", new File("/home/mog/dc")));
		Document doc = new Document(serialize(dir));
		new XMLOutputter("    ", true).output(doc, System.out);
	}
	*/
	public static Element serialize(LinkedRemoteFile file) {
		Element fileElement;
		if(file.isDirectory()) {
			fileElement = new Element("directory");
		} else {
			fileElement = new Element("file");
		}
		fileElement.setAttribute("name", file.getName());
		
		fileElement.addContent(new Element("user").setText(file.getUser()));
		fileElement.addContent(new Element("group").setText(file.getGroup()));
		if(file.isFile()) {
			fileElement.addContent(new Element("size").setText(Long.toString(file.length())));
		}
		fileElement.addContent(new Element("lastModified").setText(Long.toString(file.lastModified())));
		
		if(file.isDirectory()) {
			Element contents = new Element("contents");
			for(Iterator i = file.getFiles().values().iterator() ; i.hasNext(); ) {
				contents.addContent(serialize((LinkedRemoteFile)i.next()));
			}
			fileElement.addContent(contents);
		} else {
			String checksum = "";
			checksum = Long.toHexString(file.getCheckSum());

			fileElement.addContent(new Element("checksum").setText(checksum));
		}
		
		Element slaves = new Element("slaves");
		for(Iterator i = file.getSlaves().iterator(); i.hasNext(); ) {
			RemoteSlave slave = (RemoteSlave)i.next();
			slaves.addContent(new Element("slave").setAttribute("name", slave.getName()));
		}
		fileElement.addContent(slaves);
		
		return fileElement;
	}
}
