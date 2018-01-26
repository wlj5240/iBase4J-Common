/**
 * 
 */
package top.ibase4j.core.support.cache.mybatis;

import java.io.Serializable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;

import top.ibase4j.core.support.cache.RedisHelper;
import top.ibase4j.core.util.CacheUtil;
import top.ibase4j.core.util.SerializeUtil;

/**
 * 二级缓存
 * @author ShenHuaJie
 * @version 2018年1月26日 上午11:34:48
 */
public class MybatisRedisCache extends PerpetualCache {
    private final Logger logger = LogManager.getLogger();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    public MybatisRedisCache(final String id) {
        super(id);
        if (id == null) {
            throw new IllegalArgumentException("Cache instances require an ID");
        }
        logger.info("Redis Cache id " + id);
    }

    @Override
    public void putObject(Object key, Object value) {
        if (value != null) {
            super.putObject(key, value);
            RedisTemplate<Serializable, Serializable> redisTemplate = ((RedisHelper)CacheUtil.getCache())
                .getRedisTemplate();
            RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
            RedisConnection conn = null;
            try {
                conn = RedisConnectionUtils.getConnection(factory);
                conn.set(SerializeUtil.serialize(key), SerializeUtil.serialize(value), Expiration.seconds(60 * 60),
                    SetOption.ifPresent());
            } finally {
                RedisConnectionUtils.releaseConnection(conn, factory);
            }
        }
    }

    @Override
    public Object getObject(Object key) {
        if (key == null) {
            return null;
        }
        Object result = super.getObject(key);
        if (result != null) {
            return result;
        }
        RedisTemplate<Serializable, Serializable> redisTemplate = ((RedisHelper)CacheUtil.getCache())
            .getRedisTemplate();
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        RedisConnection conn = null;
        try {
            conn = RedisConnectionUtils.getConnection(factory);
            byte[] cashKey = SerializeUtil.serialize(key);
            byte[] value = conn.get(cashKey);
            if (value == null) {
                return value;
            }
            conn.expire(cashKey, 60 * 60 * 12);
            return SerializeUtil.deserialize(value);
        } finally {
            RedisConnectionUtils.releaseConnection(conn, factory);
        }
    }

    @Override
    public int getSize() {
        RedisTemplate<Serializable, Serializable> redisTemplate = ((RedisHelper)CacheUtil.getCache())
            .getRedisTemplate();
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        RedisConnection conn = null;
        try {
            conn = RedisConnectionUtils.getConnection(factory);
            return conn.dbSize().intValue();
        } finally {
            RedisConnectionUtils.releaseConnection(conn, factory);
        }
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return this.readWriteLock;
    }
}
