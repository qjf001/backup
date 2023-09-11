package com.qjf.backup.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.Map;
import java.util.Objects;

/**
 * SettingSharePreference
 */
public class SspUtil {

    public static SharedPreferences getShare(Context context) {
        return Objects.requireNonNull(context).getSharedPreferences("setting", Context.MODE_PRIVATE);
    }

    public static Map<String, String> getAll(Context context) {
        return (Map<String, String>) getShare(context).getAll();
    }

    public static String getOnlyWifiBackUp(Context context) {
        return getAll(context).getOrDefault("onlyWifiBackUp", "");
    }

    public static String getRemotePathByType(Context context, String type) {
        Map<String, String> map = getAll(context);
        if ("IMG".equals(type)) {
            return map.getOrDefault("imgPath", File.separator);
        } else if ("VIDEO".equals(type)) {
            return map.getOrDefault("videoPath", File.separator);
        } else if ("AUDIO".equals(type)) {
            return map.getOrDefault("audioPath", File.separator);
        }

        return File.separator;
    }

    public static String getBackUpLastDayByType(Context context, String type) {
        Map<String, String> map = SspUtil.getAll(context);
        if ("IMG".equals(type)) {
            return map.getOrDefault("imgLastBackupDatetime", "");
        } else if ("VIDEO".equals(type)) {
            return map.getOrDefault("videoLastBackupDatetime", "");
        } else if ("AUDIO".equals(type)) {
            return map.getOrDefault("audioLastBackupDatetime", "");
        }
        return "";
    }

    public static void saveBackUpLastDayByType(Context context, String type, String endQueryDateTime) {
        SharedPreferences shared = getShare(context);
        SharedPreferences.Editor editor = shared.edit();
        if ("IMG".equals(type)) {
            editor.putString("imgLastBackupDatetime", endQueryDateTime);
        } else if ("VIDEO".equals(type)) {
            editor.putString("videoLastBackupDatetime", endQueryDateTime);
        } else if ("AUDIO".equals(type)) {
            editor.putString("audioLastBackupDatetime", endQueryDateTime);
        }

        editor.apply();
    }

    public static String getSmbShareName(Context context) {
        return getAll(context).get("smbShareName");
    }

    public static String getAutoBackUp(Context context) {
        return getAll(context).getOrDefault("backUpStrategy", "").equals("autoBackUp") ? "Y" : "N";
    }

    public static boolean showHiddenSetting(Context context) {
        return getAll(context).getOrDefault("showHidden", "").equals("Y");
    }

    // 归档策略
    public static String getPlaceStrategy(Context context) {
        return getAll(context).getOrDefault("placeStrategy", "N");
    }
}
