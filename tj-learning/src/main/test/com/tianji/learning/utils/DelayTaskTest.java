package com.tianji.learning.utils;

import com.tianji.learning.util.DelayTask;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.DelayQueue;

@Slf4j
class DelayTaskTest {
    @Test
    void testDelayQueue() throws InterruptedException {
        // 1.初始化延迟队列
        DelayQueue<DelayTask<String>> queue = new DelayQueue<>();
        // 2.向队列中添加延迟执行的任务
        log.info("开始初始化延迟任务。。。。");
        queue.add(new DelayTask<>("延迟任务3", Duration.ofSeconds(6)));
        queue.add(new DelayTask<>("延迟任务1", Duration.ofSeconds(2)));
        queue.add(new DelayTask<>("延迟任务2", Duration.ofSeconds(4)));
        // 3.尝试执行任务
        while (!queue.isEmpty()) {
            // poll：非阻塞方法，take非阻塞方法
            DelayTask<String> task = queue.take();
            log.info("开始执行延迟任务：{}", task.getData());
        }
    }
}