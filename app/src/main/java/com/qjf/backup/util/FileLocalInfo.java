package com.qjf.backup.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

public class FileLocalInfo {
    private String name;
    private String path;
    private Long size;
    private Date createDate;

    private String createDateStr;

    public FileLocalInfo() {
    }

    public FileLocalInfo(String path, Map<String, Object> dataMap) {
        Date d = getCreateDate(dataMap);
        String dstr = Objects.nonNull(d) ? d.toInstant().atZone(ZoneId.of("+8")).toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
        this.path = path;
        this.name = (String) dataMap.get("fileName");
        this.size = (Long) dataMap.getOrDefault("fileSize", 0);
        this.createDate = d;
        this.createDateStr = dstr;
    }

    private Date getCreateDate(Map<String, Object> dataMap) {
        Date d1 = (Date) dataMap.get("date1");
        Date d2 = (Date) dataMap.get("date2");
        Date d3 = (Date) dataMap.get("date3");
        Date d4 = (Date) dataMap.get("date4");
        Date d5 = (Date) dataMap.get("date5");
        if (Objects.nonNull(d1)) {
            return d1;
        } else if (Objects.nonNull(d2)) {
            return d2;
        } else if (Objects.nonNull(d3)) {
            return d3;
        } else if (Objects.nonNull(d4)) {
            return d4;
        } else if (Objects.nonNull(d5)) {
            return d5;
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getCreateDateStr() {
        return createDateStr;
    }

    public void setCreateDateStr(String createDateStr) {
        this.createDateStr = createDateStr;
    }
}
