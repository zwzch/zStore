package com.zstore.consensus.raft.service;

import com.baidu.brpc.client.RpcCallback;
import com.zstore.consensus.raft.proto.StoreProto;

import java.util.concurrent.Future;

/**
 * 用于生成client异步调用所需的proxy
 */
public interface RaftConsensusServiceAsync extends RaftConsensusService {

    Future<StoreProto.VoteResponse> preVote(
            StoreProto.VoteRequest request,
            RpcCallback<StoreProto.VoteResponse> callback);

    Future<StoreProto.VoteResponse> requestVote(
            StoreProto.VoteRequest request,
            RpcCallback<StoreProto.VoteResponse> callback);

    Future<StoreProto.AppendEntriesResponse> appendEntries(
            StoreProto.AppendEntriesRequest request,
            RpcCallback<StoreProto.AppendEntriesResponse> callback);
//
//    Future<RaftProto.InstallSnapshotResponse> installSnapshot(
//            RaftProto.InstallSnapshotRequest request,
//            RpcCallback<RaftProto.InstallSnapshotResponse> callback);
}
