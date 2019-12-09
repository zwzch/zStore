package com.zstore.api.storge;

public interface Store {
    void start();
    void put(Message message);
    void get();
    void delete();
}
