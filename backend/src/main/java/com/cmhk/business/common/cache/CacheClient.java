package com.cmhk.business.common.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Component
public class CacheClient {

    private static final Logger log = LoggerFactory.getLogger(CacheClient.class);

    private static final String NULL_VALUE = "__CACHE_NULL__";
    private static final String LOCK_PREFIX = "lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final long LOCK_WAIT_MILLIS = 50L;
    private static final int MAX_LOCK_RETRY = 20;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CacheClient(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> T queryWithMutex(
            String key,
            JavaType javaType,
            Supplier<T> dbFallback,
            Duration ttl,
            Duration nullTtl
    ) {
        T cachedValue = readCache(key, javaType);
        if (cachedValue != null || isNullValue(key)) {
            return cachedValue;
        }

        String lockKey = LOCK_PREFIX + key;
        String lockValue = UUID.randomUUID().toString();
        boolean locked = false;

        try {
            locked = tryLock(lockKey, lockValue);
            if (!locked) {
                return retryReadCache(key, javaType, dbFallback);
            }

            cachedValue = readCache(key, javaType);
            if (cachedValue != null || isNullValue(key)) {
                return cachedValue;
            }

            T dbValue = dbFallback.get();
            if (dbValue == null) {
                writeNullValue(key, nullTtl);
                return null;
            }

            writeCache(key, dbValue, ttl);
            return dbValue;
        } catch (Exception ex) {
            log.warn("Redis 缓存处理失败，降级查询数据库，key={}，原因={}", key, ex.getMessage());
            return dbFallback.get();
        } finally {
            if (locked) {
                unlock(lockKey, lockValue);
            }
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception ex) {
            log.warn("删除 Redis 缓存失败，key={}，原因={}", key, ex.getMessage());
        }
    }

    private <T> T retryReadCache(String key, JavaType javaType, Supplier<T> dbFallback) {
        for (int i = 0; i < MAX_LOCK_RETRY; i++) {
            sleep();
            T cachedValue = readCache(key, javaType);
            if (cachedValue != null || isNullValue(key)) {
                return cachedValue;
            }
        }
        log.warn("等待缓存重建超时，降级查询数据库，key={}", key);
        return dbFallback.get();
    }

    private <T> T readCache(String key, JavaType javaType) {
        String json = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        if (NULL_VALUE.equals(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, javaType);
        } catch (JsonProcessingException ex) {
            log.warn("Redis 缓存反序列化失败，删除异常缓存，key={}，原因={}", key, ex.getMessage());
            redisTemplate.delete(key);
            return null;
        }
    }

    private boolean isNullValue(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return NULL_VALUE.equals(value);
    }

    private void writeCache(String key, Object value, Duration ttl) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(value);
        redisTemplate.opsForValue().set(key, json, withJitter(ttl));
    }

    private void writeNullValue(String key, Duration nullTtl) {
        redisTemplate.opsForValue().set(key, NULL_VALUE, nullTtl);
    }

    private boolean tryLock(String lockKey, String lockValue) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, LOCK_TTL);
        return Boolean.TRUE.equals(success);
    }

    private void unlock(String lockKey, String lockValue) {
        try {
            String currentValue = redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(currentValue)) {
                redisTemplate.delete(lockKey);
            }
        } catch (Exception ex) {
            log.warn("释放 Redis 互斥锁失败，lockKey={}，原因={}", lockKey, ex.getMessage());
        }
    }

    private Duration withJitter(Duration ttl) {
        long seconds = ttl.toSeconds();
        if (seconds <= 10) {
            return ttl;
        }
        long jitter = ThreadLocalRandom.current().nextLong(1, Math.max(2, seconds / 10));
        return ttl.plusSeconds(jitter);
    }

    private void sleep() {
        try {
            Thread.sleep(LOCK_WAIT_MILLIS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
