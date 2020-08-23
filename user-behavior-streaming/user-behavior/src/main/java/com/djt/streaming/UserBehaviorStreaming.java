package com.djt.streaming;


import com.djt.beans.AppUseDatas;
import com.djt.beans.AppUseTimeLenAtHours;
import com.djt.beans.UserBehaviorLog;
import com.djt.common.HBaseClient;
import com.djt.key.UserHourPackageKey;
import com.djt.monitor.ThreadMointor;
import com.djt.service.BehaviorStateService;
import com.djt.utils.DateUtils;
import com.djt.utils.JSONUtils;
import com.djt.utils.MyProperties;
import com.djt.utils.StringUtils;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.*;
import scala.Tuple2;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class UserBehaviorStreaming {
    public static Logger logger = Logger.getLogger(UserBehaviorLog.class);

    public static void main(String[] args) throws Exception {
//        定义配置文件名
        String configFile = "configParams.properties";
//        从配置文件读取每条配置信息
        Properties properties = MyProperties.getProperties(configFile);
        printConfigFile(properties);
//        创建StreamingContext
        JavaStreamingContext smc = createJavaStreamingContext(properties);
        smc.start();
//        创建streaming状态监控线程，根据状态判断是否优雅停止spark streaming
        Thread thread = new Thread(new ThreadMointor(smc, properties));
        thread.start();
        smc.awaitTermination();
    }

    /**
     * 打印配置信息
     */
    private static void printConfigFile(Properties properties) {
//        将读取到的全部配置信息转换成entrySet，每条配置信息为一个entry
        Set<Map.Entry<Object, Object>> entries = properties.entrySet();
//        遍历出所有的entry，转成字符串打印
        for (Map.Entry<Object, Object> entry : entries) {
            System.out.println(entry.getKey().toString() + "=" + entry.getValue().toString());
//            通过logger打印
//            logger.info(entry.getKey().toString() + "=" + entry.getValue().toString());
        }
    }

    /**
     * 根据配置信息创建JavaStreamingContext
     */
    private static JavaStreamingContext createJavaStreamingContext(Properties properties) {
//        ######################################## 定义kafka配置 ########################################
//        获得kafka topic
        String topic = properties.getProperty("kafka.topic");
        HashSet<String> topicSet = new HashSet<>();
        topicSet.add(topic);
//        获得消费者组groupId
        String groupId = properties.getProperty("kafka.groupId");
//        获得kafka集群列表
        String kafkaCluster = properties.getProperty("bootstrap.servers");
//        封装kafka参数
        HashMap<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put("bootstrap.servers", kafkaCluster);
        kafkaParams.put("group.id", groupId);
        kafkaParams.put("key.deserializer", StringDeserializer.class);
        kafkaParams.put("value.deserializer", StringDeserializer.class);
        kafkaParams.put("auto.offset.reset", "earliest");
        kafkaParams.put("enable.auto.commit", false);
//        打印kafka参数
        System.out.println(kafkaParams);

//        ######################################## 创建streamingContext、连接kafka ########################################
//        从hbase表中获取每个kafka分区offset值
        Map<TopicPartition, Long> offsetsMap = getOffsetFromHBase(properties, topic, groupId);
        printOffset(offsetsMap);
//        从配置信息中获取设置的streaming处理间隔时间
        int streamingInterval = Integer.parseInt(properties.getProperty("streaming.interval"));
//        创建StreamingContext
        SparkConf sparkConf = new SparkConf().setMaster("local[2]").setAppName("UserBehaviorStreaming");
//        设置使用的序列化机制
        sparkConf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer");
//        启用实现kryo序列化的程序
        sparkConf.set("spark.kryo.registrator", "com.djt.registrator.MyKryoRegistrator");
//        分区限流：设置spark每秒最多消费kafka分区中的10000条数据
        sparkConf.set("spark.streaming.kafka.maxRatePerPartition", "10000");

        JavaStreamingContext smc = new JavaStreamingContext(sparkConf, Durations.seconds(streamingInterval));
//        streamingContext连接kafka
        JavaInputDStream<ConsumerRecord<String, String>> dStream = KafkaUtils.createDirectStream(
                smc,
                LocationStrategies.PreferConsistent(),
//              消费策略：消费topicSet中的数据，使用kafkaParams配置信息，从offsetsMap的位置开始消费
                ConsumerStrategies.Subscribe(topicSet, kafkaParams, offsetsMap));

//        offsetRanges：为每条数据在Kafka中的offset详细信息，包括其topic、partition、fromOffset、untilOffset
//        用来保存每条数据在kafka中的offsetRanges的变量
        AtomicReference<OffsetRange[]> offsetRangeReference = new AtomicReference<>();
//        通过transform算子获取每条数据的offsetRanges，并不做转换操作
        JavaDStream<ConsumerRecord<String, String>> lines = dStream.transform(line -> {
//            获取每行数据的offsetRanges
            OffsetRange[] offsetRanges = ((HasOffsetRanges) line.rdd()).offsetRanges();
            offsetRangeReference.set(offsetRanges);
//            遍历打印每条数据的offset详细信息
            for (OffsetRange offsetRange : offsetRanges) {
                System.out.println("topic=" + offsetRange.topic() +
                        ",partition=" + offsetRange.partition() +
                        ",fromOffset=" + offsetRange.fromOffset() +
                        ",untilOffset" + offsetRange.untilOffset());
            }
            return line;
        });

//        ######################################## ETL数据清洗 ########################################
        JavaDStream<ConsumerRecord<String, String>> validDStream = lines.filter(line -> {
//            1.先过滤空数据
            if (line.value().isEmpty()) {
                return false;
            }
//            将每条数据转换成对象
            UserBehaviorLog userBehaviorLog = JSONUtils.jsonToObject(line.value(), UserBehaviorLog.class);
//            2.再过滤对象中的无效字段和空字段
            if (userBehaviorLog == null ||
                    userBehaviorLog.getUserId() == 0 ||
                    userBehaviorLog.getAppUseDataList() == null ||
                    userBehaviorLog.getAppUseDataList().size() == 0) {
                return false;
            }
            return true;
        });

//      ######################################## 业务统计分析 ########################################
//        transformation
        JavaPairDStream<UserHourPackageKey, Long> pairDStream = validDStream.flatMapToPair(line -> {
            ArrayList<Tuple2<UserHourPackageKey, Long>> list = new ArrayList<>();
            UserBehaviorLog userBehaviorLog = JSONUtils.jsonToObject(line.value(), UserBehaviorLog.class);

//            获取每条数据中的APP使用情况数据
            List<AppUseDatas> appUseDataList = userBehaviorLog.getAppUseDataList();
            for (AppUseDatas appUseData : appUseDataList) {
//                新建一个自定义key
                UserHourPackageKey userHourPackageKey = new UserHourPackageKey();
//                将每条数据中的信息组装到key中
                userHourPackageKey.setUserId(userBehaviorLog.getUserId());
//                将毫秒格式的beginTime转换成yyyyMMddHH字符串格式
                userHourPackageKey.setHour(DateUtils.getDateStringByMillisecond(DateUtils.HOUR_FORMAT, userBehaviorLog.getBeginTime()));
                userHourPackageKey.setPackageName(appUseData.getPackageName());
                long value = appUseData.getActiveTime() / 1000;
                Tuple2<UserHourPackageKey, Long> tuple = new Tuple2<>(userHourPackageKey, value);
                list.add(tuple);
            }
            return list.iterator();
        });
//        将相同userId、所处hour、packageName的activeTime聚合，得到每个用户在每个小时使用每个APP的总时长
        JavaPairDStream<UserHourPackageKey, Long> reducedDStream = pairDStream.reduceByKey((v1, v2) -> (v1 + v2));

//        action：将结果按分区遍历写入HBase表
        reducedDStream.foreachRDD(pairRDD -> pairRDD.foreachPartition(tupleIterator -> {
//            获取HBase客户端实例
            BehaviorStateService instance = BehaviorStateService.getInstance(properties);
//            遍历分区内的结果数据
            while (tupleIterator.hasNext()) {
                Tuple2<UserHourPackageKey, Long> tuple = tupleIterator.next();
                UserHourPackageKey userHourPackageKey = tuple._1;
                AppUseTimeLenAtHours appUseTimeLenAtHours = new AppUseTimeLenAtHours();
//                将userId转换成长度至少为10的字符串
                appUseTimeLenAtHours.setUserId(StringUtils.getFixedLengthStr(String.valueOf(userHourPackageKey.getUserId()), 10));
                appUseTimeLenAtHours.setHour(userHourPackageKey.getHour());
                appUseTimeLenAtHours.setPackageName(userHourPackageKey.getPackageName());
                appUseTimeLenAtHours.setTimeLen(tuple._2);

                instance.addTimeLen(appUseTimeLenAtHours);
            }
            offsetWriteToHBase(properties, offsetRangeReference, topic, groupId);
        }));
        return smc;
    }

    /**
     * 将每条数据的offset信息写入HBase表，用作维护
     */
    private static void offsetWriteToHBase(Properties properties, AtomicReference<OffsetRange[]> offsetRangeReference, String topic, String groupId) {
        String tableName = "topic_offset";
//        HBase连接到offsets表
        Table table = HBaseClient.getInstance(properties).getTable(tableName);
        String rowkey = topic + ":" + groupId;
        for (OffsetRange offsetRange : offsetRangeReference.get()) {
            try {
                Put put = new Put(Bytes.toBytes(rowkey));
                put.addColumn(Bytes.toBytes("offset"), Bytes.toBytes(String.valueOf(offsetRange.partition())), Bytes.toBytes(String.valueOf(offsetRange.untilOffset())));
                table.put(put);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                HBaseClient.closeTable(table);
            }
        }
    }

    /**
     * 从HBase表中获取kafka每个分区数据的消费位置
     */
    private static Map<TopicPartition, Long> getOffsetFromHBase(Properties properties, String topic, String groupId) {
        String tableName = "topic_offset";
//        HBase连接此表
        Table table = HBaseClient.getInstance(properties).getTable(tableName);
//        定义rowkey名
        String rowkey = topic + ":" + groupId;
//        将rowkey转换成hbase表中的rowkey类型
        Get get = new Get(Bytes.toBytes(rowkey));
//        创建一个map，key为TopicPartition,value为offset值
        HashMap<TopicPartition, Long> offsetsMap = new HashMap<>();
        try {
//            获取此rowkey的值（只有一row，记录kafka每个分区的offset值）
            Result result = table.get(get);
//            遍历此row的每个单格数据，获取单格数据的列簇名、分区号、offset值
            for (Cell cell : result.rawCells()) {
                String family = Bytes.toString(CellUtil.cloneFamily(cell));
                int partition = Integer.parseInt(Bytes.toString(CellUtil.cloneQualifier(cell)));
                long offset = Long.parseLong(Bytes.toString(CellUtil.cloneValue(cell)));
//                将topic和分区号封装成一个topicPartition
                TopicPartition topicPartition = new TopicPartition(topic, partition);
                offsetsMap.put(topicPartition, offset);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return offsetsMap;
    }

    /**
     * 打印offsetsMap中的每条entry信息
     */
    private static void printOffset(Map<TopicPartition, Long> offsetsMap) {
        Set<Map.Entry<TopicPartition, Long>> entries = offsetsMap.entrySet();
        for (Map.Entry<TopicPartition, Long> entry : entries) {
            TopicPartition key = entry.getKey();
            Long value = entry.getValue();
            System.out.println("kafka partition=" + key.partition() + ",offset=" + value);
        }
    }
}
