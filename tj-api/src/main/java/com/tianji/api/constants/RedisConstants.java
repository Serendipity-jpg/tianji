package com.tianji.api.constants;

/**
 * @author: hong.jian
 * @date 2024-03-23 15:38
 */
public class RedisConstants {
    // 给业务点赞的用户集合的KEY前缀，后面需要拼接业务id
    public static final String LIKE_BIZ_KEY_PREFIX = "likes:set:biz:";

    // 给业务点赞统计的KEY前缀，后面需要拼接业务类型
    public static final String LIKE_COUNT_KEY_PREFIX = "likes:times:type:";

}
