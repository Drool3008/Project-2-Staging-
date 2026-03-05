/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.roller.weblogger.ui.struts2.editor;

import java.util.ResourceBundle;
import org.apache.commons.lang3.StringUtils;
import org.apache.roller.weblogger.pojos.MediaFileFilter;
import org.apache.roller.weblogger.pojos.MediaFileFilter.MediaFileOrder;

/**
 * Bean for holding media file search criteria.
 */
public class MediaFileSearchBean {
    private transient ResourceBundle bundle = ResourceBundle.getBundle("ApplicationResources");

    // Media file name as search criteria
    private String name;

    // Media file type as search criteria
    private String type;

    // Type of size filter as search criteria
    private String sizeFilterType;

    // Size of file as search criteria
    private long size;

    // Size unit
    private String sizeUnit;

    // Tags as search criteria
    private String tags;

    // Page number of results
    private int pageNum = 0;

    // Sort option for search results
    private int sortOption;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getTypeLabel() {
        return this.bundle.getString(type);
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSizeFilterType() {
        return sizeFilterType;
    }

    public void setSizeFilterType(String sizeFilterType) {
        this.sizeFilterType = sizeFilterType;
    }

    public String getSizeFilterTypeLabel() {
        return this.bundle.getString(sizeFilterType);
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getSizeUnit() {
        return sizeUnit;
    }

    public String getSizeUnitLabel() {
        return this.bundle.getString(sizeUnit);
    }

    public void setSizeUnit(String sizeUnit) {
        this.sizeUnit = sizeUnit;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getSortOption() {
        return sortOption;
    }

    public void setSortOption(int sortOption) {
        this.sortOption = sortOption;
    }

    /**
     * Copies data from this bean to media file filter object.
     *
     */
    /**
     * Copies data from this bean to media file filter object.
     * Delegates to MediaFileFilter factory method for conversion logic.
     */
    public void copyTo(MediaFileFilter dataHolder) {
        MediaFileFilter result = MediaFileFilter.fromSearchCriteria(
                this.name, this.type, this.sizeFilterType,
                this.size, this.sizeUnit, this.tags,
                this.pageNum, this.sortOption);

        // Copy result to the provided dataHolder
        dataHolder.setName(result.getName());
        dataHolder.setType(result.getType());
        dataHolder.setSizeFilterType(result.getSizeFilterType());
        dataHolder.setSize(result.getSize());
        dataHolder.setTags(result.getTags());
        dataHolder.setStartIndex(result.getStartIndex());
        dataHolder.setLength(result.getLength());
        dataHolder.setOrder(result.getOrder());
    }
}
