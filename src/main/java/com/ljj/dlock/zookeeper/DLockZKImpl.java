/**
 * 文件名称:          		LockingException.java
 * 版权所有@ 2019-2020 	无锡爱超信息技术有限公司
 * 编译器:           		JDK1.8
 */

package com.ljj.dlock.zookeeper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import com.ljj.dlock.DLockInfo;
import com.ljj.dlock.IDLock;
import com.ljj.dlock.LockingException;

/**
 * 采用Zookeeper实现分布式锁
 * 
 * Version 1.0.0
 * 
 * @author liangjinjing
 * 
 * Date 2019-05-07 16:08
 * 
 */
public class DLockZKImpl implements IDLock {
    /*
     * zookeeper节点的默认分隔符
     */
    private final static String SEPARATOR = "/";
    /*
     * 锁在zk中的根节点，所有锁结点都是此结点子结点
     */
    private final static String ROOT_LOCK_PATH = SEPARATOR + "ichaoLock";
    /*
     * 竞争结点名称（有序临时结点）
     */
    private final static String COMPETITOR_NAME = "runtask";
    /*
     * 默认的EPHEMERAL节点的超时时间，单位毫秒
     */
    private static final int DEFAULT_SESSION_TIMEOUT = 5000;
    /*
     * 统一的zooKeeper连接，在Init的时候初始化
     */
    private static ZooKeeper zkClient = null;
    /*
     * 锁名称
     */
    private String lockName = null;
    /*
     * 锁结点路径
     * 
     * 此值组合：ROOT_LOCK_PATH + SEPARATOR + lockName, 如果 lockName="task", 则 lockPath = "/ichaolock/task"
     * 
     */
    private String lockPath = null;
    /*
     * 竞争者结点路径
     * 
     * 此值组合：lockPath + SEPARATOR + COMPETITOR_NAME, 如果 lockName="task", 则 competitorPath =
     * "/ichaolock/task/runtask0000000xxx"
     * 
     */
    private String competitorPath = null;
    /*
     * 当前竞争者结点
     */
    // private String thisCompetitorPath = null;
    /*
     * 等待竞争者结点
     */
    // private String waitCompetitorPath = null;

    /*
     * 
     */
    private String host;

    /**
     * 
     * @param host
     * @param lockName
     */
    public DLockZKImpl(String host, String lockName) {
        assert host != null && host.length() > 0;
        assert lockName != null && lockName.length() > 0;
        this.host = host;
        this.lockName = lockName;
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        try {
            if (zkClient == null) {
                /*
                 * 等待connection
                 */
                CountDownLatch connectionLatch = new CountDownLatch(1);
                zkClient = new ZooKeeper(host, DEFAULT_SESSION_TIMEOUT, new Watcher(){
                    @ Override
                    public void process(WatchedEvent event) {
                        if (event.getState().equals(KeeperState.SyncConnected)) {
                            connectionLatch.countDown();
                        }
                    }
                });
                connectionLatch.await();
                if (connectionLatch.getCount() == 1) {
                    throw new LockingException("Zookeeper connection fail, host: " + host);
                }
                // 锁根结点是否存在
                if (zkClient.exists(ROOT_LOCK_PATH, false) == null) {
                    zkClient.create(ROOT_LOCK_PATH, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
                // 锁结点是否存在
                lockPath = ROOT_LOCK_PATH + SEPARATOR + lockName;
                if (zkClient.exists(lockPath, false) == null) {
                    // 创建相对应的锁节点
                    zkClient.create(lockPath, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
                // 竞争者结点路径
                competitorPath = lockPath + SEPARATOR + COMPETITOR_NAME;
            }
        } catch (IOException | InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    @ Override
    public DLockInfo lock() {
        return tryLock(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @ Override
    public DLockInfo tryLock(int timeout, int expire) {
        try {
            DLockInfo lock = createDLockInfo(competitorPath);
            // 获取竞争者结点列表
            List<String> allCompetitorList = getAllCompetitor(lockPath);
            int index = allCompetitorList.indexOf(lock.getLockValue().substring(lockPath.length() + 1));
            // =-1，自身竞争者结点没有创建
            if (index == -1) {
                throw new LockingException("competitorPath not exit after create");
            } else if (index == 0) { // 自己是最小的结点，获得锁
                return lock;
            }
            else if (index > 0) {// 如果不是最小结点,那就就没法获取锁骨
                CountDownLatch waitLocklatch = new CountDownLatch(1);
                // 监听比自己小1的结点是否删除
                String waitCompetitorPath = lockPath + SEPARATOR + allCompetitorList.get(index - 1);
                Stat waitNodeStat = zkClient.exists(waitCompetitorPath, new Watcher(){
                    @ Override
                    public void process(WatchedEvent event) {
                        if (event.getType().equals(EventType.NodeDeleted)
                            && event.getPath().equals(waitCompetitorPath)) {
                            waitLocklatch.countDown();
                        }
                    }
                });
                // 监听状态不为null，则进入等
                if (waitNodeStat != null) {
                    waitLocklatch.await(timeout, TimeUnit.MILLISECONDS);
                } else {
                    waitLocklatch.countDown();
                }
                // 等待超时也没有获得锁
                if (waitLocklatch.getCount() > 0) {
                    // 删除自己创建的锁
                    unLock(lock);
                    return null;
                }
                return lock;
            }
            
        } catch (KeeperException | InterruptedException e) {
            throw new LockingException(e.getMessage(), e);
        }
        return null;

    }

    @ Override
    public DLockInfo tryLock() {
        try {
            DLockInfo lock = createDLockInfo(competitorPath);
            // 获取竞争者结点列表
            // 获取竞争者结点列表
            List<String> allCompetitorList = getAllCompetitor(lockPath);
            int index = allCompetitorList.indexOf(competitorPath.substring(lockPath.length() + 1));
            return index == 0 ? lock : null;
        } catch (KeeperException | InterruptedException e) {
            throw new LockingException(e.getMessage(), e);
        }
    }

    @ Override
    public boolean unLock(DLockInfo lock) {
        try {
            // TODO 此处确保高可用,如果删除异常,需要重试
            zkClient.delete(lock.getLockValue(), -1);
            return true;
        } catch (InterruptedException e) {
            throw new LockingException("the release lock has been Interrupted ");
        } catch (KeeperException e) {
            throw new LockingException("zookeeper connect error");
        }
    }

    /**
     * 创建竞争者节点
     * 
     * @throws KeeperException
     * @throws InterruptedException / private void createComPrtitorNode() throws KeeperException, InterruptedException {
     * competitorPath = lockPath + SEPARATOR + COMPETITOR_NODE; thisCompetitorPath = zkClient.create(competitorPath,
     * null, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL); }
     */

    /**
     * 获取所有竞争者结点
     * 
     * @param locPath
     * @param competitorPath
     * @return
     */
    private List<String> getAllCompetitor(String lockPath) throws KeeperException, InterruptedException {
        // 获取竞争者结点列表
        List<String> list = zkClient.getChildren(lockPath, false);
        Collections.sort(list);
        return list;
    }

    /**
     * 
     * @param competitorNode
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    private DLockInfo createDLockInfo(String competitorPath) throws KeeperException, InterruptedException {
        String realCompetitorPath = zkClient.create(competitorPath, null, Ids.OPEN_ACL_UNSAFE,
            CreateMode.EPHEMERAL_SEQUENTIAL);
        DLockInfo lock = new DLockInfo();
        lock.setLockValue(realCompetitorPath);
        lock.setLockName(competitorPath);
        lock.setCreateTime(LocalDateTime.now());
        lock.setLockName(lockName);
        return lock;
    }
}
