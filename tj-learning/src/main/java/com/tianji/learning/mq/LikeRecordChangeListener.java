package com.tianji.learning.mq;

import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: hong.jian
 * @date 2024-03-08 17:37
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LikeRecordChangeListener {

    private final IInteractionReplyService replyService;


    /***
     * 接收消息队列的点赞数量变更通知：
     rabbitMqHelper.send(
     MqConstants.Exchange.LIKE_RECORD_EXCHANGE,  // 消息队列交换机
     StrUtil.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, dto.getBizType()),  // 消息队列Key，使用了字符串格式化
     likedTimesDTO
     * @param dtoList 接受的参数类型为List<LikedTimesDTO>
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "qa.liked.times.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.Exchange.LIKE_RECORD_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.QA_LIKED_TIMES_KEY))
    public void onMsg(List<LikedTimesDTO> dtoList) {
        log.info("LikeRecordChangeListener监听到消息，消息内容：{}", dtoList);
        // 封装到list以执行批量更新
        List<InteractionReply> replyList = new ArrayList<>();
        for (LikedTimesDTO dto : dtoList) {
            InteractionReply reply = new InteractionReply();
            reply.setId(dto.getBizId());    // 业务id
            reply.setLikedTimes(dto.getLikedTimes());   // 点赞数量
            replyList.add(reply);
        }
        // 批量更新
        replyService.updateBatchById(replyList);
    }

}
