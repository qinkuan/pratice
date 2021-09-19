package com.myself.practice.distributedlock.zookeeper;

import org.apache.zookeeper.ZooKeeper;

import java.util.concurrent.CountDownLatch;

public class ZookeeperUtil {


    private static volatile ZooKeeper zkCli = null;
    private static final String connectString = "127.0.0.1:2181";
    private static final int sessionTimeout = 30000;

    /**
     * 初始化
     * @return
     */
    public static ZooKeeper getClient() {
        if (zkCli == null) {
            synchronized (ZooKeeper.class) {
                if (zkCli == null) {
                    CountDownLatch countDownLatch = new CountDownLatch(1);
                    try {
                        zkCli = new ZooKeeper(connectString, sessionTimeout, event -> {
                            if (zkCli.getState().isConnected()) {
                                countDownLatch.countDown();
                            }
                        });
                        countDownLatch.await();
                    } catch (Exception e) {
                        System.out.println("create zookeeper client fail");
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            }
        }
        return zkCli;
    }
}
