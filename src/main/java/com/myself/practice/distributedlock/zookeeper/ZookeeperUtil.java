package com.myself.practice.distributedlock.zookeeper;

import org.apache.zookeeper.ZooKeeper;

import java.util.concurrent.CountDownLatch;

public class ZookeeperUtil {

    private static volatile ZooKeeper zkCli = null;
    /* zookeeper的地址和端口 */
    private static final String connectString = "127.0.0.1:2181";
    /* 会话超时时间 */
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
                                // 监听连接zookeeper状态
                                countDownLatch.countDown();
                            }
                        });
                        // 因为连接需要时间，代码不能直接往下走
                        countDownLatch.await();
                    } catch (Exception e) {
                        System.out.println("create zookeeper client fail！！！！！！！！！！！！！！！！！！！");
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            }
        }
        return zkCli;
    }
}
