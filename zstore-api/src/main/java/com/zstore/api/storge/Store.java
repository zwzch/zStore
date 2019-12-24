package com.zstore.api.storge;

public interface Store {
    void start();
    void put();
    void get();
    void delete();
    boolean exist();
}
