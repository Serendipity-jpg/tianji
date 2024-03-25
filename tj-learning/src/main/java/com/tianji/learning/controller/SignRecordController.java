package com.tianji.learning.controller;

import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.service.ISignRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 学霸天梯榜 控制器
 * </p>
 *
 * @author Sakura
 */
@Api(tags = "SignRecord管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/sign-records")
public class SignRecordController {

    private final ISignRecordService signRecordService;

    /**
     * 用户签到
     */
    @ApiOperation("用户签到")
    @PostMapping
    public SignResultVO addSignRecords(){
        return signRecordService.addSignRecords();
    }

    /**
     * 查询用户本月签到记录
     */
    @ApiOperation("查询用户本月签到记录")
    @GetMapping
    public List<Byte> selectMonthSignRecords(){
        return signRecordService.selectMonthSignRecords();
    }

}
