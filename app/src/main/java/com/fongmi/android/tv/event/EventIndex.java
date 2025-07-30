package com.fongmi.android.tv.event;

import org.greenrobot.eventbus.meta.SimpleSubscriberInfo;
import org.greenrobot.eventbus.meta.SubscriberInfo;
import org.greenrobot.eventbus.meta.SubscriberInfoIndex;

import java.util.HashMap;
import java.util.Map;

/**
 * 这是一个自动生成的索引类，用于EventBus的索引查找
 * 通常由EventBus注解处理器自动生成
 * 在这里手动创建以解决编译错误
 */
public class EventIndex implements SubscriberInfoIndex {
    
    private static final Map<Class<?>, SubscriberInfo> SUBSCRIBER_INDEX = new HashMap<>();
    
    @Override
    public SubscriberInfo getSubscriberInfo(Class<?> subscriberClass) {
        return SUBSCRIBER_INDEX.get(subscriberClass);
    }
} 