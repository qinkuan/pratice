package com.myself.practice.limitingalgorithm;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Test {
    public static void main(String[] args) {

        // FixWindow fixWindow = new FixWindow(1000, 1000);
        SlideWindow limitingAlgorithm = new SlideWindow(1000, 1000);
        mock(limitingAlgorithm);
    }

    private static void mock(LimitingAlgorithm limitingAlgorithm) {
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            new Thread(() -> {
                while (true) {
                    limitingAlgorithm.tryAcquire();
                    try {
                        Thread.sleep(random.nextInt(100));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}
