/**
 * 文件名称:                LockingException.java
 * 版权所有@ 2019-2020  无锡爱超信息技术有限公司
 * 编译器:                 JDK1.8
 */
package cmo.ljj.dlock;

import java.time.LocalDateTime;

/**
 * 分布锁对象
 * 
 * Version 1.0.0
 * 
 * @author liangjinjing
 * 
 * Date 2019-05-07 16:08
 * 
 */
public class DLockInfo {
    
    /*
     * 锁名
     */
    private String lockName;
    /*
     * 锁的值
     *      zookeeper： 锁结点路径
     *      redis: 锁Key对应用值 
     */
    private String lockValue;

    /*
     * 锁的创建时间
     */
    private LocalDateTime createTime;

    public String getLockName() {
        return lockName;
    }

    public void setLockName(String lockName) {
        this.lockName = lockName;
    }


    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getLockValue() {
        return lockValue;
    }

    public void setLockValue(String lockValue) {
        this.lockValue = lockValue;
    }
    
    @ Override
    public String toString() {
        return "DLockInfo [lockName=" + lockName + ", lockValue=" + lockValue + ", createTime=" + createTime + "]";
    }
}
