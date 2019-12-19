package com.zstore.api.network;

public interface Processor {
    /**
     * wait for shutdown
     * */
    void shutdown() throws InterruptedException;
    /**
     * wait for startup
     * */
    void startup() throws InterruptedException;
    /**
     * startup complete
     * */
    void startupComplete();
    /**
     * shutdown complete
     * */
    void shutdownComplete();
    /**
     * processer is alive
     * */
    boolean isAlive();
}
