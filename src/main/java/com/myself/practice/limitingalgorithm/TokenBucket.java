package com.myself.practice.limitingalgorithm;

public class TokenBucket implements LimitingAlgorithm {

    /**
     * 令牌桶的容量「限流器允许的最大突发流量」
     */
    private final long capacity;
    /**
     * 单位令牌发放速率
     */
    private final long limit;

    /**
     * 单位时间
     */
    private final long limitUnit;

    /**
     * 最后一个令牌发放的时间
     */
    long lastTokenTime = System.currentTimeMillis();
    /**
     * 当前令牌数量
     */
    private long currentTokens;


    public TokenBucket(long capacity, long limit, long limitUnit) {
        this.capacity = capacity;
        this.limit = limit;
        this.limitUnit = limitUnit;
    }

    @Override
    public boolean tryAcquire() {

        long now = System.currentTimeMillis();
        if (now - lastTokenTime >= limitUnit) {
            long newPermits = (now - lastTokenTime) / limitUnit * limit;
            currentTokens = Math.min(capacity, currentTokens + newPermits);
            lastTokenTime = now;
        }
        if (currentTokens > 0) {
            currentTokens--;
            return true;
        }
        return false;
    }
}
