package com.zstore.consensus.raft.service;

import com.baidu.brpc.client.RpcCallback;
import com.zstore.consensus.raft.proto.StoreProto;

import java.util.concurrent.Future;

/**
 * 用于生成client异步调用所需的proxy
 * Created by wenweihu86 on 2017/5/2.
 */
public interface RaftConsensusServiceAsync extends RaftConsensusService {

    Future<StoreProto.VoteResponse> preVote(
            StoreProto.VoteRequest request,
            RpcCallback<StoreProto.VoteResponse> callback);

    Future<StoreProto.VoteResponse> requestVote(
            StoreProto.VoteRequest request,
            RpcCallback<StoreProto.VoteResponse> callback);

//    Future<RaftProto.AppendEntriesResponse> appendEntries(
//            RaftProto.AppendEntriesRequest request,
//            RpcCallback<RaftProto.AppendEntriesResponse> callback);
//
//    Future<RaftProto.InstallSnapshotResponse> installSnapshot(
//            RaftProto.InstallSnapshotRequest request,
//            RpcCallback<RaftProto.InstallSnapshotResponse> callback);
}
