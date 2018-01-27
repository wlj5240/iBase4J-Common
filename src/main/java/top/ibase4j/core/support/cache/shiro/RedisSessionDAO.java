package top.ibase4j.core.support.cache.shiro;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;

import top.ibase4j.core.Constants;
import top.ibase4j.core.util.InstanceUtil;
import top.ibase4j.core.util.PropertiesUtil;
import top.ibase4j.core.util.SerializeUtil;

/**
 * 
 * @author ShenHuaJie
 * @version 2017年12月24日 下午8:53:39
 */
public class RedisSessionDAO extends AbstractSessionDAO {
	private static final int EXPIRE_TIME = 600;
	@Autowired
	private RedisTemplate<Serializable, Serializable> redisTemplate;

	public void delete(Serializable sessionId) {
		if (sessionId != null) {
			byte[] sessionKey = buildRedisSessionKey(sessionId);
			RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
			RedisConnection conn = null;
			try {
				conn = RedisConnectionUtils.getConnection(factory);
				conn.del(sessionKey);
			} finally {
				RedisConnectionUtils.releaseConnection(conn, factory);
			}
		}
	}

	protected Serializable doCreate(Session session) {
		Serializable sessionId = generateSessionId(session);
		assignSessionId(session, sessionId);
		saveSession(session);
		return sessionId;
	}

	protected Session doReadSession(Serializable sessionId) {
		byte[] sessionKey = buildRedisSessionKey(sessionId);
		RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
		RedisConnection conn = null;
		try {
			conn = RedisConnectionUtils.getConnection(factory);
			byte[] value = conn.get(sessionKey);
			if (value == null) {
				return null;
			}
			Session session = SerializeUtil.deserialize(value, SimpleSession.class);
			return session;
		} finally {
			RedisConnectionUtils.releaseConnection(conn, factory);
		}
	}

	public void update(Session session) throws UnknownSessionException {
		saveSession(session);
	}

	public void delete(Session session) {
		if (session != null) {
			Serializable id = session.getId();
			if (id != null) {
				RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
				RedisConnection conn = null;
				try {
					conn = RedisConnectionUtils.getConnection(factory);
					conn.del(buildRedisSessionKey(id));
				} finally {
					RedisConnectionUtils.releaseConnection(conn, factory);
				}
			}
		}
	}

	public Collection<Session> getActiveSessions() {
		List<Session> list = InstanceUtil.newArrayList();
		RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
		RedisConnection conn = null;
		try {
			conn = RedisConnectionUtils.getConnection(factory);
			Set<byte[]> set = conn.keys((Constants.REDIS_SHIRO_SESSION + "*").getBytes());
			for (byte[] key : set) {
				list.add(SerializeUtil.deserialize(conn.get(key), SimpleSession.class));
			}
		} finally {
			RedisConnectionUtils.releaseConnection(conn, factory);
		}
		return list;
	}

	private void saveSession(Session session) {
		if (session == null || session.getId() == null)
			throw new UnknownSessionException("session is empty");
		byte[] sessionKey = buildRedisSessionKey(session.getId());
		int sessionTimeOut = PropertiesUtil.getInt("session.maxInactiveInterval", EXPIRE_TIME);
		byte[] value = SerializeUtil.serialize(session);
		RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
		RedisConnection conn = null;
		try {
			conn = RedisConnectionUtils.getConnection(factory);
			conn.set(sessionKey, value, Expiration.seconds(sessionTimeOut), SetOption.UPSERT);
		} finally {
			RedisConnectionUtils.releaseConnection(conn, factory);
		}
	}

	private byte[] buildRedisSessionKey(Serializable sessionId) {
		return (Constants.REDIS_SHIRO_SESSION + sessionId).getBytes();
	}
}