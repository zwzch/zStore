package com.zstore.consensus.raft;

import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.instance.Endpoint;
import com.zstore.consensus.raft.proto.StoreProto;

public class RaftPeer {
    private StoreProto.Server server;
    private RpcClient rpcClient;
    // 需要发送给follower的下一个日志条目的索引值，只对leader有效
    private long nextIndex;
    private volatile boolean isCatchUp;

    public RaftPeer(StoreProto.Server server) {
        this.server = server;
        this.rpcClient = new RpcClient(new Endpoint(
                server.getEndpoint().getHost(),
                server.getEndpoint().getPort()));

        isCatchUp = false;
    }

    public long getNextIndex() {
        return nextIndex;
    }

    public void setNextIndex(long nextIndex) {
        this.nextIndex = nextIndex;
    }
}
