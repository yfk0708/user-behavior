package com.djt.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 通过IO流加载文件中的内容
 */
public class MyProperties {
    public static Properties getProperties(String file) throws IOException {
        Properties properties = null;
        InputStream inputStream = null;

        try {
            inputStream = new BufferedInputStream(MyProperties.class.getClassLoader().getResourceAsStream(file));
            properties = new Properties();
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            throw e;
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }
}
