package com.cmhk.business.module.health;

import com.cmhk.business.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final StringRedisTemplate redisTemplate;

    public HealthController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        log.info("开始执行健康检查");
        Map<String, Object> status = new LinkedHashMap<>();
        String redisStatus = checkRedis();
        status.put("service", "cmhk-business-backend");
        status.put("status", "UP");
        status.put("time", LocalDateTime.now());
        status.put("redis", redisStatus);
        log.info("健康检查完成，redis={}", redisStatus);
        return ApiResponse.success(status);
    }

    private String checkRedis() {
        try {
            redisTemplate.opsForValue().set("cmhk:health", "ok");
            return "UP";
        } catch (Exception ex) {
            log.warn("Redis 健康检查失败，原因={}", ex.getMessage());
            return "DOWN";
        }
    }
}
