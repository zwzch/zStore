package com.zstore.api.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadUtils {
    public static final Logger log = LoggerFactory.getLogger(ThreadUtils.class);
    public static Thread newThread(Runnable runnable, String name, boolean daemon) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(daemon);
        thread.setUncaughtExceptionHandler((t, e) -> {
            log.error("Encountered throwable in {}", t, e);
        });
        return thread;
    }
}
