package com.zstore.consensus.raft.storge.segment;

import com.zstore.consensus.raft.proto.StoreProto;

public class Record {
    public long offset;
    public StoreProto.LogEntry entry;
    public Record(long offset, StoreProto.LogEntry entry) {
        this.offset = offset;
        this.entry = entry;
    }
}
