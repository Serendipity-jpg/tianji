package com.tianji;

import com.tianji.promotion.PromotionApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @author: hong.jian
 * @date 2024-04-02 22:09
 */
@SpringBootTest(classes = PromotionApplication.class)
public class RedisLockTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    public void test(){
        // setnx 命令对应setIfAbsent方法
        // key不存在则加锁成功，20s内再次加锁会失败
        Boolean flag = redisTemplate.opsForValue().setIfAbsent("lock", "zhangsan", 20, TimeUnit.SECONDS);
        System.out.println(flag);   // true
        Boolean flag2 = redisTemplate.opsForValue().setIfAbsent("lock", "lisi", 20, TimeUnit.SECONDS);
        System.out.println(flag2);  // false
    }

    @Test
    public void test2(){
        // setxx 命令对应setIfPresent方法
        // key存在则加锁成功，20s内再次加锁会失败
        Boolean flag = redisTemplate.opsForValue().setIfPresent("lock", "zhangsan", 20, TimeUnit.SECONDS);
        System.out.println(flag);   // false
        redisTemplate.opsForValue().set("lock","wangwu");
        Boolean flag2 = redisTemplate.opsForValue().setIfPresent("lock", "lisi", 20, TimeUnit.SECONDS);
        System.out.println(flag2);  // true
    }

}
