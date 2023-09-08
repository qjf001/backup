package com.qjf.backup.ui.log.entity;

import com.qjf.backup.util.ByteConvert;
import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;

public class ScanLogOut {
    private Long id = 0L;
    private String scanDate = "";

    private String scanType = "";

    private Integer totalFiles = 0;

    private String totalSize = "";

    private String scanTime = "";

    private boolean scanSucc = true;

    private String errMsg = "";

    private boolean clickUpLoadList = false;

    private Integer uploadSuccCount = 0;

    public ScanLogOut() {

    }

    // "\r\n"
    public ScanLogOut(Long id, String scanDate, String scanType, Integer totalFiles, Long totalSize, String scanTime, String errMsg) {
        this.id = id;
        if (id != 0) {
            this.scanDate = scanDate.replaceAll("-", "/").substring(6) + " " + scanTime.substring(0, 8);
            this.scanType = convertScanType(scanType);
            this.totalFiles = totalFiles;
            this.totalSize = ByteConvert.convertToStr(totalSize);
            this.scanSucc = StringUtils.isBlank(errMsg);
            this.clickUpLoadList = convertClickVal(this.scanSucc, totalFiles);
            this.errMsg = errMsg;
        }
    }

    private static boolean convertClickVal(boolean scanSucc, Integer totalFiles) {
        return scanSucc && totalFiles > 0;
    }

    private static String convertScanType(String srcType) {
        switch (srcType) {
            case "AUDIO":
                return "音频";
            case "IMG":
                return "图片";
            case "VIDEO":
                return "视频";
            default:
                return srcType;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getScanDate() {
        return scanDate;
    }

    public void setScanDate(String scanDate) {
        this.scanDate = scanDate;
    }

    public String getScanType() {
        return scanType;
    }

    public void setScanType(String scanType) {
        this.scanType = scanType;
    }

    public Integer getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(Integer totalFiles) {
        this.totalFiles = totalFiles;
    }

    public String getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(String totalSize) {
        this.totalSize = totalSize;
    }

    public String getScanTime() {
        return scanTime;
    }

    public void setScanTime(String scanTime) {
        this.scanTime = scanTime;
    }

    public boolean isScanSucc() {
        return scanSucc;
    }

    public void setScanSucc(boolean scanSucc) {
        this.scanSucc = scanSucc;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public boolean isClickUpLoadList() {
        return clickUpLoadList;
    }

    public void setClickUpLoadList(boolean clickUpLoadList) {
        this.clickUpLoadList = clickUpLoadList;
    }

    public Integer getUploadSuccCount() {
        return uploadSuccCount;
    }

    public void setUploadSuccCount(Integer uploadSuccCount) {
        this.uploadSuccCount = uploadSuccCount;
        this.clickUpLoadList = this.clickUpLoadList && uploadSuccCount > 0;
    }
}
