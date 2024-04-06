package com.tianji;

import com.tianji.promotion.PromotionApplication;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @author: hong.jian
 * @date 2024-04-02 22:09
 */
@SpringBootTest(classes = PromotionApplication.class)
public class RedissonTest {

    @Autowired
    private RedissonClient redissonClient;

    @Test
    public void test() throws InterruptedException {
        RLock lock = redissonClient.getLock("lock");// 可重入锁
        try {
            // 三个参数为申请加锁时间，过期时间和过期时间单位
            // 底层采用哈希结构，key为线程唯一标识，value为重入次数
            boolean isLock = lock.tryLock();    // 看门狗机制不能设置失效时间，采用默认的失效时间30s
            // boolean isLock = lock.tryLock(1, 20, TimeUnit.SECONDS);
            if (isLock){
                System.out.println("获取到分布式锁");
            }else{
                System.out.println("没有获取到锁");
            }
            // 线程睡眠60s
            TimeUnit.SECONDS.sleep(60);
        }finally {
            lock.unlock();  // 释放锁
            System.out.println("释放锁...");
        }
    }



}
