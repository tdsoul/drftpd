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
package org.drftpd.zipscript.master.flac.indexation;

import org.drftpd.common.dynamicdata.Key;

/**
 * @author norox
 */
public class FlacQueryParams {

    public static final Key<FlacQueryParams> FLACQUERYPARAMS = new Key<>(FlacQueryParams.class, "flacqueryparams");

    private String _genre;
    private String _title;
    private String _artist;
    private String _album;
    private Integer _fromYear;
    private Integer _toYear;

    public String getGenre() {
        return _genre;
    }

    public void setGenre(String genre) {
        _genre = genre;
    }

    public String getTitle() {
        return _title;
    }

    public void setTitle(String title) {
        _title = title;
    }

    public String getArtist() {
        return _artist;
    }

    public void setArtist(String artist) {
        _artist = artist;
    }

    public String getAlbum() {
        return _album;
    }

    public void setAlbum(String album) {
        _album = album;
    }

    public Integer getMinYear() {
        return _fromYear;
    }

    public void setMinYear(Integer year) {
        _fromYear = year;
    }

    public Integer getMaxYear() {
        return _toYear;
    }

    public void setMaxYear(Integer year) {
        _toYear = year;
    }
}
