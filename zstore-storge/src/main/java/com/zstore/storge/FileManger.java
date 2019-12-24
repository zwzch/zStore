package com.zstore.storge;

import org.apache.commons.collections4.map.LRUMap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;

public class FileManger {
    private ReadWriteLock readWriteLock;
    private ConcurrentHashMap fileMap;
    private ConcurrentHashMap md5Map;
    private long fileMaxSize;
    private byte[] fileSequence;
    private LRUMap nameCacheMap;
    private String currFileName;


}
