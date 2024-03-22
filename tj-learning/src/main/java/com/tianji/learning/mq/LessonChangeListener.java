package com.tianji.learning.mq;

import cn.hutool.core.collection.CollUtil;
import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author: hong.jian
 * @date 2024-03-08 17:37
 */
@Component
@Slf4j
@RequiredArgsConstructor  // 使用构造器注入，lombok会在编译器生成相应的方法
public class LessonChangeListener {

    private final ILearningLessonService lessonService;


    /***
     * MQ消息发送相关代码：
     *         rabbitMqHelper.send(
     *                 MqConstants.Exchange.ORDER_EXCHANGE, // Exchange
     *                 MqConstants.Key.ORDER_PAY_KEY,    // Key
     *                 OrderBasicDTO.builder()
     *                         .orderId(orderId)
     *                         .userId(userId)
     *                         .courseIds(cIds)
     *                         .finishTime(order.getFinishTime())
     *                         .build()
     *         );
     *
     * @param dto 接受的参数类型为OrderBasicDTO
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "learning.lesson.pay.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_PAY_KEY))
    public void onMsg(OrderBasicDTO dto) {
        log.info("LessonChangeListener接收消息，用户{}，添加课程{}", dto.getUserId(), dto.getCourseIds());
        // 校验
        if (dto.getUserId() == null
                || dto.getOrderId() == null
                || CollUtil.isEmpty(dto.getCourseIds())) {
            // 这里是接受MQ消息，中断即可，若抛异常，则会开启重试
            return;
        }
        // 保存课程到课表
        lessonService.addUserLesson(dto.getUserId(),dto.getCourseIds());
    }


    /**
     * 当用户退款成功时，取消相应课程
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "learning.lesson.refund.queue ",durable = "true"),
    exchange = @Exchange(value = MqConstants.Exchange.ORDER_EXCHANGE,type = ExchangeTypes.TOPIC ),
    key = MqConstants.Key.ORDER_REFUND_KEY))
    public void receiveMsg(OrderBasicDTO dto){
        log.info("LessonChangeListener接收消息，用户{}，取消课程{}", dto.getUserId(), dto.getCourseIds());
        // 校验
        if (dto.getUserId() == null
                || dto.getOrderId() == null
                || CollUtil.isEmpty(dto.getCourseIds())) {
            // 这里是接受MQ消息，中断即可，若抛异常，则会开启重试
            return;
        }
        // 从课表中删除课程
        lessonService.deleteLessionById(dto.getUserId(),dto.getCourseIds());
    }
}
