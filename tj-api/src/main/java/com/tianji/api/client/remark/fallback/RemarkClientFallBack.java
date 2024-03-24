package com.tianji.api.client.remark.fallback;

import com.tianji.api.client.remark.RemarkClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author: hong.jian
 * @date 2024-03-23 15:00
 */
@Slf4j
public class RemarkClientFallBack implements FallbackFactory<RemarkClient> {
    /**
     * 如果remark服务异常导致其他服务调用RemarkClient提供的服务失败，走create降级逻辑
     *
     * @param cause 失败原因
     * @return 降级返回
     */
    @Override
    public RemarkClient create(Throwable cause) {
        log.error("调用RemarkClient远程服务失败，原因：" + cause);
        return bizIds -> Collections.emptySet();
    }
}
