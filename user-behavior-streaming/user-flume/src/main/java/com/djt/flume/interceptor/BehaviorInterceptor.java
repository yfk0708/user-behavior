package com.djt.flume.interceptor;

import com.djt.beans.UserBehaviorLog;
import com.djt.utils.JSONUtils;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.interceptor.Interceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义拦截器
 * 将相同userId的数据发送到相同kafka分区
 */
public class BehaviorInterceptor implements Interceptor {
    private Logger logger = LoggerFactory.getLogger(BehaviorInterceptor.class);
//    定义一个状态，表示是否按UserId分区(默认为true)
    private boolean isUserPartition = true;

    public BehaviorInterceptor(boolean isUserPartition) {
        this.isUserPartition = isUserPartition;
    }

    @Override
    public void initialize() {
        System.out.println("BehaviorInterceptor is initializing......");
    }

//    处理每条event：将userId设为event的header
//    event包含headers和body，headers通常作为数据的标识，body为数据内容
    @Override
    public Event intercept(Event event) {
        System.out.println("BehaviorInterceptor is intercepting......");
//        过滤无效数据
        if (event == null || event.getBody().length == 0) {
            return null;
        }
        System.out.println("body=" + new String(event.getBody()));
//        如果确定是按userId分区，则将数据转换成UserBehaviorLog，获取userId
        if (isUserPartition) {
            long userId = 0L;
            try {
                UserBehaviorLog userBehaviorLog = JSONUtils.jsonToObject(new String(event.getBody()), UserBehaviorLog.class);
                userId=userBehaviorLog.getUserId();
            } catch (IOException e) {
//                logger打印转换异常数据
                logger.error("event body is invalid,body=" + event.getBody(), e);
            }
//            将userId设为该event的header
            Map<String, String> headers = event.getHeaders();
            if (headers == null) {
                event.setHeaders(new HashMap<>());
            } else {
                event.getHeaders().put("key", String.valueOf(userId));
            }
        }
        return event;
    }

//    处理eventList：循环处理每条event
    @Override
    public List<Event> intercept(List<Event> events) {
        System.out.println("BehaviorInterceptor is intercepting List......");
        ArrayList<Event> eventList = new ArrayList();
        for (Event event : events) {
            Event processedEvent = intercept(event);
            eventList.add(processedEvent);
        }
        return eventList;
    }

    @Override
    public void close() {
        System.out.println("BehaviorInterceptor is closing......");
    }

    public static class BehaviorInterceptorBuilder implements Interceptor.Builder {
        boolean isUserPartition = true;

        @Override
        public Interceptor build() {
            return new BehaviorInterceptor(isUserPartition);
        }

//        获取flume配置文件中是否启用拦截器配置
        @Override
        public void configure(Context context) {
            isUserPartition = context.getBoolean("isUserPartition");
        }
    }
}
