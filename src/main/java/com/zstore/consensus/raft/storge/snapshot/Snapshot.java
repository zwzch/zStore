package com.zstore.consensus.raft.storge.snapshot;

import com.zstore.consensus.raft.proto.StoreProto;
import com.zstore.consensus.raft.utils.StoreFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Snapshot {
    private static final Logger LOG = LoggerFactory.getLogger(Snapshot.class);
    private String snapshotDir;
    private StoreProto.SnapshotMetaData metaData;
    // 表示是否正在安装snapshot，leader向follower安装，leader和follower同时处于installSnapshot状态
    private AtomicBoolean isInstallSnapshot = new AtomicBoolean(false);
    // 表示节点自己是否在对状态机做snapshot
    private AtomicBoolean isTakeSnapshot = new AtomicBoolean(false);
    private Lock lock = new ReentrantLock();

    public Snapshot(String snapshotDir){
        this.snapshotDir = snapshotDir + File.separator + "snapshot";
        String snapshotDataDir = snapshotDir + File.separator + "data";
        File file = new File(snapshotDataDir);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public void reload() {
        metaData = this.readMetaData();
        if (metaData == null) {
            metaData = StoreProto.SnapshotMetaData.newBuilder().build();
        }
    }

    public StoreProto.SnapshotMetaData readMetaData() {
        String fileName = snapshotDir + File.separator + "metadata";
        File file = new File(fileName);
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            StoreProto.SnapshotMetaData metadata = StoreFileUtils.readProtoFromFile(
                    randomAccessFile, StoreProto.SnapshotMetaData.class);
            return metadata;
        } catch (IOException ex) {
            LOG.warn("meta file not exist, name={}", fileName);
            return null;
        }
    }

    public StoreProto.SnapshotMetaData getMetaData() {
        return metaData;
    }

    public String getSnapshotDir() {
        return snapshotDir;
    }
}
