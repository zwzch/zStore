package com.zstore.consensus.raft.service.impl;

import com.zstore.consensus.raft.service.RaftStateMachine;
import org.rocksdb.RocksDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RaftStateMachineRocksImpl implements RaftStateMachine {
    private static final Logger LOG = LoggerFactory.getLogger(RaftStateMachineRocksImpl.class);
    static {
        RocksDB.loadLibrary();
    }
    private RocksDB rocksDB;
    private String raftDataDir;

    public RaftStateMachineRocksImpl(String raftDataDir) {
        this.raftDataDir = raftDataDir;
    }

    @Override
    public void writeSnapshot(String snapshotDir) {

    }

    @Override
    public void readSnapshot(String snapshotDir) {

    }

    @Override
    public void apply(byte[] dataBytes) {

    }
}
