package com.tianji.learning.config;

import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

/**
 * @author: hong.jian
 * @date 2024-03-11 11:52
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class LearningLessonTask {

    private final ILearningLessonService lessonService;

    /**
     * 定期检查`learning_lesson`表中的课程是否过期，
     * 如果过期则将课程状态修改为已过期
     * 程序启动时间为每天早上5点
     */
    @Scheduled(cron = "0 0 5 * * ?")
    public void updateFinishedLessons() {
        // 判断当前时间是否已大于等于 `expire_time`过期时间字段，超过则修改 `status`字段为**已过期**
        boolean flag = lessonService.lambdaUpdate()
                .lt(LearningLesson::getExpireTime, LocalDateTime.now())
                .set(LearningLesson::getStatus, LessonStatus.EXPIRED)
                .update();
        if (flag){
            log.info("更新已过期课表状态成功，更新时间：{}",LocalDateTime.now());
        }else {
            log.error("更新已过期课表状态失败，更新时间：{}",LocalDateTime.now());
        }
    }
}
