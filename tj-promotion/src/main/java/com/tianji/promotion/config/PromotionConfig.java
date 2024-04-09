package com.tianji.promotion.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
public class PromotionConfig {

    /**
     * ThreadPoolTaskExecutor线程池
     */
    @Bean
    public Executor generateExchangeCodeExecutor(){
        // 自定义ThreadPoolTaskExecutor线程池的核心线程数等参数，方便控制
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 1.核心线程池大小
        executor.setCorePoolSize(20);
        // 2.最大线程池大小
        executor.setMaxPoolSize(50);
        // 3.队列大小
        executor.setQueueCapacity(200);
        // 4.线程名称
        executor.setThreadNamePrefix("exchange-code-handler-");
        // 5.拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }


    /**
     * 计算优惠方案用到的线程池
     */
    @Bean
    public Executor calSolutionExecutor(){
        ThreadPoolTaskExecutor refundExecutor = new ThreadPoolTaskExecutor();
        //配置核心线程数
        refundExecutor.setCorePoolSize(20);
        //配置最大线程数
        refundExecutor.setMaxPoolSize(20);
        //配置队列大小
        refundExecutor.setQueueCapacity(2000);
        //配置线程池中的线程的名称前缀
        refundExecutor.setThreadNamePrefix("cal-solution-handler-");
        // 由调用者线程执行
        refundExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        refundExecutor.initialize();
        return refundExecutor;
    }
}