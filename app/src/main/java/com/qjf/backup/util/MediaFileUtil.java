package com.qjf.backup.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.MediaStore;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class MediaFileUtil {

    private static String[] mediaColumns = {MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_TAKEN
    };

    // 升序 ， 大日期在最后
    public static Map<String, List<FileLocalInfo>> convertToMap(List<FileLocalInfo> datas) {
        Map<String, List<FileLocalInfo>> map = datas.stream().collect(Collectors.groupingBy(FileLocalInfo::getCreateDateStr));
        TreeMap<String, List<FileLocalInfo>> treeMap = new TreeMap<>(Comparator.comparing(LocalDate::parse));
        treeMap.putAll(map);
        return treeMap;
    }

    @SuppressLint("Range")
    public static List<FileLocalInfo> getAndConvertByCursor(Context context, String type, String startQueryDateTime, String endQueryDateTime) throws ParseException {
        Cursor c = getCursor(context, type, startQueryDateTime, endQueryDateTime);
        int count = c.getCount();
        List<FileLocalInfo> files = new ArrayList<>(count);
        while (c.moveToNext()) {
            String path0 = c.getString(c.getColumnIndex(mediaColumns[1]));// 绝对路径
            FileLocalInfo localInfo = MeidaLocalFileUtil.getFileInfo(path0, type);
            if (Objects.nonNull(localInfo)) {
                files.add(localInfo);
            }
        }
        c.close();
        return files;
    }

    // fromQueryDateTime toQueryDateTime "yyyy-MM-dd HH:mm:ss"
    private static Cursor getCursor(Context context, String type, String startQueryDateTime, String endQueryDateTime) throws ParseException {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long endDayTimeLong = Objects.requireNonNull(sdf.parse(endQueryDateTime)).getTime();
        if (StringUtils.isNotBlank(startQueryDateTime)) {
            long startDayTimeLong = Objects.requireNonNull(sdf.parse(startQueryDateTime)).getTime();
            String selection = MediaStore.MediaColumns.DATE_TAKEN + " > ? and " + MediaStore.MediaColumns.DATE_TAKEN + " <= ?";
            String[] selectionArgs = new String[]{String.valueOf(startDayTimeLong), String.valueOf(endDayTimeLong)};
            return context.getContentResolver().query(getIntent(type).getData(), mediaColumns, selection, selectionArgs, MediaStore.MediaColumns.DATE_TAKEN + " DESC");
        } else {
            return context.getContentResolver().query(getIntent(type).getData(), mediaColumns, MediaStore.MediaColumns.DATE_TAKEN + " <= ?", new String[]{String.valueOf(endDayTimeLong)}, MediaStore.MediaColumns.DATE_TAKEN + " ASC");
        }
    }

    private static Intent getIntent(String type) {
        // 图片和视频不能同时获取吗？
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        switch (type) {
            case "IMG" -> i.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");// 图片
            case "VIDEO" -> i.setDataAndType(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*");// 获取视频
            case "AUDIO" -> i.setDataAndType(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio/*");// 获取音频
        }
        return i;
    }
}
