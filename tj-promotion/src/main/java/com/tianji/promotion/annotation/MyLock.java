package com.tianji.promotion.annotation;

import com.tianji.promotion.enums.MyLockStrategy;
import com.tianji.promotion.enums.MyLockType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME) // 运行时生效
@Target(ElementType.METHOD) // 方法
public @interface MyLock {
    String name();  // 锁名称

    long waitTime() default 1;  // 申请锁的等待时间

    long leaseTime() default -1;    // 持有锁的TTL有效时间

    TimeUnit unit() default TimeUnit.SECONDS;   // 时间单位

    // 锁的类型，默认为可重入锁，由工厂模式根据lockType进行创建
    MyLockType lockType() default MyLockType.RE_ENTRANT_LOCK;

    // 锁的失败策略，默认为重试超时后失败（有限重试，失败后抛出异常），由工厂模式根据lockType进行创建
    MyLockStrategy lockStrategy() default MyLockStrategy.FAIL_AFTER_RETRY_TIMEOUT;

}