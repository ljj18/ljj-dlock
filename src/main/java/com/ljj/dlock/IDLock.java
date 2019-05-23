/**
 * 文件名称:                DistributedLock.java
 * 版权所有@ 2019-2020  无锡爱超信息技术有限公司
 * 编译器:                 JDK1.8
 */

package com.ljj.dlock;

import java.net.ConnectException;

import org.apache.zookeeper.KeeperException;

/**
 * 分布式锁接口
 * 
 * Version 1.0.0
 * 
 * @author liangjinjing
 * 
 * Date 2019-05-07 16:08
 * 
 */
public interface IDLock {

    /**
     * 尝试获得锁，一直阻塞，直到获得锁为止 (实战环境不能这样用)
     * 

     * @throws LockingException
     * @throws ConnectException
     * 
     * @return 
     *      非空,获得锁,执行业务
     *      空,没获得锁
     */
    DLockInfo lock();
    
    /**
     * 尝试获得锁，能获得就立马获得锁DLockInfo，如果不能获得就立马返回Null
     * 
     *      
     * @throws LockingException
     * @throws ConnectException
     * 
     * @return 
     *      非空,获得锁,执行业务
     *      空,没获得锁
     */
    DLockInfo tryLock();  

    /**
     * 尝试获得锁，如果有锁就返回DLockInfo实例，如果没有锁就等待，如果等待了一段时间后还没能获取到锁，那么就返回Null
     * 
     * @param timeout 等待时间, 毫秒
     * @param expire  过期时间，毫秒(Redis实现才需要老邪)
     * 
     * @throws LockingException
     * @throws ConnectException
     * 
     * @return 
     *      非空,获得锁,执行业务
     *      空,没获得锁
     */
    DLockInfo tryLock(int timeout, int expire);
    
    /**
     * 释放锁
     * 
     * @param lock 锁信息
     * 
     * @throws LockingException
     * @throws KeeperException
     * @throws InterruptedException
     */
    boolean unLock(DLockInfo lock);
 
}
