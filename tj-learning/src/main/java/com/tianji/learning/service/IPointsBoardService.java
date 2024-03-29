package com.tianji.learning.service;

import com.tianji.learning.domain.po.PointsBoard;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;

import java.util.List;

/**
 * <p>
 * 学霸天梯榜 服务类
 * </p>
 *
 * @author Sakura
 */
public interface IPointsBoardService extends IService<PointsBoard> {

    /**
     * 查询赛季积分榜-当前赛季和历史赛季共用
     * season为null或0表示当前赛季
     * @param query 查询参数
     * @return  赛季积分列表
     */
    PointsBoardVO queryPointsBoardList(PointsBoardQuery query);

    /**
     * 分页查询当前赛季积分和排名列表-redis
     *
     * @param key redis的zset的key
     * @return 当前用户的当前赛季积分和排名
     */
    List<PointsBoard> queryCurrentBoardRankList(PointsBoardQuery query, String key);
}
