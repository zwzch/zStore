package com.zstore.consensus.raft.service;

import com.zstore.consensus.raft.proto.StoreProto;

public interface RaftConsensusService {

    StoreProto.VoteResponse preVote(StoreProto.VoteRequest request);

    StoreProto.VoteResponse requestVote(StoreProto.VoteRequest request);

    StoreProto.AppendEntriesResponse appendEntries(StoreProto.AppendEntriesRequest request);
//
//    StoreProto.InstallSnapshotResponse installSnapshot(StoreProto.InstallSnapshotRequest request);
}