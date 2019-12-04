package com.zstore.consensus.raft.constants;

public enum RaftNodeState {
    STATE_FOLLOWER,
    STATE_PRE_CANDIDATE,
    STATE_CANDIDATE,
    STATE_LEADER
}
