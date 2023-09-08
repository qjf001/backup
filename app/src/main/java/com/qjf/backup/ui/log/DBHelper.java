package com.qjf.backup.ui.log;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.qjf.backup.ui.log.entity.BackupLog;
import com.qjf.backup.ui.log.entity.ScanLogOut;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class DBHelper extends SQLiteOpenHelper {

    private static DBHelper mInstance = null;

    public synchronized static DBHelper getInstance(Context context) {
        if (Objects.isNull(mInstance)) {
            mInstance = new DBHelper(context);
        }
        return mInstance;
    }

    private static final String DATABASE_NAME = "backup_data.db";
    private static final int DATABASE_VERSION = 1;

    private DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "create table back_log(id integer primary key autoincrement,scan_id integer , upload_date text, file_name text,file_size integer,result integer,upload_time text,take_mill integer, err_msg)";
        db.execSQL(sql);

        String scanSql = "create table scan_log(id integer primary key autoincrement, scan_date text, scan_type text,total_files integer,total_size integer,scan_time text,err_msg)";
        db.execSQL(scanSql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public int countScanLog() {
        String sql = " select count(*) from  scan_log";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(sql, null);
        int total = 0;
        if (cursor.moveToNext()) {
            total = cursor.getInt(0);
        }
        if (Objects.nonNull(cursor)) {
            cursor.close();
        }
        return total;
    }

    @SuppressLint({"Range", "Recycle"})
    public List<Long> queryScanLogIdMore() {
        String sql = " select id from scan_log order by id desc ";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(sql, null);
        List<Long> datas = new ArrayList<>(cursor.getCount() - 200);
        cursor.move(199);
        while (cursor.moveToNext()) {
            Long id = cursor.getLong(cursor.getColumnIndex("id"));
            datas.add(id);
        }
        cursor.close();

        return datas;
    }

    // String table, String[] columns, String selection,
    //            String[] selectionArgs, String groupBy, String having,
    //            String orderBy, String limit
    @SuppressLint({"Range", "Recycle"})
    public List<ScanLogOut> queryScanLog(int pageNum) {
        String sql = " select id, scan_date as scanDate, scan_type as scanType, total_files AS totalFiles, total_size AS totalSize, scan_time AS scanTime, err_msg AS errMsg from scan_log order by id desc limit " + (pageNum - 1) * 200 + ", 200";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(sql, null);
        List<ScanLogOut> datas = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            Long id = cursor.getLong(cursor.getColumnIndex("id"));
            String scanDate = cursor.getString(cursor.getColumnIndex("scanDate"));
            String type = cursor.getString(cursor.getColumnIndex("scanType"));
            int totalFiles = cursor.getInt(cursor.getColumnIndex("totalFiles"));
            long totalSize = cursor.getLong(cursor.getColumnIndex("totalSize"));
            String scanTime = cursor.getString(cursor.getColumnIndex("scanTime"));
            String errMsg = cursor.getString(cursor.getColumnIndex("errMsg"));
            ScanLogOut log = new ScanLogOut(id, scanDate, type, totalFiles, totalSize, scanTime, errMsg);
            datas.add(log);
        }
        cursor.close();

        List<Long> scanIds = datas.stream().map(ScanLogOut::getId).collect(Collectors.toList());
        Map<Long, Integer> countMap = countSuccUploadRecordByScanId(scanIds);
        datas = datas.stream().peek(scanLog -> scanLog.setUploadSuccCount(countMap.getOrDefault(scanLog.getId(), 0))).collect(Collectors.toList());

        return datas;
    }

    public Long insertScanLog(String scanType) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("scan_date", LocalDate.now(ZoneId.of("+8")).toString());
        values.put("scan_type", scanType);
        values.put("scan_time", LocalTime.now(ZoneId.of("+8")).toString());
        return db.insert("scan_log", null, values);
    }

    public int upScanFileInfo(Long id, Integer totalFiles, Long totalSize) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("total_files", totalFiles);
        values.put("total_size", totalSize);
        return db.update("scan_log", values, "id=?", new String[]{id.toString()});
    }

    public int upScanFailLog(Long id, String errMsg) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("err_msg", errMsg);
        return db.update("scan_log", values, "id=?", new String[]{id.toString()});
    }

    public void insertBackLog(Long scanId, String fileName, Long fileSize, int result, Long takeMill, String errMsg) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("scan_id", scanId);
        values.put("upload_date", LocalDate.now(ZoneId.of("+8")).toString());
        values.put("file_name", fileName);
        values.put("file_size", fileSize);
        values.put("result", result);
        values.put("upload_time", LocalTime.now(ZoneId.of("+8")).toString());
        values.put("take_mill", takeMill);
        values.put("err_msg", errMsg);
        db.insert("back_log", null, values);
    }

    @SuppressLint("Range")
    public Map<Long, Integer> countSuccUploadRecordByScanId(List<Long> scanIds) {
        if (scanIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String questionMark = scanIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "select count(1) as total, scan_id as scanId from back_log where scan_id in (" + questionMark + ") and result=1 group by scan_id";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(sql, scanIds.stream().map(String::valueOf).collect(Collectors.toList()).toArray(new String[scanIds.size()]));

        Map<Long, Integer> countMap = new HashMap<>(scanIds.size());
        while (cursor.moveToNext()) {
            Long scanId = cursor.getLong(cursor.getColumnIndex("scanId"));
            Integer total = cursor.getInt(cursor.getColumnIndex("total"));
            countMap.put(scanId, total);
        }
        cursor.close();
        return countMap;
    }


    @SuppressLint({"Range", "Recycle"})
    public List<BackupLog> queryUploadLog(String scanId) {
        String sql = " select id, upload_date as uploadDate, upload_time as uploadTime, file_name as fileName, file_size AS fileSize, result ,take_mill as takeMill, err_msg AS errMsg from back_log where scan_id = ? order by id desc";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(sql, new String[]{scanId});
        List<BackupLog> datas = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndex("id"));
            String uploadDate = cursor.getString(cursor.getColumnIndex("uploadDate"));
            String uploadTime = cursor.getString(cursor.getColumnIndex("uploadTime"));
            String fileName = cursor.getString(cursor.getColumnIndex("fileName"));
            long fileSize = cursor.getLong(cursor.getColumnIndex("fileSize"));
            int result = cursor.getInt(cursor.getColumnIndex("result"));
            int takeMill = cursor.getInt(cursor.getColumnIndex("takeMill"));
            String errMsg = cursor.getString(cursor.getColumnIndex("errMsg"));
            datas.add(new BackupLog(id, uploadDate, uploadTime, fileName, fileSize, result, takeMill, errMsg));
        }

        return datas;
    }


    public void deleteBackLogByScanLogId(Long scanLogId) {
        String sql = " delete from back_log where scan_id = ?";
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(sql, new Object[]{scanLogId});
    }

    public void deleteScanLogById(Long id) {
        String sql = " delete from scan_log where id = ?";
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(sql, new Object[]{id});
    }
}
