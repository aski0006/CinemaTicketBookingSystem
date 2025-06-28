package com.asaki0019.cinematicketbookingsystem.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.exceptions.JedisException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;

/**
 * Redis 操作工具类，支持多种部署模式，涵盖常用数据结构操作、事务、管道、发布订阅、异常处理与日志。
 */
@Component
public class RedisCacheUtils {
    private static final Logger logger = LoggerFactory.getLogger(RedisCacheUtils.class);
    private static JedisPool jedisPool;
    private static JedisSentinelPool sentinelPool;
    private static JedisCluster jedisCluster;
    private static boolean isCluster = false;
    private static boolean isSentinel = false;

    // ================== 基础连接与配置 ==================
    /**
     * 初始化单机模式连接池
     * 
     * @param host     主机地址
     * @param port     端口
     * @param password 密码
     * @param timeout  连接超时时间（毫秒）
     * @param maxTotal 最大连接数
     * @param maxIdle  最大空闲连接数
     * @param minIdle  最小空闲连接数
     */
    public static void initSingle(String host, int port, String password, int timeout, int maxTotal, int maxIdle,
            int minIdle) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        if (password != null && !password.isEmpty()) {
            jedisPool = new JedisPool(config, host, port, timeout, password);
        } else {
            jedisPool = new JedisPool(config, host, port, timeout);
        }
        isCluster = false;
        isSentinel = false;
        logger.info("[Redis] 单机模式连接池初始化完成: {}:{}", host, port);
    }

    /**
     * 初始化哨兵模式连接池
     * 
     * @param masterName 主节点名称
     * @param sentinels  哨兵节点列表
     * @param password   密码
     * @param timeout    连接超时时间（毫秒）
     * @param maxTotal   最大连接数
     * @param maxIdle    最大空闲连接数
     * @param minIdle    最小空闲连接数
     */
    public static void initSentinel(String masterName, Set<String> sentinels, String password, int timeout,
            int maxTotal, int maxIdle, int minIdle) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        if (password != null && !password.isEmpty()) {
            sentinelPool = new JedisSentinelPool(masterName, sentinels, config, timeout, password);
        } else {
            sentinelPool = new JedisSentinelPool(masterName, sentinels, config, timeout);
        }
        isCluster = false;
        isSentinel = true;
        logger.info("[Redis] 哨兵模式连接池初始化完成: {} {}", masterName, sentinels);
    }

    /**
     * 获取Jedis实例
     * 
     * @return Jedis实例
     */
    private static Jedis getJedis() {
        if (isCluster)
            throw new UnsupportedOperationException("集群模式请使用JedisCluster");
        if (isSentinel)
            return sentinelPool.getResource();
        return jedisPool.getResource();
    }

    // ================== 键值对操作 ==================
    /**
     * 设置键值对
     * 
     * @param key           键
     * @param value         值
     * @param expireSeconds 过期时间（秒）
     * @return 操作结果
     */
    public static String set(String key, String value, long expireSeconds) {
        try {
            if (isCluster) {
                if (expireSeconds > 0) {
                    String res = jedisCluster.set(key, value, SetParams.setParams().ex(expireSeconds));
                    logger.debug("[Redis] set(cluster) key={}, expire={}s", key, expireSeconds);
                    return res;
                } else {
                    String res = jedisCluster.set(key, value);
                    logger.debug("[Redis] set(cluster) key={}, no expire", key);
                    return res;
                }
            } else {
                try (Jedis jedis = getJedis()) {
                    if (expireSeconds > 0) {
                        String res = jedis.set(key, value, SetParams.setParams().ex(expireSeconds));
                        logger.debug("[Redis] set key={}, expire={}s", key, expireSeconds);
                        return res;
                    } else {
                        String res = jedis.set(key, value);
                        logger.debug("[Redis] set key={}, no expire", key);
                        return res;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] set error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 设置键值对
     * 
     * @param key   键
     * @param value 值
     * @return 操作结果
     */
    public static String set(String key, String value) {
        return set(key, value, 0);
    }

    /**
     * 获取键值对
     * 
     * @param key 键
     * @return 值
     */
    public static String get(String key) {
        try {
            if (isCluster) {
                return jedisCluster.get(key);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.get(key);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] get error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 删除键值对
     * 
     * @param keys 键
     * @return 操作结果
     */
    public static Long del(String... keys) {
        try {
            if (isCluster) {
                return jedisCluster.del(keys);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.del(keys);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] del error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 判断键是否存在
     * 
     * @param key 键
     * @return 是否存在
     */
    public static boolean exists(String key) {
        try {
            if (isCluster) {
                return jedisCluster.exists(key);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.exists(key);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] exists error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 获取键的过期时间
     * 
     * @param key 键
     * @return 过期时间（秒）
     */
    public static Long ttl(String key) {
        try {
            if (isCluster) {
                return jedisCluster.ttl(key);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.ttl(key);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] ttl error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 设置键的过期时间
     * 
     * @param key     键
     * @param seconds 过期时间（秒）
     * @return 操作结果
     */
    public static Long expire(String key, long seconds) {
        try {
            if (isCluster) {
                return jedisCluster.expire(key, (int) seconds);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.expire(key, (int) seconds);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] expire error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 设置多个键值对
     * 
     * @param keysvalues 键值对
     * @return 操作结果
     */
    public static String mset(String... keysvalues) {
        try {
            if (isCluster) {
                return jedisCluster.mset(keysvalues);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.mset(keysvalues);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] mset error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 获取多个键值对
     * 
     * @param keys 键
     * @return 值
     */
    public static List<String> mget(String... keys) {
        try {
            if (isCluster) {
                return jedisCluster.mget(keys);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.mget(keys);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] mget error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    // ================== 哈希表操作 ==================
    /**
     * 设置哈希表键值对
     * 
     * @param key   键
     * @param field 字段
     * @param value 值
     * @return 操作结果
     */
    public static Long hset(String key, String field, String value) {
        try {
            if (isCluster) {
                return jedisCluster.hset(key, field, value);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.hset(key, field, value);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] hset error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 设置多个哈希表键值对
     * 
     * @param key  键
     * @param hash 哈希表
     * @return 操作结果
     */
    public static String hmset(String key, Map<String, String> hash) {
        try {
            if (isCluster) {
                return jedisCluster.hmset(key, hash);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.hmset(key, hash);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] hmset error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 获取哈希表键值对
     * 
     * @param key   键
     * @param field 字段
     * @return 值
     */
    public static String hget(String key, String field) {
        try {
            if (isCluster) {
                return jedisCluster.hget(key, field);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.hget(key, field);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] hget error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 获取多个哈希表键值对
     * 
     * @param key    键
     * @param fields 字段
     * @return 值
     */
    public static List<String> hmget(String key, String... fields) {
        try {
            if (isCluster) {
                return jedisCluster.hmget(key, fields);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.hmget(key, fields);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] hmget error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 删除哈希表键值对
     * 
     * @param key    键
     * @param fields 字段
     * @return 操作结果
     */
    public static Long hdel(String key, String... fields) {
        try {
            if (isCluster) {
                return jedisCluster.hdel(key, fields);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.hdel(key, fields);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] hdel error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 获取哈希表所有键值对
     * 
     * @param key 键
     * @return 哈希表
     */
    public static Map<String, String> hgetAll(String key) {
        try {
            if (isCluster) {
                return jedisCluster.hgetAll(key);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.hgetAll(key);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] hgetAll error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 获取哈希表所有键
     * 
     * @param key 键
     * @return 哈希表
     */
    public static Set<String> hkeys(String key) {
        try {
            if (isCluster) {
                return jedisCluster.hkeys(key);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.hkeys(key);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] hkeys error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 获取哈希表长度
     * 
     * @param key 键
     * @return 长度
     */
    public static Long hlen(String key) {
        try {
            if (isCluster) {
                return jedisCluster.hlen(key);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.hlen(key);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] hlen error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    // ================== 列表操作 ==================
    /**
     * 从列表左侧插入元素
     * 
     * @param key    键
     * @param values 值
     * @return 操作结果
     */
    public static Long lpush(String key, String... values) {
        try {
            if (isCluster) {
                return jedisCluster.lpush(key, values);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.lpush(key, values);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] lpush error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 从列表右侧插入元素
     * 
     * @param key    键
     * @param values 值
     * @return 操作结果
     */
    public static Long rpush(String key, String... values) {
        try {
            if (isCluster) {
                return jedisCluster.rpush(key, values);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.rpush(key, values);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] rpush error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 从列表左侧弹出元素
     * 
     * @param key 键
     * @return 值
     */
    public static String lpop(String key) {
        try {
            if (isCluster) {
                return jedisCluster.lpop(key);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.lpop(key);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] lpop error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 从列表右侧弹出元素
     * 
     * @param key 键
     * @return 值
     */
    public static String rpop(String key) {
        try {
            if (isCluster) {
                return jedisCluster.rpop(key);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.rpop(key);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] rpop error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 获取列表元素
     * 
     * @param key   键
     * @param start 开始索引
     * @param end   结束索引
     * @return 列表
     */
    public static List<String> lrange(String key, long start, long end) {
        try {
            if (isCluster) {
                return jedisCluster.lrange(key, start, end);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.lrange(key, start, end);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] lrange error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 获取列表长度
     * 
     * @param key 键
     * @return 长度
     */
    public static Long llen(String key) {
        try {
            if (isCluster) {
                return jedisCluster.llen(key);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.llen(key);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] llen error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    // ================== 集合操作 ==================
    /**
     * 添加元素到集合
     * 
     * @param key     键
     * @param members 元素
     * @return 操作结果
     */
    public static Long sadd(String key, String... members) {
        try {
            if (isCluster) {
                return jedisCluster.sadd(key, members);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.sadd(key, members);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] sadd error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 从集合中删除元素
     * 
     * @param key     键
     * @param members 元素
     * @return 操作结果
     */
    public static Long srem(String key, String... members) {
        try {
            if (isCluster) {
                return jedisCluster.srem(key, members);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.srem(key, members);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] srem error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 判断元素是否在集合中
     * 
     * @param key    键
     * @param member 元素
     * @return 是否存在
     */
    public static boolean sismember(String key, String member) {
        try {
            if (isCluster) {
                return jedisCluster.sismember(key, member);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.sismember(key, member);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] sismember error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 获取集合所有元素
     * 
     * @param key 键
     * @return 集合
     */
    public static Set<String> smembers(String key) {
        try {
            if (isCluster) {
                return jedisCluster.smembers(key);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.smembers(key);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] smembers error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 获取集合长度
     * 
     * @param key 键
     * @return 长度
     */
    public static Long scard(String key) {
        try {
            if (isCluster) {
                return jedisCluster.scard(key);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.scard(key);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] scard error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 获取集合交集
     * 
     * @param keys 键
     * @return 集合
     */
    public static Set<String> sinter(String... keys) {
        try {
            if (isCluster) {
                return jedisCluster.sinter(keys);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.sinter(keys);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] sinter error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 获取集合并集
     * 
     * @param keys 键
     * @return 集合
     */
    public static Set<String> sunion(String... keys) {
        try {
            if (isCluster) {
                return jedisCluster.sunion(keys);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.sunion(keys);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] sunion error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 获取集合差集
     * 
     * @param keys 键
     * @return 集合
     */
    public static Set<String> sdiff(String... keys) {
        try {
            if (isCluster) {
                return jedisCluster.sdiff(keys);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.sdiff(keys);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] sdiff error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    // ================== 有序集合操作 ==================
    /**
     * 添加元素到有序集合
     * 
     * @param key    键
     * @param score  分数
     * @param member 元素
     * @return 操作结果
     */
    public static Long zadd(String key, double score, String member) {
        try {
            if (isCluster) {
                return jedisCluster.zadd(key, score, member);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.zadd(key, score, member);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] zadd error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 从有序集合中删除元素
     * 
     * @param key     键
     * @param members 元素
     * @return 操作结果
     */
    public static Long zrem(String key, String... members) {
        try {
            if (isCluster) {
                return jedisCluster.zrem(key, members);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.zrem(key, members);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] zrem error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 获取有序集合元素
     * 
     * @param key   键
     * @param start 开始索引
     * @param end   结束索引
     * @return 有序集合
     */
    public static List<String> zrange(String key, long start, long end) {
        try {
            try (Jedis jedis = getJedis()) {
                return jedis.zrange(key, start, end);
            }
        } catch (Exception e) {
            logger.error("[Redis] zrange error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 获取有序集合元素（逆序）
     *
     * @param key   键
     * @param start 开始索引
     * @param end   结束索引
     * @return 有序集合
     */
    public static List<String> zrevrange(String key, long start, long end) {
        try {
            if (isCluster) {
                return jedisCluster.zrevrange(key, start, end);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.zrevrange(key, start, end);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] zrevrange error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 获取有序集合元素分数
     * 
     * @param key    键
     * @param member 元素
     * @return 分数
     */
    public static Double zscore(String key, String member) {
        try {
            if (isCluster) {
                return jedisCluster.zscore(key, member);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.zscore(key, member);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] zscore error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 增加有序集合元素分数
     * 
     * @param key       键
     * @param increment 增加分数
     * @param member    元素
     * @return 操作结果
     */
    public static Double zincrby(String key, double increment, String member) {
        try {
            if (isCluster) {
                return jedisCluster.zincrby(key, increment, member);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.zincrby(key, increment, member);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] zincrby error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 获取有序集合长度
     * 
     * @param key 键
     * @return 长度
     */
    public static Long zcard(String key) {
        try {
            if (isCluster) {
                return jedisCluster.zcard(key);
            } else {
                try (Jedis jedis = getJedis()) {
                    return jedis.zcard(key);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] zcard error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    // ================== 事务与管道操作 ==================
    /**
     * 事务操作
     * 
     * @param consumer 消费者
     */
    public static void transaction(Consumer<Transaction> consumer) {
        if (isCluster)
            throw new UnsupportedOperationException("集群模式暂不支持事务");
        try (Jedis jedis = getJedis()) {
            Transaction tx = jedis.multi();
            try {
                consumer.accept(tx);
                tx.exec();
            } catch (Exception e) {
                tx.discard();
                logger.error("[Redis] transaction error: {}", e.getMessage(), e);
                throw new JedisException(e);
            }
        }
    }

    /**
     * 管道操作
     * 
     * @param consumer 消费者
     * @return 结果
     */
    public static List<Object> pipeline(Consumer<Pipeline> consumer) {
        if (isCluster)
            throw new UnsupportedOperationException("集群模式暂不支持管道");
        try (Jedis jedis = getJedis()) {
            Pipeline pipeline = jedis.pipelined();
            try {
                consumer.accept(pipeline);
                return pipeline.syncAndReturnAll();
            } catch (Exception e) {
                logger.error("[Redis] pipeline error: {}", e.getMessage(), e);
                throw new JedisException(e);
            }
        }
    }

    // ================== 发布订阅 ==================
    /**
     * 发布消息
     * 
     * @param channel 频道
     * @param message 消息
     */
    public static void publish(String channel, String message) {
        try {
            if (isCluster) {
                jedisCluster.publish(channel, message);
            } else {
                try (Jedis jedis = getJedis()) {
                    jedis.publish(channel, message);
                }
            }
        } catch (Exception e) {
            logger.error("[Redis] publish error: {}", e.getMessage(), e);
            throw new JedisException(e);
        }
    }

    /**
     * 订阅消息
     * 
     * @param pubSub   发布订阅
     * @param channels 频道
     */
    public static void subscribe(JedisPubSub pubSub, String... channels) {
        new Thread(() -> {
            try {
                if (isCluster) {
                    jedisCluster.subscribe(pubSub, channels);
                } else {
                    try (Jedis jedis = getJedis()) {
                        jedis.subscribe(pubSub, channels);
                    }
                }
            } catch (Exception e) {
                logger.error("[Redis] subscribe error: {}", e.getMessage(), e);
            }
        }).start();
    }
}