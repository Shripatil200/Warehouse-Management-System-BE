package com.infotact.warehouse.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.List;

@Configuration
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // 1. Build a dedicated ObjectMapper for Redis serialization
        ObjectMapper redisMapper = new ObjectMapper();

        // 2. Handle Java 8 date/time types (LocalDate, LocalDateTime, etc.)
        redisMapper.registerModule(new JavaTimeModule());
        redisMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 3. CRITICAL FIX: Embed the full class name in the JSON so Redis can
        //    deserialize back to the correct type instead of LinkedHashMap.
        redisMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // 4. Register Mixins so Jackson can reconstruct PageImpl, PageRequest, and Sort from cached JSON
        redisMapper.addMixIn(PageImpl.class, PageImplMixin.class);
        redisMapper.addMixIn(PageRequest.class, PageRequestMixin.class);
        redisMapper.addMixIn(Sort.class, SortMixin.class);
        redisMapper.addMixIn(Sort.Order.class, SortOrderMixin.class);

        // 5. Build the serializer with the configured mapper
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(redisMapper);

        // 6. Cache config: 10-min TTL, skip null values
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer)
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * Mixin to teach Jackson how to reconstruct a Spring Data PageImpl.
     * PageImpl has no default constructor, so @JsonCreator maps the
     * cached JSON fields back to the correct constructor.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class PageImplMixin<T> {
        @JsonCreator
        PageImplMixin(
                @JsonProperty("content") List<T> content,
                @JsonProperty("pageable") Pageable pageable,
                @JsonProperty("totalElements") long totalElements
        ) {}
    }

    /**
     * Mixin to teach Jackson how to reconstruct a Spring Data PageRequest.
     * PageRequest has no default constructor either; we map the page number,
     * page size, and sort info from the cached JSON.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class PageRequestMixin {
        @JsonCreator
        PageRequestMixin(
                @JsonProperty("pageNumber") int pageNumber,
                @JsonProperty("pageSize") int pageSize,
                @JsonProperty("sort") Sort sort
        ) {}
    }

    /**
     * Mixin for Sort — constructed from its list of Order objects.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class SortMixin {
        @JsonCreator
        SortMixin(@JsonProperty("orders") List<Sort.Order> orders) {}
    }

    /**
     * Mixin for Sort.Order — constructed from direction and property name.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class SortOrderMixin {
        @JsonCreator
        SortOrderMixin(
                @JsonProperty("direction") Sort.Direction direction,
                @JsonProperty("property") String property
        ) {}
    }
}