package com.myself.practice.distributedlock.zookeeper;

import lombok.SneakyThrows;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class ZookeeperLock {

    /* 将方法名和zookeeper节点路径缓存起来，提高性能，可能存在的问题就是缓存下来了，有其他人偷偷删了，典型缓存不一致问题，可以根据实际情况思考 */
    private static final ConcurrentHashMap<String, String> concurrentHashMap = new ConcurrentHashMap<>();

    @SneakyThrows
    public static void unLock(String zNode) {
        // 因为删除的接口有个版本号，所以先获取节点的信息，再删除
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

    /**
     *
     * @param methodName
     * @return
     */
    @SneakyThrows
    public static String getPath(String methodName) {

        String path = concurrentHashMap.get(methodName);
        if (path == null) {
            // 通过方法名，判断zookeeper目录节点是否存在，不存在则创建一个持久性节点
            Stat stat = ZookeeperUtil.getClient().exists("/locks/" + methodName, true);
            if (stat == null) {
                ZookeeperUtil.getClient().create("/locks/" + methodName, "locks".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            concurrentHashMap.put(methodName, "/locks/" + methodName);
        }
        return concurrentHashMap.get(methodName);
    }

}
