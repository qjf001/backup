package com.qjf.backup.util;

import android.util.Log;
import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StrToDate {

    public static Date convert(String strDate) {
        if (StringUtils.isBlank(strDate)) {
            return null;
        }

        try {
            if (strDate.contains(" CTS ")) {
                return cstDate(strDate);
            }

            if (strDate.contains("周") && strDate.contains("月")) {
                return chinaDate(strDate);
            }

            if (strDate.length() == 19 && strDate.substring(0, 10).matches("\\d{4}:\\d{2}:\\d{2}")) {
                return specialDate(strDate);
            }
        } catch (ParseException e) {
            Log.v("日期转换失败", strDate + "  " + e.getMessage());
        }

        return null;

    }

    // Fri Jun 23 11:16:08 CST 2023
    private static Date cstDate(String cstDateStr) throws ParseException {
        DateFormat gmt = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
        return gmt.parse(cstDateStr);
    }

    // 周五 6月 23 11:16:08 +08:00 2023
    // 周日 8月 13 17:48:07 +08:00 2023
    private static Date chinaDate(String chinaDateStr) throws ParseException {
        DateFormat gmt = new SimpleDateFormat("EEE MMM dd HH:mm:ss XXX yyyy", Locale.CHINA);
        return gmt.parse(chinaDateStr);
    }

    // "yyyy:MM:dd HH:mm:ss"
    private static Date specialDate(String str) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        return sdf.parse(str);
    }

}
