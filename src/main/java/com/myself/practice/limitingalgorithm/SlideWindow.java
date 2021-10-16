package com.myself.practice.limitingalgorithm;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class SlideWindow implements LimitingAlgorithm {


    // 滑动窗口的时间宽度，单位为ms
    private long windowSize;
    // 单个窗口时间内次数
    private long limit;

    private final TreeMap<Long, Integer> counters;

    public SlideWindow(long windowSize, long limit) {
        this.windowSize = windowSize;
        this.limit = limit;
        this.counters = new TreeMap<>();
    }

    @Override
    public synchronized boolean tryAcquire() {

        long now = System.currentTimeMillis();

        int currentWindowCount = getCurrentWindowCount(now);

        System.out.println(currentWindowCount);

        if (currentWindowCount >= limit) {
            return false;
        }
        // 计数器+1
        counters.merge(now, 1, Integer::sum);

        return true;
    }

    /**
     * 获取窗口中的所有请求数，并删除所有无效的子窗口计数器
     *
     * @return
     */
    private int getCurrentWindowCount(long currentWindowTime) {

        long windowStartTime = currentWindowTime - windowSize;
        int result = 0;

        // 遍历当前存储的计数器，删除无效的子窗口计数器，并累加当前窗口中的所有计数器之和
        Iterator<Map.Entry<Long, Integer>> iterator = counters.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Long, Integer> entry = iterator.next();
            if (entry.getKey() < windowStartTime) {
                iterator.remove();
            } else {
                result += entry.getValue();
            }
        }
        return result;

    }
}
