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
package org.drftpd.tvmaze.master.list;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.drftpd.common.slave.LightRemoteInode;
import org.drftpd.master.commands.list.AddListElementsInterface;
import org.drftpd.master.commands.list.ListElementsContainer;
import org.drftpd.master.vfs.DirectoryHandle;
import org.drftpd.tvmaze.master.TvMazeConfig;
import org.drftpd.tvmaze.master.TvMazeUtils;
import org.drftpd.tvmaze.master.metadata.TvMazeInfo;
import org.drftpd.tvmaze.master.vfs.TvMazeVFSData;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author lh
 */
public class TvMazeList implements AddListElementsInterface {
    private static final Logger logger = LogManager.getLogger(TvMazeList.class);

    public void initialize() { }

    public ListElementsContainer addElements(DirectoryHandle dir, ListElementsContainer container) {
        try {
            ResourceBundle bundle = container.getCommandManager().getResourceBundle();
            if (TvMazeConfig.getInstance().barEnabled()) {
                TvMazeVFSData tvmazeData = new TvMazeVFSData(dir);
                TvMazeInfo tvmazeInfo = tvmazeData.getTvMazeInfoFromCache();
                if (tvmazeInfo != null) {
                    Map<String, Object> env;
                    if (tvmazeInfo.getEPList().length == 1) {
                        env = TvMazeUtils.getEPEnv(tvmazeInfo, tvmazeInfo.getEPList()[0]);
                    } else {
                        env = TvMazeUtils.getShowEnv(tvmazeInfo);
                    }
                    String tvmazeDirName = container.getSession().jprintf(bundle, "tvmaze.dir", env, container.getUser());
                    try {
                        container.getElements().add(new LightRemoteInode(tvmazeDirName, "drftpd", "drftpd",
                                TvMazeConfig.getInstance().barAsDirectory(), dir.lastModified(), 0L));
                    } catch (FileNotFoundException e) {
                        // dir was deleted during list operation
                    }
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return container;
    }

    public void unload() {
        AnnotationProcessor.unprocess(this);
    }

}
