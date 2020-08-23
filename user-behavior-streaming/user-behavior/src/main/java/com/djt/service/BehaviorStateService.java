package com.djt.service;

import com.djt.beans.AppUseTimeLenAtHours;
import com.djt.common.HBaseClient;
import com.djt.utils.DateUtils;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.Properties;

public class BehaviorStateService {
    private Properties properties;
    private static BehaviorStateService instance;

    /**
     * 获取HBase客户端实例
     */
    public static BehaviorStateService getInstance(Properties properties) {
        if (instance == null) {
            synchronized (BehaviorStateService.class) {
                if (instance == null) {
                    instance = new BehaviorStateService();
                    instance.properties = properties;
                }
            }
        }
        return instance;
    }

    /**
     * 统计各种时长指标，并写入对应的HBase表中
     */
    public void addTimeLen(AppUseTimeLenAtHours appUseTimeLenAtHours) {
        addUserBehaviorList(appUseTimeLenAtHours);
        addUserHourTimeLen(appUseTimeLenAtHours);
        addUserDayTimeLen(appUseTimeLenAtHours);
        addUserPackageHourTimeLen(appUseTimeLenAtHours);
        addUserPackageDayTimeLen(appUseTimeLenAtHours);
    }

    /**
     * 将数据写入表
     * 各用户每天使用各APP的时长
     */
    private void addUserBehaviorList(AppUseTimeLenAtHours appUseTimeLenAtHours) {
        String tableName = "behavior_user_app";
        Table table = HBaseClient.getInstance(properties).getTable(tableName);
        String rowkey = appUseTimeLenAtHours.getUserId() + ":" + DateUtils.getDayByHour(appUseTimeLenAtHours.getHour()) + ":" + appUseTimeLenAtHours.getPackageName();
        try {
            table.incrementColumnValue(Bytes.toBytes(rowkey), Bytes.toBytes("timeLen"), Bytes.toBytes(DateUtils.getDayByHour(appUseTimeLenAtHours.getHour())), appUseTimeLenAtHours.getTimeLen());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            HBaseClient.closeTable(table);
        }
    }

    /**
     * 将数据写入表
     * 各用户每天各小时使用所有APP的时长
     */
    private void addUserHourTimeLen(AppUseTimeLenAtHours appUseTimeLenAtHours) {
        String tableName = "behavior_user_day_app_time_" + DateUtils.getMonthByHour(appUseTimeLenAtHours.getHour());
        Table table = HBaseClient.getInstance(properties).getTable(tableName);
        String rowkey = appUseTimeLenAtHours.getUserId() + ":" + appUseTimeLenAtHours.getPackageName();
        try {
            table.incrementColumnValue(Bytes.toBytes(rowkey), Bytes.toBytes("timeLen"), Bytes.toBytes(DateUtils.getOnlyDayByHour(appUseTimeLenAtHours.getHour())), appUseTimeLenAtHours.getTimeLen());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            HBaseClient.closeTable(table);
        }
    }

    /**
     * 将数据写入表
     * 各用户每月各天使用所有APP的时长
     */
    private void addUserDayTimeLen(AppUseTimeLenAtHours appUseTimeLenAtHours) {
        String tableName = "behavior_user_day_time_" + DateUtils.getMonthByHour(appUseTimeLenAtHours.getHour());
        Table table = HBaseClient.getInstance(properties).getTable(tableName);
        String rowKey = String.valueOf(appUseTimeLenAtHours.getUserId());
        try {
            table.incrementColumnValue(Bytes.toBytes(rowKey), Bytes.toBytes("timeLen"), Bytes.toBytes(DateUtils.getOnlyDayByHour(appUseTimeLenAtHours.getHour())), appUseTimeLenAtHours.getTimeLen());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            HBaseClient.closeTable(table);
        }
    }

    /**
     * 将数据写入表
     * 各用户每天在各小时使用各APP的时长
     */
    private void addUserPackageHourTimeLen(AppUseTimeLenAtHours appUseTimeLenAtHours) {
        String tableName = "behavior_user_hour_app_time";
        Table table = HBaseClient.getInstance(properties).getTable(tableName);
        String rowKey = appUseTimeLenAtHours.getUserId() + ":" + DateUtils.getDayByHour(appUseTimeLenAtHours.getHour()) + ":" + appUseTimeLenAtHours.getPackageName();

        try {
            table.incrementColumnValue(Bytes.toBytes(rowKey), Bytes.toBytes("timeLen"), Bytes.toBytes(DateUtils.getOnlyHourByHour(appUseTimeLenAtHours.getHour())), appUseTimeLenAtHours.getTimeLen());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            HBaseClient.closeTable(table);
        }
    }

    /**
     * 将数据写入表
     * 各用户每个APP在每天使用的时长
     */
    private void addUserPackageDayTimeLen(AppUseTimeLenAtHours appUseTimeLenAtHours) {
        String tableName = "behavior_user_day_app_time_" + DateUtils.getMonthByHour(appUseTimeLenAtHours.getHour());
        Table table = HBaseClient.getInstance(properties).getTable(tableName);
        String rowKey = appUseTimeLenAtHours.getUserId() + ":" + appUseTimeLenAtHours.getPackageName();

        try {
            table.incrementColumnValue(Bytes.toBytes(rowKey), Bytes.toBytes("timeLen"), Bytes.toBytes(DateUtils.getOnlyDayByHour(appUseTimeLenAtHours.getHour())), appUseTimeLenAtHours.getTimeLen());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            HBaseClient.closeTable(table);
        }
    }

    /**
     * 获取Spark Streaming运行状态表中的状态
     */
    public String getStatus() {
        String tableName = "user_behavior_status";
        Table table = HBaseClient.getInstance(properties).getTable(tableName);
        String rowKey = "status";

        Get get = new Get(Bytes.toBytes(rowKey)).addColumn(Bytes.toBytes("status"), Bytes.toBytes("status"));

        try {
            Result result = table.get(get);
            return Bytes.toString(CellUtil.cloneValue(result.rawCells()[0]));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            HBaseClient.closeTable(table);
        }
    }
}
