package com.djt.registrator;

import com.djt.beans.AppUseDatas;
import com.djt.beans.UserBehaviorLog;
import com.djt.key.UserHourPackageKey;
import com.esotericsoftware.kryo.Kryo;
import org.apache.spark.serializer.KryoRegistrator;

/**
 * 自定义kryo序列化
 */
public class MyKryoRegistrator implements KryoRegistrator {
    //  让以下类使用kryo序列化机制
    @Override
    public void registerClasses(Kryo kryo) {
        kryo.register(UserBehaviorLog.class);
        kryo.register(AppUseDatas.class);
        kryo.register(UserHourPackageKey.class);
    }
}
