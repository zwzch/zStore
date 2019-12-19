package com.zstore.api.config;

public class NettyConfig {
    public static final int NETTY_SERVER_WORKER_THREAD_COUNT = 1;
    public static final int NETTY_SERVER_BOSS_THREAD_COUNT = 1;
    public static final int NETTY_SERVER_SO_BACKLOG = 100;
    public static final int NETTY_SERVER_PORT = 4396;
    public static final int NETTY_SERVER_MAX_INITIAL_LINE_LENGTH = 4096;
    public static final int NETTY_SERVER_MAX_HEADER_SIZE = 8192;
    public static final int NETTY_SERVER_MAX_CHUNK_SIZE = 8192;
    public static final int NETTY_SERVER_IDLE_TIME_SECONDS = 60;

}
