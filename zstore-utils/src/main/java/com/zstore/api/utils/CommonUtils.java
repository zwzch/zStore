package com.zstore.api.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class CommonUtils {
    public static final Logger log = LoggerFactory.getLogger(CommonUtils.class);
    public static Throwable getRootCause(Throwable t) {
        Throwable throwable = t;
        while (throwable != null && throwable.getCause() != null) {
            throwable = throwable.getCause();
        }
        return throwable;
    }
    public static void createFile(File file) {
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            log.error("create file error msg={}", e.getMessage());
            throw new RuntimeException("create file error");
        }
    }
}
