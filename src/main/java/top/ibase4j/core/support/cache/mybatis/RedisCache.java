/**
 * 
 */
package top.ibase4j.core.support.cache.mybatis;

import java.io.Serializable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ibatis.cache.Cache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import top.ibase4j.core.Constants;
import top.ibase4j.core.util.CacheUtil;
import top.ibase4j.core.util.PropertiesUtil;
import top.ibase4j.core.util.SecurityUtil;

/**
 * 二级缓存
 * 
 * @author ShenHuaJie
 * @version 2018年1月26日 上午11:34:48
 */
public class RedisCache implements Cache {
	private final Logger logger = LogManager.getLogger();
	/**
	 * The cache id (namespace)
	 */
	protected final String id;

	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

	public RedisCache(final String id) {
		if (id == null) {
			throw new IllegalArgumentException("Cache instances require an ID");
		}
		this.id = id;
		logger.info("Redis Cache id " + id);
	}

	@Override
	public void putObject(Object key, Object value) {
		if (value != null) {
			CacheUtil.getCache().set(getKey(key), (Serializable) value,
					PropertiesUtil.getInt("mybatis.cache.expires", 60 * 60));
		}
	}

	@Override
	public Object getObject(Object key) {
		if (key == null) {
			return null;
		}
		return CacheUtil.getCache().get(getKey(key), PropertiesUtil.getInt("mybatis.cache.expires", 60 * 60));
	}

	@Override
	public ReadWriteLock getReadWriteLock() {
		return this.readWriteLock;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public Object removeObject(Object key) {
		Object obj = getObject(key);
		CacheUtil.getCache().del(getKey(key));
		return obj;
	}

	@Override
	public void clear() {
		CacheUtil.getCache().delAll(Constants.MYBATIS_CACHE + id + "*");
	}

	public int getSize() {
		return CacheUtil.getCache().getAll(Constants.MYBATIS_CACHE + id + "*").size();
	}

	public int hashCode() {
		return this.id.hashCode();
	}

	public String toString() {
		return "RedisCache {" + this.id + "}";
	}

	private String getKey(Object key) {
		return Constants.MYBATIS_CACHE + id + ":" + SecurityUtil.encryptMd5(key.toString());
	}
}
