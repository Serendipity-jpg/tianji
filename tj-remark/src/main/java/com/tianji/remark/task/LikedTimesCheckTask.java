package com.tianji.remark.task;

import com.tianji.remark.service.ILikedRecordService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author: hong.jian
 * @date 2024-03-23 17:14
 */
@Slf4j
@Component
@Data
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "remark")
public class LikedTimesCheckTask {



    private List<String> bizTypes ;

    // ZPOPMIN 删除并返回最多count个有序集合key中最低得分的成员。如未指定，count的默认值为1。
    private static final int MAX_BIZ_SIZE = 30;    // 每次任务取出的业务score标准

    private final ILikedRecordService recordService;


    /**
     * 每20s执行1次，将redis的各业务的点赞数量发送消息到RabbitMQ
     */
    // @Scheduled(fixedDelay = 20000)  // 单位ms，即等价于20s
    // @Scheduled(cron = "0/20 * * * * ?") // 每20s执行一次
    @XxlJob("checkLikedTimes") // 更换xxl-job实现分布式定时任务
    private void checkLikedTimes() {
        for (String bizType : bizTypes) {
            // 从redis中zset读取得分低于MAX_BIZ_SIZE的业务的点赞数量，并发送RabbitMQ消息
            recordService.readLikedTimesAndSendMessage(bizType, MAX_BIZ_SIZE);
        }
    }
}
