package com.qjf.backup.ui.log;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

import java.util.List;

public class LogClearService extends Service {

    // 自动清理日志
    @Override
    public void onCreate() {
        new Thread(() -> {
            DBHelper dbHelper = DBHelper.getInstance(getApplicationContext());
            int totalScanLog = dbHelper.countScanLog();
            if (totalScanLog > 200) {
                List<Long> scanLogIds = dbHelper.queryScanLogIdMore();// 找到200条记录以前的记录
                for (Long scanLogId : scanLogIds) {
                    // 先删除 上传日志
                    dbHelper.deleteBackLogByScanLogId(scanLogId);
                    dbHelper.deleteScanLogById(scanLogId);
                }
            }
        }).start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
