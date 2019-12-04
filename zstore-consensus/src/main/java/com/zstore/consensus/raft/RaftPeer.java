package com.zstore.consensus.raft;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.instance.Endpoint;
import com.zstore.consensus.raft.proto.StoreProto;
import com.zstore.consensus.raft.service.RaftConsensusServiceAsync;

public class RaftPeer {
    private StoreProto.Server server;
    private RpcClient rpcClient;
    // 需要发送给follower的下一个日志条目的索引值，只对leader有效
    private long nextIndex;
    private volatile boolean isCatchUp;
    private volatile Boolean voteGranted;
    private RaftConsensusServiceAsync raftConsensusServiceAsync;
    // 已复制日志的最高索引值
    private long matchIndex;

    public RaftPeer(StoreProto.Server server) {
        this.server = server;
        this.rpcClient = new RpcClient(new Endpoint(
                server.getEndpoint().getHost(),
                server.getEndpoint().getPort()));
        this.raftConsensusServiceAsync = BrpcProxy.getProxy(rpcClient,RaftConsensusServiceAsync.class );

        isCatchUp = false;
    }

    public long getNextIndex() {
        return nextIndex;
    }

    public void setNextIndex(long nextIndex) {
        this.nextIndex = nextIndex;
    }

    public Boolean getVoteGranted() {
        return voteGranted;
    }

    public void setVoteGranted(Boolean voteGranted) {
        this.voteGranted = voteGranted;
    }

    public RaftConsensusServiceAsync getRaftConsensusServiceAsync() {
        return raftConsensusServiceAsync;
    }

    public long getMatchIndex() {
        return matchIndex;
    }

    public void setMatchIndex(long matchIndex) {
        this.matchIndex = matchIndex;
    }

    public RpcClient getRpcClient() {
        return rpcClient;
    }

    public void setRpcClient(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }


    public void setRaftConsensusServiceAsync(RaftConsensusServiceAsync raftConsensusServiceAsync) {
        this.raftConsensusServiceAsync = raftConsensusServiceAsync;
    }

    public StoreProto.Server getServer() {
        return server;
    }

    public void setServer(StoreProto.Server server) {
        this.server = server;
    }

    public boolean isCatchUp() {
        return isCatchUp;
    }

    public void setCatchUp(boolean catchUp) {
        isCatchUp = catchUp;
    }
}
