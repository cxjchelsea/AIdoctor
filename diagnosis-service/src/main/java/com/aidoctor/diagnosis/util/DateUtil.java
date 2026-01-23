package com.aidoctor.diagnosis.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 日期工具类
 * 提供日期时间相关的工具方法
 */
public class DateUtil {
    
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    /**
     * 格式化日期时间为字符串（默认格式：yyyy-MM-dd HH:mm:ss）
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DEFAULT_FORMATTER);
    }
    
    /**
     * 格式化日期时间为指定格式的字符串
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return dateTime.format(formatter);
    }
    
    /**
     * 格式化日期为字符串（格式：yyyy-MM-dd）
     */
    public static String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATE_FORMATTER);
    }
    
    /**
     * 格式化时间为字符串（格式：HH:mm:ss）
     */
    public static String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(TIME_FORMATTER);
    }
    
    /**
     * 解析字符串为LocalDateTime（默认格式：yyyy-MM-dd HH:mm:ss）
     */
    public static LocalDateTime parse(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DEFAULT_FORMATTER);
    }
    
    /**
     * 解析字符串为LocalDateTime（指定格式）
     */
    public static LocalDateTime parse(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDateTime.parse(dateTimeStr, formatter);
    }
    
    /**
     * 获取当前时间
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
    
    /**
     * 计算两个日期时间之间的天数差
     */
    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start, end);
    }
    
    /**
     * 计算两个日期时间之间的小时差
     */
    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(start, end);
    }
    
    /**
     * 计算两个日期时间之间的分钟差
     */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(start, end);
    }
    
    /**
     * 判断日期时间是否在指定范围内
     */
    public static boolean isBetween(LocalDateTime dateTime, LocalDateTime start, LocalDateTime end) {
        if (dateTime == null || start == null || end == null) {
            return false;
        }
        return !dateTime.isBefore(start) && !dateTime.isAfter(end);
    }
    
    /**
     * 判断日期时间是否早于指定时间
     */
    public static boolean isBefore(LocalDateTime dateTime, LocalDateTime compareTo) {
        if (dateTime == null || compareTo == null) {
            return false;
        }
        return dateTime.isBefore(compareTo);
    }
    
    /**
     * 判断日期时间是否晚于指定时间
     */
    public static boolean isAfter(LocalDateTime dateTime, LocalDateTime compareTo) {
        if (dateTime == null || compareTo == null) {
            return false;
        }
        return dateTime.isAfter(compareTo);
    }
}

