package com.qjf.backup.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.PowerManager;

public class MyWakeLockManager {
    private static PowerManager.WakeLock wakeLock;

    @SuppressLint("InvalidWakeLockTag")
    public static void acquireWakeLock(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        wakeLock.acquire();
    }

    public static void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }
}
