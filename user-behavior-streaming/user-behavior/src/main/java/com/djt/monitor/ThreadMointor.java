package com.djt.monitor;

import com.djt.service.BehaviorStateService;
import org.apache.log4j.Logger;
import org.apache.spark.streaming.StreamingContextState;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import java.util.Properties;

public class ThreadMointor implements Runnable {
    private Logger log = Logger.getLogger(ThreadMointor.class);

    private JavaStreamingContext smc;
    private Properties properties;

    public ThreadMointor(JavaStreamingContext smc, Properties properties) {
        this.smc = smc;
        this.properties = properties;
    }

//    判断streaming运行状态表中的状态是否等于stop
    public boolean isStop(){
        BehaviorStateService instance = BehaviorStateService.getInstance(properties);
        if ("stop".equals(instance.getStatus())){
            return true;
        }
        return false;
    }

    /**
     * 每隔10秒让hbase获取一次spark streaming状态表的状态是否为stop
     * 如果是stop，再判断StreamingContextState是否为ACTIVE
     * 如果是ACTIVE，则优雅关闭spark streaming
     */
    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            StreamingContextState state = smc.getState();
            log.info("the streamingContextState of javaStreamingContext is " + state.toString());
            if (isStop()) {
                log.info("begin to stop streaming...");
                if (state == StreamingContextState.ACTIVE) {
                    smc.stop(true, true);
                } else {
                    log.info("streaming be stopped");
                    break;
                }
            }
        }
    }
}
