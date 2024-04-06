package com.tianji.promotion.config;

import com.tianji.promotion.enums.MyLockType;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;



@Component
public class MyLockFactory {

    private final Map<MyLockType, Function<String, RLock>> lockHandlers;

    /**
     * 使用工厂模式，来实现不同的锁类型
     *
     * @param redissonClient Redisson 客户端实例
     */
    public MyLockFactory(RedissonClient redissonClient) {
        // 初始化锁处理器映射表
        this.lockHandlers = new EnumMap<>(MyLockType.class);
        // 添加不同类型的锁处理器到映射表中
        this.lockHandlers.put(MyLockType.RE_ENTRANT_LOCK, redissonClient::getLock);
        this.lockHandlers.put(MyLockType.FAIR_LOCK, redissonClient::getFairLock);
        this.lockHandlers.put(MyLockType.READ_LOCK, name -> redissonClient.getReadWriteLock(name).readLock());
        this.lockHandlers.put(MyLockType.WRITE_LOCK, name -> redissonClient.getReadWriteLock(name).writeLock());
    }

    /**
     * 获取指定类型的锁实例
     *
     * @param lockType 锁类型
     * @param name     锁名称
     * @return 对应类型的锁实例
     */
    public RLock getLock(MyLockType lockType, String name){
        // get获取锁类型的引用，apply调用对应的创建方法
        return lockHandlers.get(lockType).apply(name);
    }
}
