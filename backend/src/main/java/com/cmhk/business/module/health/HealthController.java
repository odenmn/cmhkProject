package com.cmhk.business.module.health;

import com.cmhk.business.common.ApiResponse;
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

    private final StringRedisTemplate redisTemplate;

    public HealthController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("service", "cmhk-business-backend");
        status.put("status", "UP");
        status.put("time", LocalDateTime.now());
        status.put("redis", checkRedis());
        return ApiResponse.success(status);
    }

    private String checkRedis() {
        try {
            redisTemplate.opsForValue().set("cmhk:health", "ok");
            return "UP";
        } catch (Exception ex) {
            return "DOWN";
        }
    }
}

