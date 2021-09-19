package com.myself.practice.distributedlock.redis;

import com.myself.practice.Application;
import com.myself.practice.distributedlock.redis.RedisLock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class RedisLockTest {

    @Autowired
    private RedisLock redisLock;
    int count = 0;
    String methodName = "addOne";

    @Test
    public void test() throws InterruptedException {
        ThreadPoolExecutor threadPoolExecutor =
                new ThreadPoolExecutor(100, 200, 20, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
        for (int i = 0; i < 500; i++) {
            CompletableFuture.runAsync(() -> addOne(redisLock), threadPoolExecutor);
            CompletableFuture.runAsync(() -> addOne(redisLock), threadPoolExecutor);
            CompletableFuture.runAsync(() -> addOne(redisLock), threadPoolExecutor);
        }
        // 等待50s，等剩余任务跑完
        Thread.sleep(50000);
        System.out.println(count);
    }

    public void addOne() {
        count++;
    }

    public void addOne(RedisLock redisLock) {
        long id = Thread.currentThread().getId();
        redisLock.lock(methodName, String.valueOf(id), 2000);
        count++;
        System.out.println(count);
        redisLock.unlock(methodName, String.valueOf(id));
    }

}
