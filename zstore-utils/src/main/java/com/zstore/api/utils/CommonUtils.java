package com.zstore.api.utils;

public class CommonUtils {
    public static Throwable getRootCause(Throwable t) {
        Throwable throwable = t;
        while (throwable != null && throwable.getCause() != null) {
            throwable = throwable.getCause();
        }
        return throwable;
    }
}
