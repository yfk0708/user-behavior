package com.djt.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;

import java.io.IOException;
import java.util.Properties;

public class HBaseClient {
    private static Log log = LogFactory.getLog(HBaseClient.class);
    private static HBaseClient instance = null;
    private Connection connection;

    /**
     * 初始化HBase客户端实例
     */
    private void initialize(Properties properties) {
        Configuration configuration = HBaseConfiguration.create();
        configuration.set("hbase.zookeeper.quorum", properties.getProperty("hbase.zookeeper.quorum"));
        configuration.set("hbase.zookeeper.property.clientPort", properties.getProperty("hbase.zookeeper.property.clientPort"));
        try {
            connection = ConnectionFactory.createConnection(configuration);
        } catch (IOException e) {
            log.error("create hbase connection error!", e);
        }
    }

    /**
     * 获取HBase客户端实例
     */
    public static HBaseClient getInstance(Properties properties) {
        if (instance == null) {
            synchronized (HBaseClient.class) {
                if (instance == null) {
                    instance = new HBaseClient();
                    instance.initialize(properties);
                }
            }
        }
        if (instance.connection == null || instance.connection.isClosed()) {
            synchronized (HBaseClient.class) {
                if (instance.connection == null || instance.connection.isClosed()) {
                    instance.initialize(properties);
                }
            }
        }
        return instance;
    }

    /**
     * 获取HBase表
     */
    public Table getTable(String tableName) {
        Table table = null;
        try {
            table = connection.getTable(TableName.valueOf(tableName));
        } catch (IOException e) {
            table=null;
            log.error("get hbase table error,tableName=" + tableName, e);
        }
        return table;
    }

    /**
     * 关闭HBase表
     */
    public static void closeTable(Table table) {
        if (table != null) {
            try {
                table.close();
            } catch (Exception e) {
                log.error("close table error,tableName=" + table.getName(), e);
            }
        }
    }

    /**
     * 获取连接
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * 关闭连接
     */
    public void close() {
        if (connection != null && !connection.isClosed()) {
            try {
                connection.close();
            } catch (IOException e) {
                log.error("close hbase connect error", e);
            }
        }
    }
}
