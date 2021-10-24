package com.myself.practice.skiplist;

import lombok.Data;

public class SkipList {
    private final static int MAX_LEVEL = 32;
    private final static double P = 0.25;

    private SkipListNode header;

    private SkipListNode tail;

    private int level = 1;

    private int length;

    public static void main(String[] args) {
        SkipList skipList = new SkipList();
        skipList.insert(3);
        skipList.insert(4);
        skipList.insert(1);
        skipList.display();
        System.out.println(skipList.find(3));
        System.out.println(skipList.find(5));
        skipList.insert(5);
        skipList.display();
        skipList.delete(4);
        skipList.display();

    }

    public SkipList() {
        this.header = new SkipListNode();
        this.header.setNext(new SkipListNode[32]);
    }

    /**
     * 跳表插入函数
     * @param value
     */
    public void insert(int value) {
        int level = randomLevel();
        SkipListNode skipListNode = new SkipListNode(value, level);
        SkipListNode curNode = this.header;

        int curLevel = level - 1;
        // 从当前所有节点中的最高层数开始遍历
        while (curLevel >= 0) {
            // 依次对没一层进行搜寻并连接节点
            while (curNode.next[curLevel] != null && curNode.next[curLevel].value < value) {
                curNode = curNode.next[curLevel];
            }
            if (curNode.next[curLevel] != null) {
                // 说明 curNode.next[curLevel].value >= value，则将节点插入curNode 和 curNode.next[curLevel] 之间
                // 否则该层没有找到节点间插入位置，直接插入该层最尾处
                skipListNode.next[curLevel] = curNode.next[curLevel];
            }
            curNode.next[curLevel] = skipListNode;
            // 进入下一层
            curLevel--;
        }
        // 此时以及是最底层了，backward 直接指向curNode，为上一个节点
        skipListNode.backward = curNode;
        length++;
        this.level = Math.max(this.level, level);
        // 末尾节点变更
        if (skipListNode.next[0] == null) tail = skipListNode;

    }

    /**
     * 跳表查找函数
     * @param value
     * @return
     */
    public SkipListNode find(int value) {

        int curLevel = this.level - 1;
        SkipListNode curNode = this.header;

        while (curLevel >= 0) {
            // 在curLevel层搜索，
            while (curNode.next[curLevel] != null && curNode.next[curLevel].value < value) {
                curNode = curNode.next[curLevel];
            }
            // 如果当前找到，则会直接一直下沉，没有找到，则会下一层继续向右寻找
            curLevel--;
        }
        // 最后判断下一个节点是否为寻找的值
        if (curNode.next[0] != null && curNode.next[0].value == value) {
            return curNode.next[0];
        } else {
            return null;
        }

    }

    /**
     * 跳表删除函数
     * @param value
     * @return
     */
    public SkipListNode delete(int value) {

        // 先尝试找节点，找不到直接返回
        SkipListNode skipListNode = find(value);
        if (skipListNode == null) return null;
        int curLevel = this.level - 1;
        SkipListNode curNode = this.header;
        // 根据前面找到的节点，再来一次遍历，只要找到下一个节点是需要删除的节点，则直接将当前节点指向被删除节点的下一个节点
        while (curLevel >= 0) {
            while (curNode.next[curLevel] != null && curNode.next[curLevel] != skipListNode) {
                curNode = curNode.next[curLevel];
            }
            if (curNode.next[curLevel] != null) {
                curNode.next[curLevel] = curNode.next[curLevel].next[curLevel];
            }
            curLevel--;
        }
        // 尾节点判断
        if (skipListNode.next[0] == null) {
            tail = skipListNode.backward;
        }
        skipListNode.backward = null;
        return skipListNode;
    }

    @Data
    private static class SkipListNode {
        private int value = -1;
        // 节点的前一个节点
        private SkipListNode backward;
        // 节点的下一个节点数组
        private SkipListNode[] next;

        public SkipListNode() {
        }

        public SkipListNode(int value, int level) {
            this.value = value;
            this.next = new SkipListNode[level];
        }

        @Override
        public String toString() {
            return "SkipListNode{" +
                    "value=" + value +
                    '}';
        }
    }

    /**
     * 按一定的概率生成节点层数，P为统计所得，和redis一样
     *
     * @return
     */
    private int randomLevel() {
        int level = 1;
        while (Math.random() < P && level < MAX_LEVEL) {
            level += 1;
        }
        return level;
    }

    // 显示跳表中的结点
    public void display() {
        SkipListNode p = header;
        while (p.next[0] != null) {
            System.out.println(p.next[0] + " ");
            p = p.next[0];
        }
        System.out.println();
    }

}
