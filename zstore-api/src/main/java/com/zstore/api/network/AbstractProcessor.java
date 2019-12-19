package com.zstore.api.network;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractProcessor implements Runnable, Processor {
    private AtomicBoolean isAlive;
    private CountDownLatch startupContdownLatch;
    private CountDownLatch shutdownContdownLatch;

    public AbstractProcessor() {
        isAlive = new AtomicBoolean(false);
        startupContdownLatch = new CountDownLatch(1);
        shutdownContdownLatch = new CountDownLatch(1);
    }

    @Override
    public void shutdown() throws InterruptedException {
        isAlive.set(false);
        shutdownContdownLatch.await();
    }

    @Override
    public void startup() throws InterruptedException {
        isAlive.set(true);
        startupContdownLatch.await();
    }

    @Override
    public void startupComplete() {
        startupContdownLatch.countDown();
    }

    @Override
    public void shutdownComplete() {
        shutdownContdownLatch.countDown();
    }

    @Override
    public boolean isAlive() {
        return isAlive.get();
    }
}
