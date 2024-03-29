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

    // 点赞的key前缀，后面需要拼接用户id和年月，如sign:uid:111:202403
    public static final String  SIGN_RECORD_KEY_PREFIX = "sign:uid:";

    // 排行榜的的key前缀，后面需要拼接赛季年月，如boards:202403，采用zset，存储user_id-score（总积分）键值对
    public static final String  POINTS_BOARD_KEY_PREFIX = "boards:";
}
