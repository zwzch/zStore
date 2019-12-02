package com.zstore.consensus.raft.common;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class RaftThreadFactory implements ThreadFactory {
    private AtomicInteger counter;
    private String prefix;

    public RaftThreadFactory(String prefix) {
        this.prefix = prefix;
        counter = new AtomicInteger();
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(prefix + counter.getAndIncrement());
    }
}
