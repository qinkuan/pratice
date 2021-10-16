package com.myself.practice.limitingalgorithm;

import java.util.Random;

public class FixWindow implements LimitingAlgorithm {

    // 窗口时间，单位为ms
    private long windowSize;
    // 窗口时间内次数
    private long limit;
    // 当前窗口次数
    private long count;
    // 上一窗口的开始时间
    private long lastWindowStartTime = System.currentTimeMillis();

    public FixWindow(long windowSize, long limit) {
        this.windowSize = windowSize;
        this.limit = limit;
    }

    @Override
    public synchronized boolean tryAcquire() {
        System.out.println(count);
        long now = System.currentTimeMillis();
        if (now - lastWindowStartTime < windowSize) {
            // 在窗口期间内
            if (count < limit) {
                count++;
                return true;
            } else {
                return false;
            }

        }
        count = 1;
        lastWindowStartTime = now;
        return true;
    }

    public static void main(String[] args) throws InterruptedException {

    }
}
