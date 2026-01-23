package com.aidoctor.diagnosis.util;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 验证工具类
 * 提供数据验证相关的工具方法
 */
public class ValidationUtil {
    
    // 常用正则表达式
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^1[3-9]\\d{9}$"
    );
    
    private static final Pattern ID_CARD_PATTERN = Pattern.compile(
        "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$"
    );
    
    /**
     * 判断字符串是否为空（null或空字符串）
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 判断字符串是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * 判断对象是否为空
     */
    public static boolean isNull(Object obj) {
        return obj == null;
    }
    
    /**
     * 判断对象是否不为空
     */
    public static boolean isNotNull(Object obj) {
        return !isNull(obj);
    }
    
    /**
     * 判断集合是否为空
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
    
    /**
     * 判断集合是否不为空
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }
    
    /**
     * 判断Map是否为空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }
    
    /**
     * 判断Map是否不为空
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }
    
    /**
     * 判断数组是否为空
     */
    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }
    
    /**
     * 判断数组是否不为空
     */
    public static boolean isNotEmpty(Object[] array) {
        return !isEmpty(array);
    }
    
    /**
     * 验证邮箱格式
     */
    public static boolean isValidEmail(String email) {
        if (isEmpty(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * 验证手机号格式（中国大陆）
     */
    public static boolean isValidPhone(String phone) {
        if (isEmpty(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }
    
    /**
     * 验证身份证号格式（18位）
     */
    public static boolean isValidIdCard(String idCard) {
        if (isEmpty(idCard)) {
            return false;
        }
        return ID_CARD_PATTERN.matcher(idCard).matches();
    }
    
    /**
     * 验证字符串长度是否在指定范围内
     */
    public static boolean isLengthInRange(String str, int min, int max) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        return length >= min && length <= max;
    }
    
    /**
     * 验证数字是否在指定范围内
     */
    public static boolean isNumberInRange(Number number, Number min, Number max) {
        if (number == null || min == null || max == null) {
            return false;
        }
        double value = number.doubleValue();
        double minValue = min.doubleValue();
        double maxValue = max.doubleValue();
        return value >= minValue && value <= maxValue;
    }
    
    /**
     * 验证整数是否为正数
     */
    public static boolean isPositive(Integer number) {
        return number != null && number > 0;
    }
    
    /**
     * 验证整数是否为非负数
     */
    public static boolean isNonNegative(Integer number) {
        return number != null && number >= 0;
    }
    
    /**
     * 验证字符串是否匹配正则表达式
     */
    public static boolean matches(String str, String regex) {
        if (isEmpty(str) || isEmpty(regex)) {
            return false;
        }
        return Pattern.matches(regex, str);
    }
    
    /**
     * 验证字符串是否包含指定字符
     */
    public static boolean contains(String str, String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        return str.contains(searchStr);
    }
    
    /**
     * 验证字符串是否以指定前缀开头
     */
    public static boolean startsWith(String str, String prefix) {
        if (str == null || prefix == null) {
            return false;
        }
        return str.startsWith(prefix);
    }
    
    /**
     * 验证字符串是否以指定后缀结尾
     */
    public static boolean endsWith(String str, String suffix) {
        if (str == null || suffix == null) {
            return false;
        }
        return str.endsWith(suffix);
    }
}

