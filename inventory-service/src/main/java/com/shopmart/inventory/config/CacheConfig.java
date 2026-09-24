package com.shopmart.inventory.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Set;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    public static final String PRODUCTS_CACHE = "products";

    @Bean
    public InventoryCacheErrorHandler inventoryCacheErrorHandler() {
        return new InventoryCacheErrorHandler();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return inventoryCacheErrorHandler();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory,
                                          @Value("${spring.cache.redis.time-to-live:10m}") Duration ttl,
                                          @Value("${spring.cache.redis.key-prefix:shopmart:}") String keyPrefix) {
        RedisCacheManager redisCacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(redisCacheConfiguration(ttl, keyPrefix))
                .initialCacheNames(Set.of(PRODUCTS_CACHE))
                .transactionAware()
                .build();
        redisCacheManager.afterPropertiesSet();
        log.info("Đã tạo RedisCacheManager: vùng '{}', TTL {}, tiền tố khóa '{}', transactionAware",
                PRODUCTS_CACHE, ttl, keyPrefix);
        return new WriteGuardedCacheManager(redisCacheManager, inventoryCacheErrorHandler());
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "simple")
    public CacheManager localCacheManager() {
        ConcurrentMapCacheManager inMemory = new ConcurrentMapCacheManager(PRODUCTS_CACHE);
        inMemory.setAllowNullValues(false);
        inMemory.setBeanClassLoader(CacheConfig.class.getClassLoader());
        inMemory.setStoreByValue(true);
        log.info("Đang dùng cache trong bộ nhớ (spring.cache.type=simple), không dùng Redis");
        return new WriteGuardedCacheManager(new TransactionAwareCacheManagerProxy(inMemory),
                inventoryCacheErrorHandler());
    }

    public static RedisCacheConfiguration redisCacheConfiguration(Duration ttl, String keyPrefix) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> keyPrefix + cacheName + "::")
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(redisValueSerializer()));
    }

    public static GenericJackson2JsonRedisSerializer redisValueSerializer() {
        return new GenericJackson2JsonRedisSerializer(redisObjectMapper());
    }

    static ObjectMapper redisObjectMapper() {
        PolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.shopmart.inventory.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.lang.")
                .build();
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .activateDefaultTyping(validator, ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY)
                .build();
    }
}
