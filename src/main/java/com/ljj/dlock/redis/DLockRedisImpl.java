/**
 * 文件名称:          		LockingException.java
 * 版权所有@ 2019-2020 	无锡爱超信息技术有限公司
 * 编译器:           		JDK1.8
 */

package com.ljj.dlock.redis;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import org.apache.zookeeper.KeeperException;

import com.ljj.dlock.DLockInfo;
import com.ljj.dlock.IDLock;

import redis.clients.jedis.Jedis;

/**
 * 采用Redis实现分布式锁
 * 
 * Version 1.0.0
 * 
 * @author liangjinjing
 * 
 * Date 2019-05-07 16:08
 * 
 */
public class DLockRedisImpl implements IDLock {

    /*
     * 获取锁成功
     */
    private static final String LOCK_SUCCESS = "OK";
    /*
     * 解锁成功
     */
    private static final Long RELEASE_SUCCESS = 1L;

    /*
     * setnx命令
     */
    private static final String SET_IF_NOT_EXIST = "NX";
    /*
     * 过期时间
     */
    private static final String SET_WITH_EXPIRE_TIME = "PX";
    /*
     * 连接超时时间，单位毫秒
     */
    private static final int DEFAULT_TIMEOUT = 5000;
    /*
     * 默认有效时间50位
     */
    private static final int DEFAULT_EXPIRE_TIME = 50000;
    /*
     * Redis客户端，在Init的时候初始化
     */
    private Jedis jedis;
    /*
     * 锁名称
     */
    private String lockName = null;
    /*
     * 
     */
    private String host;
    /*
     * 
     */
    private String password;
    
    private int port;

    /**
     * 
     * @param host
     * @param lockName
     */
    public DLockRedisImpl(String host, int port, String password, String lockName) {
        assert host != null && host.length() > 0;
        assert lockName != null && lockName.length() > 0;
        this.host = host;
        this.port = port;
        this.password = password;
        this.lockName = lockName;
        
        
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        if (jedis == null) {
            jedis = new Jedis(host, port, DEFAULT_TIMEOUT);
            jedis.auth(password);
        }
    }

    @ Override
    public DLockInfo lock() {
        return tryLock(DEFAULT_EXPIRE_TIME, DEFAULT_EXPIRE_TIME);
    }

    @ Override
    public DLockInfo tryLock() {
        return tryLock(0, DEFAULT_EXPIRE_TIME);
    }

    @ Override
    public DLockInfo tryLock(int timeout, int expireTime) {
        DLockInfo lock = createDLockInfo(lockName);
        long st = System.currentTimeMillis();
        do {
            /*
             * 参数1 Key 参数2 value 参数3 setnx 参数4 设置过期 参数5 过期时间
             */
            String result = jedis.set(lock.getLockName(), lock.getLockValue(), SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME,
                expireTime);
            if (LOCK_SUCCESS.equals(result)) {
                return lock;
            } else {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                }
            }
        }
        while (st + timeout > System.currentTimeMillis());
        return null;
    }

    @ Override
    public boolean unLock(DLockInfo lock) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Object result = jedis.eval(script, Collections.singletonList(lock.getLockName()),
            Collections.singletonList(lock.getLockValue()));
        return RELEASE_SUCCESS.equals(result);
    }

    /**
     * 
     * @param competitorNode
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    private DLockInfo createDLockInfo(String lockName) {
        DLockInfo lock = new DLockInfo();
        lock.setLockName(lockName);
        lock.setLockValue(UUID.randomUUID().toString());
        lock.setCreateTime(LocalDateTime.now());
        return lock;
    }
}
