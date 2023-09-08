package com.qjf.backup.ui.log.entity;

import com.qjf.backup.util.ByteConvert;
import com.qjf.backup.util.FileLocalInfo;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class BackupLog {
    private Long id;

    private String uploadDate;
    private String uploadTime;

    private String fileName;

    private String fileSize;

    private String result;

    private String takeMill;

    private String errMsg;

    public BackupLog() {
    }

    // 只有成功和失败的日志，没有记录已经存在，不上传的日志
    public BackupLog(Long id, String uploadDate, String uploadTime, String fileName, Long fileSize, Integer result, Integer takeMill, String errMsg) {
        this.id = id;
        this.uploadDate = uploadDate.replaceAll("-", "/").substring(6) + " " + uploadTime.substring(0, 8);
//        this.uploadDate = uploadDate;
//        this.uploadTime = uploadTime;
        this.fileName = fileName;
        this.fileSize = ByteConvert.convertToStr(fileSize);
        this.result = result == 0 ? "失败" : "成功";
        this.takeMill = takeMill / 1000 + "." + takeMill % 1000 + "秒";
        this.errMsg = errMsg;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(String uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getTakeMill() {
        return takeMill;
    }

    public void setTakeMill(String takeMill) {
        this.takeMill = takeMill;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public static BackupLog convertBy(FileLocalInfo localInfo){
        BackupLog log = new BackupLog();
        log.setFileName(localInfo.getName());
        log.setFileSize(localInfo.getSize()+"");
        log.setUploadDate(LocalDate.now(ZoneId.of("+8")).toString());
        log.setUploadTime(LocalTime.now(ZoneId.of("+8")).format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        log.setResult("1");
        return log;
    }
}
