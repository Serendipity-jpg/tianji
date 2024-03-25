package com.tianji.learning.mq;

import cn.hutool.core.collection.CollUtil;
import com.tianji.common.constants.MqConstants;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 消费消息 用于保存积分
 * @author: hong.jian
 * @date 2024-03-08 17:37
 */
@Component
@Slf4j
@RequiredArgsConstructor  // 使用构造器注入，lombok会在编译器生成相应的方法
public class LearningPointListener {

    private final IPointsRecordService pointsRecordService;

    /***
     * 接收签到产生的保存积分的消息
     * @param message 接受的参数类型为SignInMessage
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "signs.points.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.SIGN_IN))
    public void signInListener(SignInMessage message) {
        log.debug("LearningPointListener接收签到消息，用户{}，积分数量{}", message.getUserId(),message.getPoints());
        // 校验
        if (message.getUserId() == null
                || message.getPoints() == null) {
            // 这里是接受MQ消息，中断即可，若抛异常，则会开启重试
            return;
        }
        // 保存积分
        pointsRecordService.addPointRecord(message, PointsRecordType.SIGN);
    }

    /***
     * 接收问答产生的保存积分的消息
     * @param message 接受的参数类型为SignInMessage
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "relies.points.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.WRITE_REPLY))
    public void replyListener(SignInMessage message) {
        log.debug("LearningPointListener接收问答消息，用户{}，积分数量{}", message.getUserId(),message.getPoints());
        // 校验
        if (message.getUserId() == null
                || message.getPoints() == null) {
            // 这里是接受MQ消息，中断即可，若抛异常，则会开启重试
            return;
        }
        // 保存积分
        pointsRecordService.addPointRecord(message,PointsRecordType.QA);
    }

    /***
     * 接收问答产生的保存积分的消息
     * @param message 接受的参数类型为SignInMessage
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "learning.points.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.LEARN_SECTION))
    public void videoListener(SignInMessage message) {
        log.debug("LearningPointListener接收学习消息，用户{}，积分数量{}", message.getUserId(),message.getPoints());
        // 校验
        if (message.getUserId() == null
                || message.getPoints() == null) {
            // 这里是接受MQ消息，中断即可，若抛异常，则会开启重试
            return;
        }
        // 保存积分
        pointsRecordService.addPointRecord(message,PointsRecordType.LEARNING);
    }


}
