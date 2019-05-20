/**
 * 文件名称:          		LockingException.java
 * 版权所有@ 2019-2020 	无锡爱超信息技术有限公司
 * 编译器:           		JDK1.8
 */

package com.ljj.dlock;

/**
 * 分布锁异常
 * 
 * Version 1.0.0
 * 
 * @author liangjinjing
 * 
 * Date 2019-05-07 16:08
 * 
 */
public class LockingException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = -2421888979885050925L;

    /**
     * 
     * @param msg
     * @param e
     */
    public LockingException(String msg, Exception e) {
        super(msg, e);
    }

    /**
     * 
     * @param msg
     */
    public LockingException(String msg) {
        super(msg);
    }
}
