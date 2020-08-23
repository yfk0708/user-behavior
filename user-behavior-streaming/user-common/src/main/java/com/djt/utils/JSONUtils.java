package com.djt.utils;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON与对象的互转换
 */
public class JSONUtils {
  private static final Logger LOG = LoggerFactory.getLogger(JSONUtils.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final ObjectMapper NEWMAPPER = new ObjectMapper();
  private static JSONUtils jsonUtil;

  public static JSONUtils getInstance() {
    synchronized (JSONUtils.class) {
      if (jsonUtil == null) {
        jsonUtil = new JSONUtils();
      }
    }
    return jsonUtil;
  }

  public static String fromObject(Object obj) throws IOException {
    StringWriter stringWriter = new StringWriter();
    MAPPER.writeValue(stringWriter, obj);
    return stringWriter.toString();
  }

  public static String fromListForData(List<?> list) throws IOException {
    StringWriter stringWriter = new StringWriter();
    stringWriter.write("{data:[");
    for (int i = 0; i < list.size(); i++) {
      stringWriter.write(fromObject(list.get(i)));
      if (i != list.size() - 1) {
        stringWriter.write(",");
      }
    }
    stringWriter.write("]}");
    return stringWriter.toString();
  }

  public static List<?> toList(String json) throws IOException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Get json string is:" + json);
    }
    MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    return MAPPER.readValue(json, List.class);
  }

  public static Map<?, ?> toMap(String json) throws IOException {
    MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    return MAPPER.readValue(json, Map.class);
  }

  /**
   * 从Json字符串中得到指定属性值
   * @param jsonStr
   * @param propertyName
   * @return
   */
  public static Object getFromJson(String jsonStr, String propertyName) {
    Map map = new HashMap();
    try {
      map = JSONUtils.getInstance().toMap(jsonStr);
    } catch (Exception e) {
      LOG.error("", e);
    }
    return (Object) map.get(propertyName);
  }

  public static <T> T jsonToObject(String json, Class<T> clazz) throws IOException {
    return NEWMAPPER.readValue(json, clazz);
  }
}
