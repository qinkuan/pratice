package com.myself.practice.limitingalgorithm;

public class LeakyBucket implements LimitingAlgorithm {

    // 桶的容量
    private final int capacity;

    // 单位时间漏出速率
    private final int limit;

    // 时间单位 ms
    private final int limitUnit;

    // 剩余水量
    private long leftWater;

    // 上次注入时间
    private long refreshTime = System.currentTimeMillis();

    public LeakyBucket(int capacity, int limit, int limitUnit) {
        this.capacity = capacity;
        this.limit = limit;
        this.limitUnit = limitUnit;
    }

    @Override
    public synchronized boolean tryAcquire() {

        // 计算剩余水量
        long now = System.currentTimeMillis();

        long timeGapUnit = (now - refreshTime) / limitUnit;

        leftWater = Math.max(0, leftWater - timeGapUnit * limit);

        refreshTime = now;

        if (leftWater < capacity) {
            leftWater++;
            return true;
        }

        return false;
    }
}
