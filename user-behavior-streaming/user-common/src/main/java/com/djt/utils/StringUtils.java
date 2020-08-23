package com.djt.utils;

public final class StringUtils {
  private StringUtils() {
  }

  public static boolean isEmpty(String str) {
    if (str == null || str.replace(" ", "").equals("")) {
      return true;
    }
    return false;
  }

  /**
   * 将字符串转换成指定长度的字符串，小于指定长度左边补0，大于等于则不变
   */
  public static String getFixedLengthStr(String str, int minLength) {
    str = StringUtils.isEmpty(str) ? "" : str.replace(" ", "");
    if (str.length() < minLength) {
      return generateZeroString(minLength - str.length()) + str;
    }
    return str;
  }

  /**
   * 生成一个指定长度的纯0字符串
   */
  public static String generateZeroString(int length) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < length; i++) {
      sb.append('0');
    }
    return sb.toString();
  }

  /**
   * 字符串转long
   */
  public static long getLongFromString(String str) {
    long n = 0;
    try {
      n = org.apache.commons.lang.StringUtils.isEmpty(str) ? 0 : Long.parseLong(str);
    } catch (Exception e) {
      n = 0;
    }
    return n;
  }

  /**
   * object转long
   */
  public static long getLongFromObject(Object o) {
    if (o == null || !(o instanceof Long)) {
      return 0;
    } else {
      return (Long) o;
    }
  }

  public static void main(String[] args) {
    System.out.println(getFixedLengthStr("123", 10));
  }
}
