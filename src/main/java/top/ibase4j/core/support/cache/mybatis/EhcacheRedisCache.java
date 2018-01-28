/**
 * 
 */
package top.ibase4j.core.support.cache.mybatis;

import java.io.Serializable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mybatis.caches.ehcache.AbstractEhcacheCache;

import top.ibase4j.core.Constants;
import top.ibase4j.core.util.CacheUtil;
import top.ibase4j.core.util.SecurityUtil;

/**
 * 二级缓存
 * 
 * @author ShenHuaJie
 * @version 2018年1月26日 上午11:34:48
 */
public class EhcacheRedisCache extends AbstractEhcacheCache {
	private final Logger logger = LogManager.getLogger();

	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

	public EhcacheRedisCache(final String id) {
		super(id);
		if (!(CACHE_MANAGER.cacheExists(id))) {
			CACHE_MANAGER.addCache(id);
		}
		this.cache = CACHE_MANAGER.getEhcache(id);
		logger.info("Redis Cache id " + id);
	}

	@Override
	public void putObject(Object key, Object value) {
		if (value != null) {
			super.putObject(key, value);
			CacheUtil.getLockManager().set(getKey(key), (Serializable) value, 60 * 60);
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
		Object value = CacheUtil.getLockManager().get(getKey(key), 60 * 60);
		super.putObject(key, value);
		return value;
	}

	private String getKey(Object key) {
		return Constants.MYBATIS_CACHE + SecurityUtil.encryptPassword(id + key.hashCode());
	}

	@Override
	public ReadWriteLock getReadWriteLock() {
		return this.readWriteLock;
	}
}
