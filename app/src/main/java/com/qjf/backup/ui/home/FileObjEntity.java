package com.qjf.backup.ui.home;

import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.protocol.commons.EnumWithValue;

public class FileObjEntity {
    private String fileName;

    private Integer size;

    private boolean isDir;

    private long childrenDirCount = 0;

    private long childrenFileCount = 0;

    public FileObjEntity(String fileName, Integer size, boolean isDir) {
        this.fileName = fileName;

        this.size = size;
        this.isDir = isDir;
    }

    public static int compareBy(FileObjEntity obj1, FileObjEntity obj2) {
        return Boolean.compare(obj1.isDir(), obj2.isDir());
    }

    public static FileObjEntity convert(FileIdBothDirectoryInformation remoteFile) {
        boolean isDir = EnumWithValue.EnumUtils.isSet(remoteFile.getFileAttributes(), FileAttributes.FILE_ATTRIBUTE_DIRECTORY);
        return new FileObjEntity(remoteFile.getFileName(), Long.valueOf(remoteFile.getEndOfFile()).intValue(), isDir);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public boolean isDir() {
        return isDir;
    }

    public void setDir(boolean dir) {
        isDir = dir;
    }

    public long getChildrenDirCount() {
        return childrenDirCount;
    }

    public void setChildrenDirCount(long childrenDirCount) {
        this.childrenDirCount = childrenDirCount;
    }

    public long getChildrenFileCount() {
        return childrenFileCount;
    }

    public void setChildrenFileCount(long childrenFileCount) {
        this.childrenFileCount = childrenFileCount;
    }
}
