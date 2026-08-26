package com.cmhk.business.common.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Redis 通用缓存客户端的降级和版本失效测试。 */
class CacheClientTests {
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;
    private CacheClient cacheClient;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        objectMapper = new ObjectMapper();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheClient = new CacheClient(redisTemplate, objectMapper);
    }

    @Test
    void fallsBackToDatabaseWhenInitialRedisReadFails() {
        when(valueOperations.get("test:key"))
                .thenThrow(new RedisConnectionFailureException("redis unavailable"));
        JavaType stringType = objectMapper.getTypeFactory().constructType(String.class);

        String result = cacheClient.queryWithMutex(
                "test:key",
                stringType,
                () -> "database-value",
                Duration.ofMinutes(5),
                Duration.ofMinutes(1)
        );

        assertEquals("database-value", result);
    }

    @Test
    void buildsCacheKeyWithCurrentNamespaceVersion() {
        when(valueOperations.get("cmhk:admin:test:version")).thenReturn("3");

        String key = cacheClient.versionedKey("cmhk:admin:test:", "list:abc");

        assertEquals("cmhk:admin:test:v3:list:abc", key);
    }

    @Test
    void invalidatesNamespaceImmediatelyOutsideTransaction() {
        cacheClient.invalidateNamespacesAfterCommit("cmhk:admin:test:");

        verify(valueOperations).increment("cmhk:admin:test:version");
    }
}
