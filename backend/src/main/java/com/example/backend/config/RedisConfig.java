package com.example.backend.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.support.ResourceScriptSource;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

/**
 * 配置RedisTemplate Bean，用于Redis操作
 * @param factory Redis连接工厂
 * @return 配置好的RedisTemplate实例
 */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
    // 创建RedisTemplate实例
        RedisTemplate<String, Object> template = new RedisTemplate<>();
    // 设置Redis连接工厂
        template.setConnectionFactory(factory);

    // 创建JSON序列化器
        GenericJackson2JsonRedisSerializer serializer = createJsonSerializer();
    // 创建字符串序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

    // 设置key的序列化器为字符串序列化器
        template.setKeySerializer(stringSerializer);
    // 设置value的序列化器为JSON序列化器
        template.setValueSerializer(serializer);
    // 设置hash key的序列化器为字符串序列化器
        template.setHashKeySerializer(stringSerializer);
    // 设置hash value的序列化器为JSON序列化器
        template.setHashValueSerializer(serializer);
    // 初始化RedisTemplate的配置
        template.afterPropertiesSet();
    // 返回配置完成的RedisTemplate实例
        return template;
    }

/**
 * 创建一个类型感知的JSON序列化器
 * 该方法用于配置并返回一个支持Java时间类型的Jackson Redis序列化器
 *
 * @return 配置好的GenericJackson2JsonRedisSerializer实例
 */
    private GenericJackson2JsonRedisSerializer createTypedJsonSerializer() {
    // 创建ObjectMapper实例，用于JSON序列化和反序列化
        ObjectMapper mapper = new ObjectMapper();
    // 设置所有属性的可见性为ANY，确保所有字段都可以被序列化和反序列化
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    // 启用默认的类型信息嵌入，使用LaissezFaireSubTypeValidator验证器
    // NON_FINAL表示非final类型的字段会包含类型信息
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
    // 注册JavaTimeModule，以支持Java 8时间类型的序列化和反序列化
        mapper.registerModule(new JavaTimeModule());
    // 使用配置好的ObjectMapper创建并返回GenericJackson2JsonRedisSerializer实例
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

/**
 * 创建一个普通的JSON序列化器
 * 该方法用于配置并返回一个用于Redis的JSON序列化器
 *
 * @return 配置好的GenericJackson2JsonRedisSerializer实例
 */
    private GenericJackson2JsonRedisSerializer createPlainJsonSerializer() {
    // 创建ObjectMapper实例，用于处理JSON序列化和反序列化
        ObjectMapper mapper = new ObjectMapper();
    // 设置所有属性的可见性为ANY，使得所有字段都可以被序列化和反序列化
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    // 注册JavaTimeModule，以支持Java 8的时间类型（如LocalDate、LocalDateTime等）的序列化
        mapper.registerModule(new JavaTimeModule());
    // 使用配置好的ObjectMapper创建并返回GenericJackson2JsonRedisSerializer实例
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

/**
 * 创建用于JSON序列化的Redis序列化器
 * 该方法返回一个类型化的JSON序列化器，以确保与RedisTemplate的向后兼容性
 *
 * @return 返回一个类型化的Jackson JSON序列化器实例
 */
    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        // Use typed serializer for backwards-compatible RedisTemplate use
        return createTypedJsonSerializer();
    }

/**
 * 配置Redis缓存管理器，用于管理应用程序中的缓存
 * @param factory Redis连接工厂，用于创建Redis连接
 * @return 配置好的Redis缓存管理器实例
 */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
    // 创建JSON序列化器，用于将对象序列化为JSON格式
        GenericJackson2JsonRedisSerializer jsonSerializer = createPlainJsonSerializer();
    // 创建字符串序列化器，用于处理键的序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

    // 设置默认的缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            // 设置默认缓存过期时间为30分钟
                .entryTtl(Duration.ofMinutes(30))
            // 设置键的序列化方式为字符串序列化
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
            // 设置值的序列化方式为JSON序列化
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
            // 禁止缓存null值
                .disableCachingNullValues();

    // 构建并返回Redis缓存管理器
        return RedisCacheManager.builder(factory)
            // 应用默认缓存配置
                .cacheDefaults(defaultConfig)
            // 为"categories"缓存单独配置，过期时间为1小时
                .withCacheConfiguration("categories",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1))
                                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)))
            // 为"productDetail"缓存单独配置，过期时间为30分钟
                .withCacheConfiguration("productDetail",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(30))
                                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)))
            // 构建并返回缓存管理器实例
                .build();
    }

    // ============ Lua 脚本：将「资格判断 + 数据更新」原子化，防止并发数据错乱 ============

/**
 * 创建并配置一个用于添加收藏操作的Redis脚本Bean
 * 该脚本使用Lua语言编写，用于在Redis中执行收藏添加操作
 *
 * @return 配置好的DefaultRedisScript对象，用于执行favorite_add.lua脚本
 *         该脚本执行后返回Long类型的结果
 *         Spring Data Redis 在接收到 Redis 的 Integer 响应时，默认会将其反序列化为 Java 的 Long 类型。
 */
    @Bean
    public DefaultRedisScript<Long> favoriteAddScript() {
    // 创建DefaultRedisScript对象，用于执行Redis Lua脚本
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    // 设置脚本的来源，从类路径下的lua/favorite_add.lua文件加载
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/favorite_add.lua")));
    // 设置脚本执行后返回结果的类型为Long
        script.setResultType(Long.class);
    // 返回配置完成的Redis脚本对象
        return script;
    }

    @Bean
    public DefaultRedisScript<Long> favoriteRemoveScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/favorite_remove.lua")));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public DefaultRedisScript<Long> viewCountIncrementScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/view_count_increment.lua")));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public DefaultRedisScript<Long> orderStatusTransitionScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/order_status_transition.lua")));
        script.setResultType(Long.class);
        return script;
    }

    // ============ 新增：商品库存并发控制脚本 ============

    @Bean
    public DefaultRedisScript<Long> productReserveScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/product_reserve.lua")));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public DefaultRedisScript<Long> productRollbackScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/product_rollback.lua")));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public DefaultRedisScript<Long> productConfirmScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/product_confirm.lua")));
        script.setResultType(Long.class);
        return script;
    }
}
