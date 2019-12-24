package com.zstore.api.storge;

public interface MetaStore {
    void start() throws Exception;
    void put(byte[] key, byte[] value) throws Exception;
    byte[] get(byte[] key) throws Exception;
    void delete(byte[] key) throws Exception;
    boolean exist(byte[] key);
}
