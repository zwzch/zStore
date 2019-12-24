package com.zstore.storge;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileData {
    private String fileName;
    private long curPoint;
    private File file;
    private File idFile;
    private AtomicInteger rps;
    private int lastReadTime;
    private ReadWriteLock rwLock;
    public FileData(String fileName, long curPoint, File file, File idFile) {
        this.fileName = fileName;
        this.curPoint = curPoint;
        this.file = file;
        this.idFile = idFile;
        this.rwLock = new ReentrantReadWriteLock();
        this.rps = new AtomicInteger(0);
    }

}

