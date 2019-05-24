
package com.ljj.dlock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.ljj.dlock.DLockInfo;
import com.ljj.dlock.IDLock;
import com.ljj.dlock.zookeeper.DLockZKImpl;

import junit.framework.TestCase;

/**
 * Unit test for simple App.
 */
public class DLockZKTest extends TestCase {
    @Test
    public void testDLockZk() {
        int count = 20;
        ExecutorService threadPool =  Executors.newFixedThreadPool(count);
        IDLock dlockImpl = new DLockZKImpl("127.0.0.7:2181", "zk_lock");
        CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            threadPool.execute( new Runnable(){
                @ Override
                public void run() {
                    DLockInfo lockInfo = dlockImpl.tryLock(5000, 5000); 
                    if (lockInfo != null) {
                        System.out.println("=========================" + lockInfo.getLockValue());
                        dlockImpl.unLock(lockInfo);
                    }                    
                }
            });
        }
        try {
            latch.await(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("latch count:" + latch.getCount());
    }
}