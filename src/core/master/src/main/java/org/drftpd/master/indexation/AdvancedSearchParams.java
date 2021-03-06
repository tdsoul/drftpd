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
package org.drftpd.master.indexation;

import org.drftpd.common.dynamicdata.Key;
import org.drftpd.common.dynamicdata.KeyNotFoundException;
import org.drftpd.common.dynamicdata.KeyedMap;

import java.util.Collections;
import java.util.Set;

/**
 * @author fr0w
 * @version $Id$
 */
public class AdvancedSearchParams {

    private String _name;
    private String _exact;
    private String _regex;
    private String _endsWith;
    private InodeType _inodeType = InodeType.ANY;
    private Set<String> _slaves = Collections.emptySet();
    private String _user;
    private String _group;
    private Long _minAge;
    private Long _maxAge;
    private Long _minSize;
    private Long _maxSize;
    private Integer _minSlaves;
    private Integer _maxSlaves;
    private String _sortField;
    private Boolean _sortOrder = false;
    private Integer _limit;
    private final KeyedMap<Key<?>, Object> _extensionMap;
    public AdvancedSearchParams() {
        _extensionMap = new KeyedMap<>();
    }

    public <T> void addExtensionData(Key<T> key, T data) {
        _extensionMap.put(key, data);
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public String getExact() {
        return _exact;
    }

    public void setExact(String exact) {
        _exact = exact;
    }

    public String getRegex() {
        return _regex;
    }

    public void setRegex(String regex) {
        _regex = regex;
    }

    public String getEndsWith() {
        return _endsWith;
    }

    public void setEndsWith(String endsWith) {
        _endsWith = endsWith;
    }

    public InodeType getInodeType() {
        return _inodeType;
    }

    public void setInodeType(InodeType type) {
        _inodeType = type;
    }

    public Set<String> getSlaves() {
        return _slaves;
    }

    public void setSlaves(Set<String> slaves) {
        _slaves = slaves;
    }

    public String getOwner() {
        return _user;
    }

    public void setOwner(String user) {
        _user = user;
    }

    public String getGroup() {
        return _group;
    }

    public void setGroup(String group) {
        _group = group;
    }

    public Long getMinAge() {
        return _minAge;
    }

    public void setMinAge(long minAge) {
        _minAge = minAge;
    }

    public Long getMaxAge() {
        return _maxAge;
    }

    public void setMaxAge(long maxAge) {
        _maxAge = maxAge;
    }

    public Long getMinSize() {
        return _minSize;
    }

    public void setMinSize(long minSize) {
        _minSize = minSize;
    }

    public Long getMaxSize() {
        return _maxSize;
    }

    public void setMaxSize(long maxSize) {
        _maxSize = maxSize;
    }

    public Integer getMinSlaves() {
        return _minSlaves;
    }

    public void setMinSlaves(int minSlaves) {
        _minSlaves = minSlaves;
    }

    public Integer getMaxSlaves() {
        return _maxSlaves;
    }

    public void setMaxSlaves(int maxSlaves) {
        _maxSlaves = maxSlaves;
    }

    public String getSortField() {
        return _sortField;
    }

    public void setSortField(String sortField) {
        _sortField = sortField;
    }

    public Boolean getSortOrder() {
        return _sortOrder;
    }

    public void setSortOrder(Boolean sortOrder) {
        _sortOrder = sortOrder;
    }

    public Integer getLimit() {
        return _limit;
    }

    public void setLimit(int limit) {
        _limit = limit;
    }

    public <T> T getExtensionData(Key<T> key) throws KeyNotFoundException {
        return _extensionMap.getObject(key);
    }

    public enum InodeType {
        DIRECTORY, FILE, ANY
    }
}
