package com.zstore.consensus.raft;

import com.google.protobuf.InvalidProtocolBufferException;
import com.googlecode.protobuf.format.JsonFormat;
import com.zstore.consensus.raft.proto.StoreProto;
import com.zstore.consensus.raft.service.RaftStateMachine;
import com.zstore.consensus.raft.storge.segment.Segment;
import com.zstore.consensus.raft.storge.segment.SegmentLog;
import com.zstore.consensus.raft.storge.snapshot.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RaftNode {
    private static final Logger LOG = LoggerFactory.getLogger(RaftNode.class);
    private static final JsonFormat jsonFormat = new JsonFormat();
    private RaftOptions raftOptions;
    private StoreProto.Configuration configuration;
    private ConcurrentHashMap<Integer, RaftPeer> peerMap = new ConcurrentHashMap<>();
    private StoreProto.Server localServer;
    private RaftStateMachine stateMachine;
    private SegmentLog raftLog;
    private Snapshot snapshot;
    private RaftNodeState state = RaftNodeState.STATE_FOLLOWER;
    // 服务器最后一次知道的任期号（初始化为 0，持续递增）
    private long currentTerm;
    // 在当前获得选票的候选人的Id
    private int votedFor;
    private int leaderId;
    // 已知的最大的已经被提交的日志条目的索引值
    private long commitIndex;
    // 最后被应用到状态机的日志条目索引值（初始化为 0，持续递增）
    private volatile long lastAppliedIndex;
    private Lock lock = new ReentrantLock();
    private Condition commitIndexCondition = lock.newCondition();
    private Condition catchUpCondition = lock.newCondition();
    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture electionScheduledFuture;
    private ScheduledFuture heartbeatScheduledFuture;
    public RaftNode(RaftOptions raftOptions, List<StoreProto.Server> servers,
                    StoreProto.Server localServer, RaftStateMachine stateMachine) {
        this.raftOptions = raftOptions;
        StoreProto.Configuration.Builder confBuilder = StoreProto.Configuration.newBuilder();
        for (StoreProto.Server server:servers) {
            confBuilder.addServers(server);
        }
        configuration = confBuilder.build();
        this.localServer = localServer;
        this.stateMachine = stateMachine;
        raftLog = new SegmentLog(raftOptions.getDataDir(), raftOptions.getMaxSegmentFileSize());
        snapshot = new Snapshot(raftOptions.getDataDir());
        snapshot.reload();
        currentTerm = raftLog.getMetaData().getCurrentTerm();
        votedFor = raftLog.getMetaData().getVotedFor();
        commitIndex = Math.max(snapshot.getMetaData().getLastIncludedIndex(), commitIndex);
        // 删除无用数据
        if (snapshot.getMetaData().getLastIncludedIndex() > 0
                && raftLog.getFirstIndex() <= snapshot.getMetaData().getLastIncludedIndex()) {
            raftLog.truncatePrefix(snapshot.getMetaData().getLastIncludedIndex() + 1);
        }
        // 应用状态机
        StoreProto.Configuration snapConf = snapshot.getMetaData().getConfiguration();
        if (confBuilder.getServersCount() > 0) {
            configuration = snapConf;
        }
        String snapshotDataDir = snapshot.getSnapshotDir() + File.separator + "data";
        stateMachine.readSnapshot(snapshotDataDir);
        for (long index = snapshot.getMetaData().getLastIncludedIndex() + 1;
             index <= commitIndex; index++) {
            StoreProto.LogEntry entry = raftLog.getEntry(index);
            if (entry.getType() == StoreProto.LogType.ENTRY_TYPE_DATA) {
                stateMachine.apply(entry.getData().toByteArray());
            } else if (entry.getType() == StoreProto.LogType.ENTRY_TYPE_CONFIGURATION) {
                applyConfiguration(entry);
            }
        }
        lastAppliedIndex = commitIndex;
    }

    public void applyConfiguration(StoreProto.LogEntry entry) {
        try {
            StoreProto.Configuration newConfiguration
                    = StoreProto.Configuration.parseFrom(entry.getData().toByteArray());
            configuration = newConfiguration;
            for (StoreProto.Server server : newConfiguration.getServersList()) {
                if (!peerMap.contains(server.getServerId()) && server.getServerId() != localServer.getServerId()) {
                    RaftPeer peer = new RaftPeer(server);
                    peer.setNextIndex(raftLog.getLastLogIndex() + 1);
                    peerMap.put(server.getServerId(), peer);

                }
            }
            LOG.info("new conf is {}, leaderId={}", jsonFormat.printToString(newConfiguration), leaderId);
        } catch (InvalidProtocolBufferException ex) {
            ex.printStackTrace();
        }
    }


}
