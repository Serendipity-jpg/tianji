package com.tianji.learning.mapper;

import com.tianji.learning.domain.po.PointsBoard;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.learning.domain.vo.PointsBoardVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 学霸天梯榜 Mapper 接口
 * </p>
 *
 * @author Sakura
 */
public interface PointsBoardMapper extends BaseMapper<PointsBoard> {

    /**
     * 查询指定用户指定赛季的积分和排名
     * @param tableName 赛季表名称
     * @param userId    用户id
     * @return  指定用户指定赛季的积分和排名
     */
    PointsBoardVO queryMyHistoryBoardRank(@Param("tableName") String tableName, @Param("userId") Long userId);

    /**
     * 分页查询指定历史赛季积分和排名
     *
     * @param tableName 赛季表
     * @return 指定历史赛季积分和排名
     */
    List<PointsBoard> queryHistoryBoardRankList(@Param("tableName")String tableName,
                                                @Param("pageNo")Integer pageNo,
                                                @Param("pageSize")Integer pageSize);
}
