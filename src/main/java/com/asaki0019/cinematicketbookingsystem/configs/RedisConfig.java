package com.asaki0019.cinematicketbookingsystem.configs;

import com.asaki0019.cinematicketbookingsystem.utils.RedisCacheUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password}")
    private String password;

    @Value("${spring.data.redis.timeout}")
    private int timeout;

    @Value("${spring.data.redis.jedis.pool.max-active}")
    private int maxTotal;

    @Value("${spring.data.redis.jedis.pool.max-idle}")
    private int maxIdle;

    @Value("${spring.data.redis.jedis.pool.min-idle}")
    private int minIdle;

    @PostConstruct
    public void init() {
        RedisCacheUtils.initSingle(host, port, password, timeout, maxTotal, maxIdle, minIdle);
    }
}