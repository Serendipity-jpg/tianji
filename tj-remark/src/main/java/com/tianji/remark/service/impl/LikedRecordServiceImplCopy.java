// package com.tianji.remark.service.impl;
//
// import cn.hutool.core.bean.BeanUtil;
// import cn.hutool.core.collection.CollUtil;
// import cn.hutool.core.util.StrUtil;
// import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
// import com.tianji.api.dto.remark.LikedTimesDTO;
// import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
// import com.tianji.common.constants.MqConstants;
// import com.tianji.common.utils.UserContext;
// import com.tianji.remark.domain.dto.LikeRecordFormDTO;
// import com.tianji.remark.domain.po.LikedRecord;
// import com.tianji.remark.mapper.LikedRecordMapper;
// import com.tianji.remark.service.ILikedRecordService;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
//
// import java.util.Collections;
// import java.util.List;
// import java.util.Set;
// import java.util.stream.Collectors;
//
// /**
//  * <p>
//  * 点赞记录表 服务实现类
//  * </p>
//  *
//  * @author Sakura
//  */
// // @Service
// @RequiredArgsConstructor
// @Slf4j
// public class LikedRecordServiceImplCopy extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {
//
//
//     private final RabbitMqHelper rabbitMqHelper;
//
//     /**
//      * 点赞或者取消赞
//      */
//     @Override
//     public void addLikeRecrod(LikeRecordFormDTO dto) {
//         // 获取登录用户
//         Long userId = UserContext.getUser();
//         // 判断是否点赞、取消赞是否成功，根据liked属性判断是点赞还是取消赞
//         boolean flag = dto.getLiked() ? liked(dto, userId) : unliked(dto, userId);
//         if (!flag) { // 如果点赞或者取消赞失败
//             return;
//         }
//         // 统计该业务id的总点赞数
//         Integer likedTimes = this.lambdaQuery().eq(LikedRecord::getBizId, dto.getBizId()).count();
//         LikedTimesDTO likedTimesDTO = LikedTimesDTO.builder()
//                 .bizId(dto.getBizId())
//                 .likedTimes(likedTimes)
//                 .build();
//         log.info("发送点赞消息，消息内容：{}", likedTimesDTO);
//         // 发送消息到RabbitMQ消息队列
//         rabbitMqHelper.send(
//                 MqConstants.Exchange.LIKE_RECORD_EXCHANGE,  // 消息队列交换机
//                 StrUtil.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, dto.getBizType()),  // 消息队列Key，使用了字符串格式化
//                 likedTimesDTO
//         );
//     }
//
//     /**
//      * 取消赞
//      *
//      * @param dto    表单信息
//      * @param userId 用户id
//      * @return 取消是否成功
//      */
//     private boolean unliked(LikeRecordFormDTO dto, Long userId) {
//         // 根据userId和bizId查询点赞记录是否已存在
//         LikedRecord likedRecord = this.lambdaQuery().eq(LikedRecord::getUserId, userId)
//                 .eq(LikedRecord::getBizId, dto.getBizId())
//                 .one();
//         if (likedRecord == null) { // 没点过赞
//             return false;
//         }
//         // 删除点赞记录
//         return this.removeById(likedRecord.getId());
//     }
//
//     /**
//      * 点赞
//      *
//      * @param dto    表单信息
//      * @param userId 用户id
//      * @return 点赞是否成功
//      */
//     private boolean liked(LikeRecordFormDTO dto, Long userId) {
//         // 根据userId和bizId查询点赞记录是否已存在
//         LikedRecord likedRecord = this.lambdaQuery().eq(LikedRecord::getUserId, userId)
//                 .eq(LikedRecord::getBizId, dto.getBizId())
//                 .one();
//         if (likedRecord != null) { // 点过赞
//             return false;
//         }
//         // 保存点赞记录
//         LikedRecord newRecord = BeanUtil.toBean(dto, LikedRecord.class);
//         newRecord.setUserId(userId);
//         return this.save(newRecord);
//     }
//
//     /**
//      * 批量查询点赞状态
//      * 使用set返回可以自动去重
//      *
//      * @param bizIds 业务id列表
//      */
//     @Override
//     public Set<Long> getLikesStatusByBizIds(List<Long> bizIds) {
//         // 传入的业务id列表为空，直接返回空集
//         if (CollUtil.isEmpty(bizIds)) {
//             return Collections.emptySet();
//         }
//         // 获取当前登录用户
//         Long userId = UserContext.getUser();
//         // 查询数据库，根据userId和bizId进行匹配
//         List<LikedRecord> recordList = this.lambdaQuery().eq(LikedRecord::getUserId, userId)
//                 .in(LikedRecord::getBizId, bizIds)
//                 .list();
//         // stream返回业务id列表
//         return recordList.stream().map(LikedRecord::getBizId).collect(Collectors.toSet());
//     }
// }
