package com.qjf.backup.ui.home;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import com.qjf.backup.ui.log.DBHelper;
import com.qjf.backup.util.MyWakeLockManager;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class UploadTimerService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        // 在此处做一些初始化操作
        Timer timer = new Timer("UploadMediaFileThread", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // 在此处执行需要定时执行的任务
                try {
                    MyWakeLockManager.acquireWakeLock(getApplicationContext());
                    for (String type : Arrays.asList("IMG", "VIDEO", "AUDIO")) {
                        Long scanLogId = DBHelper.getInstance(getApplicationContext()).insertScanLog(type);
                        try {
                            new MediaFileBackUp().syncMediaToRemote(type, getApplicationContext(), scanLogId);
                        } catch (Exception e) {
                            // 屏幕失去焦点是可能触发该异常 com.hierynomus.protocol.transport.TransportException: java.net.SocketException: Broken pipe
                            Log.v(type + "文件同步失败", e.getMessage());// Can't toast on a thread that has not called Looper.prepare()
                            DBHelper.getInstance(getApplicationContext()).upScanFailLog(scanLogId, e.getMessage());
                        }
                    }
                } finally {
                    MyWakeLockManager.releaseWakeLock();
                }

            }
        }, 2000000000, 3600000 * 12);// delay表示任务的延迟执行时间（以毫秒为单位），period 表示任务的间隔执行时间（以毫秒为单位）
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
