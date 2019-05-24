
package com.ljj.dlock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.ljj.dlock.redis.DLockRedisImpl;

import junit.framework.TestCase;

/**
 * Unit test for simple App.
 */
public class DLockRedisTest extends TestCase {
    @Test
    public void testDLockRedis() {
        int count = 3;
        ExecutorService threadPool =  Executors.newFixedThreadPool(count);
        
        CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            threadPool.execute( new Runnable(){
                @ Override
                public void run() {
                    IDLock dlockImpl = new DLockRedisImpl("127.0.0.1", 6379, "ichao_redis", "redislock");
                    DLockInfo lockInfo = dlockImpl.tryLock(5000, 60000); 
                    if (lockInfo != null) {
                        System.out.println("=" + lockInfo + ", delete: " + dlockImpl.unLock(lockInfo));
                    }  
                    latch.countDown();
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
