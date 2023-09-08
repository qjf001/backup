package com.qjf.backup.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class ByteConvert {

    public static String convertToStr(long totalByte) {
        return totalByte == 0 ? "0" : parseToStr(totalByte);
    }

    private static String parseToStr(long totalByte) {
        long oneMb = 1024 * 1024;
        long oneGb = 1024 * 1024 * 1024;
        // 小于1G  返回  xx.yy M // 大于1G 显示为 xx.yy G
        return new BigDecimal(String.valueOf(totalByte)).divide(new BigDecimal(totalByte < oneGb ? oneMb : oneGb), MathContext.DECIMAL64).setScale(2, RoundingMode.HALF_EVEN) + (totalByte < oneGb ? "M" : "G");
    }

    // 输出 80%
    public static String getSpeed(long totalByte, long succByte) {
        BigDecimal speedBD = new BigDecimal(String.valueOf(succByte))
                .divide(new BigDecimal(String.valueOf(totalByte)), MathContext.DECIMAL64)
                .multiply(new BigDecimal("100"), MathContext.DECIMAL64)
                .setScale(2, RoundingMode.HALF_EVEN);
        return speedBD.doubleValue() + "%"; // 保留两位小数
    }
}
