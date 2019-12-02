package com.zstore.consensus.raft;

public enum RaftNodeState {
    STATE_FOLLOWER,
    STATE_PRE_CANDIDATE,
    STATE_CANDIDATE,
    STATE_LEADER
}
