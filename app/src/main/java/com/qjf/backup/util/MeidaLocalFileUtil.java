package com.qjf.backup.util;


import android.util.Log;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.mp3.Mp3MetadataReader;
import com.drew.imaging.mp4.Mp4MetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.file.FileSystemDirectory;
import com.drew.metadata.mp4.Mp4Directory;
import com.drew.metadata.mp4.media.Mp4MetaDirectory;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

public class MeidaLocalFileUtil {

    // Exif IFD0 Date/Time 306  class=com.drew.metadata.exif.ExifIFD0Directory
    // Exif SubIFD   Date/Time Digitized 36868;   Date/Time Original 36867;  class=class com.drew.metadata.exif.ExifSubIFDDirectory
    // GPS   Date Stamp  tagType=29   class=class com.drew.metadata.exif.GpsDirectory
    // class=class com.drew.metadata.Metadata
    // class=class com.drew.metadata.file.FileTypeDirectory
    // File Modified Date tagType=3 class=class com.drew.metadata.file.FileSystemDirectory
    public static FileLocalInfo getFileInfo(String fileLocalPath, String type) {
        Metadata metadata = readMetadata(fileLocalPath, type);//  ".HEIC"  访问文件被拒绝，权限不够？？
        if (Objects.isNull(metadata)) {
            return null;
        }
        Map<String, Object> fileDataMap = new HashMap<>();
        for (Directory directory : metadata.getDirectories()) {
            if ("VIDEO".equals(type)) {
                getVideoInfo(fileDataMap, directory);
            }

            if ("IMG".equals(type)) {
                getImgeInfo(fileDataMap, directory);
            }

            // 图片、音频、视频 共有属性
            if (FileSystemDirectory.class.equals(directory.getClass())) {
                String fileName = directory.getString(1);
                if (StringUtils.isNotBlank(fileName)) {
                    fileDataMap.put("fileName", fileName);
                }
                Long fileSize = directory.getLongObject(2);
                if (Objects.nonNull(fileSize) && fileSize > 0) {
                    fileDataMap.put("fileSize", fileSize);
                }

                Optional<String> optStr = directory.getTags().stream().filter(tag -> tag.getTagType() == 3 && StringUtils.isNotBlank(tag.getDescription())).map(Tag::getDescription).findFirst();
                optStr.ifPresent(str -> fileDataMap.put("date5", StrToDate.convert(str)));
            }

            // png Date/Time Original tagType=36867;  HEIC Date/Time tagType=306, Date/Time Original tagType=36867,  Date/Time Digitized tagType=36868
            // File Modified Date tagType=3 ; Date Created tagType=567 ;  GPS Date Stamp  tagType=29
            // Profile Date/Time  tagType=24 文件移动时的创建时间，不可取
        }

        FileLocalInfo fileLocalInfo = new FileLocalInfo(fileLocalPath, fileDataMap);
//        Log.v("file", JSONObject.toJSONString(fileLocalInfo));
        return fileLocalInfo;
    }

    private static void getVideoInfo(Map<String, Object> fileDataMap, Directory directory) {
        // 视频： MOV有这个类，mp4没有这个类
        if (Mp4MetaDirectory.class.equals(directory.getClass())) {//  tagtype = 101
            directory.getTags().stream().filter(tag -> tag.getTagType() == 101 && StringUtils.isNotBlank(tag.getDescription())).map(Tag::getDescription).findFirst().ifPresent(tagDate ->
                    fileDataMap.put("date1", StrToDate.convert(tagDate)));
        }

        // 视频
        if (Mp4Directory.class.equals(directory.getClass())) {//  tagtype = 256
            directory.getTags().stream().filter(tag -> tag.getTagType() == 256 && StringUtils.isNotBlank(tag.getDescription())).map(Tag::getDescription).findFirst().ifPresent(tagDate ->
                    fileDataMap.put("date2", StrToDate.convert(tagDate)));
        }
    }

    private static void getImgeInfo(Map<String, Object> fileDataMap, Directory directory) {
        // 图片属性
        if (ExifSubIFDDirectory.class.equals(directory.getClass())) {
            directory.getTags().stream().filter(tag -> (tag.getTagType() == 36867 || tag.getTagType() == 36867) && StringUtils.isNotBlank(tag.getDescription())).forEach(tag -> {
                Date d = StrToDate.convert(tag.getDescription());
                if (Objects.nonNull(d)) {
                    fileDataMap.put(tag.getTagType() == 36867 ? "date1" : "date2", d);
                }
            });
        }

        // 图片属性
        if (ExifIFD0Directory.class.equals(directory.getClass())) {
            // 2023:08:13 17:48:18
            Optional<String> optStr = directory.getTags().stream().filter(tag -> tag.getTagType() == 306 && StringUtils.isNotBlank(tag.getDescription())).map(Tag::getDescription).findFirst();
            optStr.ifPresent(str -> fileDataMap.put("date3", StrToDate.convert(str)));
        }
    }

    private static Metadata readMetadata(String fileLocalPath, String type) {
        Metadata metadata = null;
        try {
            if ("IMG".equals(type)) {// 导入的 iphone 拍摄的 ".HEIC" 文件，读取时会出现权限问题，本机设备拍摄的没有问题
                metadata = ImageMetadataReader.readMetadata(new File(fileLocalPath));
            } else if ("AUDIO".equals(type)) {
                // 能读取到 .awb 文件（android的录音文件）
                // 读取不到 小米手机的录音文件，需要将录音文件导出到外部存储卡才能读取到
                metadata = Mp3MetadataReader.readMetadata(new File(fileLocalPath));
            } else if ("VIDEO".equals(type)) {
                metadata = Mp4MetadataReader.readMetadata(new File(fileLocalPath));
            }
        } catch (Exception e) {// V/读取媒体文件MetaData: file=/storage/emulated/0/IMG_0010.HEIC  /storage/emulated/0/IMG_0010.HEIC: open failed: EACCES (Permission denied)
            Log.v("读取媒体文件MetaData失败", "file=" + fileLocalPath + "  " + e.getMessage());
        }
        return metadata;
    }

}
