package com.myself.practice.distributedlock.zookeeper;

import com.myself.practice.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class ZookeeperLockTest {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    int count = 0;
    String methodName = "addOne";

    @Test
    public void test() throws InterruptedException {
        ThreadPoolExecutor threadPoolExecutor =
                new ThreadPoolExecutor(100, 200, 20, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
        for (int i = 0; i < 500; i++) {
            CompletableFuture.runAsync(() -> addOneWithZookeeperLock(), threadPoolExecutor);
            CompletableFuture.runAsync(() -> addOneWithZookeeperLock(), threadPoolExecutor);
            CompletableFuture.runAsync(() -> addOneWithZookeeperLock(), threadPoolExecutor);
        }
        // 等待30s，等剩余任务跑完
        Thread.sleep(30000);
        System.out.println(count);
    }

    public void addOne() {
        count++;
    }

    public void addOneWithZookeeperLock() {
        long id = Thread.currentThread().getId();
        String lock = ZookeeperLock.lock(methodName, String.valueOf(id));
        count++;
        System.out.println(count);
        ZookeeperLock.unLock(lock);
    }


}
