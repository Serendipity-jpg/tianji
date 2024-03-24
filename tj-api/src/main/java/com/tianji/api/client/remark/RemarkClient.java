package com.tianji.api.client.remark;

import com.tianji.api.client.remark.fallback.RemarkClientFallBack;
import com.tianji.api.dto.course.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

@FeignClient(contextId = "remark", value = "remark-service", fallbackFactory = RemarkClientFallBack.class)
public interface RemarkClient {

    /**
     * 批量查询点赞状态
     * 使用set返回可以自动去重
     */
    @GetMapping("/likes/list")
    Set<Long> getLikesStatusByBizIds(@RequestParam("bizIds")List<Long> bizIds);
}