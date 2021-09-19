package com.myself.practice.distributedlock.zookeeper;

import lombok.SneakyThrows;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class ZookeeperLock {


    private static final ConcurrentHashMap<String, String> concurrentHashMap = new ConcurrentHashMap<>();

    @SneakyThrows
    public static void unLock(String zNode) {
        Stat stat = ZookeeperUtil.getClient().exists(zNode, true);
        ZookeeperUtil.getClient().delete(zNode, stat.getVersion());
    }

    @SneakyThrows
    public static String lock(String methodName, String lockId) {

        String path = getPath(methodName);
        // 注册一个节点
        String zNode = ZookeeperUtil.getClient().create(path + "/node", lockId.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        while (true) {
            // 获取所有节点
            List<String> children = ZookeeperUtil.getClient().getChildren(path, true);
            int index = children.indexOf(zNode.substring(zNode.lastIndexOf("/") + 1));
            // 判断是否第一个
            if (index != 0) {
                //否，注册监听到前一个节点，然后挂起
                CountDownLatch countDownLatch = new CountDownLatch(1);
                Stat exists = ZookeeperUtil.getClient().exists(path + "/" + children.get(index - 1), event -> {
                    countDownLatch.countDown();
                });
                if (exists != null) {
                    countDownLatch.await();
                }
            } else {
                return zNode;
            }
        }
    }

    @SneakyThrows
    public static String getPath(String methodName) {

        String path = concurrentHashMap.get(methodName);
        if (path == null) {
            Stat stat = ZookeeperUtil.getClient().exists("/locks/" + methodName, true);
            if (stat == null) {
                ZookeeperUtil.getClient().create("/locks/" + methodName, "locks".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            concurrentHashMap.put(methodName, "/locks/" + methodName);
        }
        return concurrentHashMap.get(methodName);
    }

}
