package com.tianji.learning.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: hong.jian
 * @date 2024-03-25 16:12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class SignInMessage {
    // 用户id
    private Long userId;

    // 积分数量
    private Integer points;
}
