package com.myself.practice.distributedlock.redis;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;


@Getter
@Component
public class RedisLock {

    /* redis操作类 */
    private final RedisTemplate<String, String> redisTemplate;

    /* 最大延迟次数 需要配置可自己修改代码 */
    public long maxExtendTimes = 5;

    /* 看门狗守护线程 */
    private final Thread daemonTread;

    /* 延时队列 */
    DelayQueue<DelayTask> queue = new DelayQueue<>();
    @Autowired
    public RedisLock(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        daemonTread = new Thread(() -> watchDog(queue), "delay-watchdog");
        daemonTread.setDaemon(true);
        daemonTread.start();
    }


    /**
     * 实现看门狗的逻辑
     *
     * @param queue
     */
    public void watchDog(DelayQueue<DelayTask> queue) {
        while (true) {
            // 不独单自旋，从延时队列里取出延时任务并执行
            try {
                DelayTask delayTask = queue.take();
                String methodName = delayTask.getMethodName();
                String threadId = delayTask.getLockId();
                long times = delayTask.getTimes();
                long expire = delayTask.getExpire();
                if (times < this.maxExtendTimes) {
                    // 如果未达到延长次数的，自动延长时间
                    boolean success = extendExpire(methodName, threadId, expire);
                    if (success) {
                        // 延时成功，重新加入延时队列
                        queue.add(new DelayTask(methodName, threadId, expire / 2 + 1, times + 1));
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取锁的过程
     *
     * @param methodName
     * @param lockId
     * @param expire
     * @return
     */
    public boolean lock(String methodName, String lockId, long expire) {
        if (expire <= 1) throw new RuntimeException("过期时间应大于1毫秒");
        while (true) {
            // 没有获取到锁就自旋，应该还有挂起通知的方式，暂且不研究
            boolean lock = tryLock(methodName, lockId, expire);
            if (lock) {
                // 获取锁成功，新建延时任务
                queue.add(new RedisLock.DelayTask(methodName, lockId, expire / 2 + 1));
                break;
            }
            try {
                // 线程暂停50毫秒，避免请求redis太频繁
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * 与redis通信
     *
     * @param methodName
     * @param lockId
     * @param timeout
     * @return
     */
    public boolean tryLock(String methodName, String lockId, long timeout) {
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(methodName, lockId, timeout, TimeUnit.MILLISECONDS);
        if (flag == null)
            return false;
        return flag;
    }

    // 解除锁的脚本
    public static final String UN_LOCK_SCRIPT = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
            "then\n" +
            "    return redis.call(\"del\",KEYS[1])\n" +
            "else\n" +
            "    return 0\n" +
            "end\n";

    /**
     * 释放锁
     *
     * @param methodName
     * @param lockId
     * @return
     */
    public boolean unlock(String methodName, String lockId) {
        DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>(UN_LOCK_SCRIPT, Boolean.class);
        Boolean result = redisTemplate.execute(redisScript, Collections.singletonList(methodName), lockId);
        if (result == null) {
            return false;
        }
        return result;
    }


    // 延长锁时间lua脚本，pexpire 表示过期时间毫秒
    public static final String EXTEND_EXPIRE_TIME_SCRIPT = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
            "then\n" +
            "    return redis.call(\"pexpire\",KEYS[1],ARGV[2])\n" +
            "else\n" +
            "    return 0\n" +
            "end\n";

    // 延长锁的时间
    public boolean extendExpire(String methodName, String lockId, long expire) {
        DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>(EXTEND_EXPIRE_TIME_SCRIPT, Boolean.class);
        Boolean result = this.redisTemplate.execute(redisScript, Collections.singletonList(methodName), lockId, String.valueOf(expire));
        if (result == null) {
            return false;
        }
        return result;
    }


    @Getter
    public static class DelayTask implements Delayed {
        // 方法名称
        private String methodName;
        // 线程名称
        private String lockId;
        // 单位为毫秒
        private long expire;
        // 单位为毫秒
        private long exeTime;
        // 当前重试次数
        private long times;

        public DelayTask(String methodName, String lockId, long expire, long times) {
            this(methodName, lockId, expire);
            this.times = times;
        }

        public DelayTask(String methodName, String lockId, long expire) {
            this.methodName = methodName;
            this.lockId = lockId;
            this.expire = expire;
            this.exeTime = System.currentTimeMillis() + expire;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return exeTime - System.currentTimeMillis();
        }

        @Override
        public int compareTo(Delayed o) {
            DelayTask t = (DelayTask) o;
            if (this.exeTime - t.exeTime <= 0) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
