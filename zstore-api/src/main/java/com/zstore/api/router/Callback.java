package com.zstore.api.router;

public interface Callback<T> {
    void onCompletion(T result, Exception exception);
}
