package com.zstore.consensus.raft;

import com.baidu.brpc.client.RpcCallback;
import com.google.protobuf.InvalidProtocolBufferException;
import com.googlecode.protobuf.format.JsonFormat;
import com.zstore.consensus.raft.constants.RaftNodeState;
import com.zstore.consensus.raft.constants.RaftOptions;
import com.zstore.consensus.raft.proto.StoreProto;
import com.zstore.consensus.raft.service.RaftStateMachine;
import com.zstore.consensus.raft.storge.segment.SegmentLog;
import com.zstore.consensus.raft.storge.snapshot.Snapshot;
import com.zstore.consensus.raft.utils.ConfigurationUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
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
        if (snapConf.getServersCount() > 0) {
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

    public void init() {
        for (StoreProto.Server server: configuration.getServersList()) {
            if (!peerMap.contains(server.getServerId()) && server.getServerId() != localServer.getServerId()) {
                RaftPeer peer = new RaftPeer(server);
                peer.setNextIndex(raftLog.getLastLogIndex() + 1);
                peerMap.put(server.getServerId(), peer);
            }
        }
        executorService = new ThreadPoolExecutor(
                raftOptions.getRaftConsensusThreadNum(),
                raftOptions.getRaftConsensusThreadNum(),
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
        scheduledExecutorService = Executors.newScheduledThreadPool(2);
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                takeSnapshot();
            }
        }, raftOptions.getSnapshotPeriodSeconds(), raftOptions.getSnapshotPeriodSeconds(), TimeUnit.SECONDS);
        resetElectionTimer();
    }

    private void resetElectionTimer() {
        if (electionScheduledFuture != null && !electionScheduledFuture.isDone()) {
            electionScheduledFuture.cancel(true);
        }
        electionScheduledFuture = scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                startPreVote();

            }
        }, getElectionTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    /**
     * 客户端发起pre-vote请求。
     * pre-vote/vote是典型的二阶段实现。
     * 作用是防止某一个节点断网后，不断的增加term发起投票；
     * 当该节点网络恢复后，会导致集群其他节点的term增大，导致集群状态变更。
     */
    private void startPreVote() {
        lock.lock();
        try {
            if (!ConfigurationUtils.containsServer(configuration, localServer.getServerId())) {
                resetElectionTimer();
                return;
            }
            LOG.info("Running pre-vote in term {}", currentTerm);
            state = RaftNodeState.STATE_PRE_CANDIDATE;
        } finally {
            lock.unlock();
        }
        for (StoreProto.Server server: configuration.getServersList()) {
            if (server.getServerId() == localServer.getServerId()) {
                continue;
            }
            final RaftPeer peer = peerMap.get(server.getServerId());
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    preVote(peer);
                }
            });
        }
    }

    private void preVote(RaftPeer peer) {
        LOG.info("begin pre vote request to server {}", peer.getServer().getServerId());
        StoreProto.VoteRequest.Builder requestBuilder = StoreProto.VoteRequest.newBuilder();
        lock.lock();
        try {
            peer.setVoteGranted(null);
            requestBuilder.setServerId(localServer.getServerId())
                    .setTerm(currentTerm)
                    .setLastLogIndex(raftLog.getLastLogIndex())
                    .setLastLogTerm(getLastLogTerm());
        } finally {
            lock.unlock();
        }
        StoreProto.VoteRequest request = requestBuilder.build();
        peer.getRaftConsensusServiceAsync().preVote(
                request, new PreVoteResponseCallback(peer, request));
    }
    public long getLastLogTerm() {
        long lastLogIndex = raftLog.getLastLogIndex();
        if (lastLogIndex >= raftLog.getFirstIndex()) {
            return raftLog.getEntryTerm(lastLogIndex);
        } else {
            // log为空，lastLogIndex == lastSnapshotIndex
            return snapshot.getMetaData().getLastIncludedTerm();
        }
    }
    /**
    * random elect timeout
    * */
    private int getElectionTimeoutMs() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int randomElectionTimeout = raftOptions.getElectionTimeoutMilliseconds()
                + random.nextInt(0, raftOptions.getElectionTimeoutMilliseconds());
        LOG.debug("new election time is after {} ms", randomElectionTimeout);
        return randomElectionTimeout;
    }

    public void takeSnapshot() {
//        snapshot
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

    public void stepDown(long newTerm) {
        if (currentTerm > newTerm) {
            LOG.error("can't be happened");
            return;
        }
        if (currentTerm < newTerm) {
            currentTerm = newTerm;
            leaderId = 0;
            votedFor = 0;
            raftLog.updateMetaData(currentTerm, votedFor, null);
        }
        state = RaftNodeState.STATE_FOLLOWER;
        // stop heartbeat
        if (heartbeatScheduledFuture != null && !heartbeatScheduledFuture.isDone()) {
            heartbeatScheduledFuture.cancel(true);
        }
        resetElectionTimer();
    }

    /**
     * 客户端发起正式vote，对candidate有效
     */
    private void startVote() {
        lock.lock();
        try {
            if (!ConfigurationUtils.containsServer(configuration, localServer.getServerId())) {
                resetElectionTimer();
                return;
            }
            // 投票前将自己的任期加一
            currentTerm++;
            LOG.info("Running for election in term {}", currentTerm);
            state = RaftNodeState.STATE_CANDIDATE;
            leaderId = 0;
            votedFor = localServer.getServerId();
        } finally {
            lock.unlock();
        }
        for (StoreProto.Server server: configuration.getServersList()) {
            if (server.getServerId() == localServer.getServerId()) {
                continue;
            }
            final RaftPeer peer = peerMap.get(server.getServerId());
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    requestVote(peer);
                }
            });
        }
    }

    /**
     * 客户端发起正式vote请求
     * @param peer 服务端节点信息
     */
    private void requestVote(RaftPeer peer) {
        LOG.info("begin vote request");
        lock.lock();
        StoreProto.VoteRequest.Builder requestBuilder = StoreProto.VoteRequest.newBuilder();
        try {
            peer.setVoteGranted(null);
            requestBuilder.setServerId(localServer.getServerId())
                    .setTerm(currentTerm)
                    .setLastLogIndex(raftLog.getLastLogIndex())
                    .setLastLogTerm(getLastLogTerm());
        } finally {
            lock.unlock();
        }
        StoreProto.VoteRequest request = requestBuilder.build();
        peer.getRaftConsensusServiceAsync().requestVote(request, new VoteResponseCallback(peer, request) );
    }

    private class VoteResponseCallback implements RpcCallback<StoreProto.VoteResponse> {
        private RaftPeer peer;
        private StoreProto.VoteRequest request;

        public VoteResponseCallback(RaftPeer peer, StoreProto.VoteRequest request) {
            this.peer = peer;
            this.request = request;
        }

        @Override
        public void success(StoreProto.VoteResponse response) {
            lock.lock();
            try {
                peer.setVoteGranted(response.getGranted());
                if (currentTerm != request.getTerm() || state != RaftNodeState.STATE_CANDIDATE) {
                    LOG.info("ignore requestVote RPC result");
                    return;
                }
                if (response.getTerm() > currentTerm) {
                    LOG.info("Received RequestVote response from server {} " +
                                    "in term {} (this server's term was {})",
                            peer.getServer().getServerId(),
                            response.getTerm(),
                            currentTerm);
                    stepDown(response.getTerm());
                } else {
                    if (response.getGranted()) {
                        LOG.info("Got vote from server {} for term {}",
                                peer.getServer().getServerId(), currentTerm);
                        int voteGrantedNum = 0;
                        if (votedFor == localServer.getServerId()) {
                            voteGrantedNum += 1;
                        }
                        for (StoreProto.Server server : configuration.getServersList()) {
                            if (server.getServerId() == localServer.getServerId()) {
                                continue;
                            }
                            RaftPeer peer1 = peerMap.get(server.getServerId());
                            if (peer1.getVoteGranted() != null && peer1.getVoteGranted() == true) {
                                voteGrantedNum += 1;
                            }
                        }
                        LOG.info("voteGrantedNum={}", voteGrantedNum);
                        if (voteGrantedNum > configuration.getServersCount() / 2) {
                            LOG.info("Got majority vote, serverId={} become leader", localServer.getServerId());
                            becomeLeader();
                        }
                    } else {
                        LOG.info("Vote denied by server {} with term {}, my term is {}",
                                peer.getServer().getServerId(), response.getTerm(), currentTerm);
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void fail(Throwable e) {
            LOG.warn("requestVote with peer[{}:{}] failed",
                    peer.getServer().getEndpoint().getHost(),
                    peer.getServer().getEndpoint().getPort());
            peer.setVoteGranted(new Boolean(false));
        }
    }

    private void becomeLeader() {
        System.out.println("become leader");
        state = RaftNodeState.STATE_LEADER;
        leaderId = localServer.getServerId();
        // stop vote timer
        if (electionScheduledFuture != null && !electionScheduledFuture.isDone()) {
            electionScheduledFuture.cancel(true);
        }
        // start heartbeat timer
        startNewHeartbeat();
    }

    // heartbeat timer, append entries
    // in lock
    private void resetHeartbeatTimer() {
        if (heartbeatScheduledFuture != null && !heartbeatScheduledFuture.isDone()) {
            heartbeatScheduledFuture.cancel(true);
        }
        heartbeatScheduledFuture = scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                startNewHeartbeat();
            }
        }, raftOptions.getHeartbeatPeriodMilliseconds(), TimeUnit.MILLISECONDS);
    }

    private void startNewHeartbeat() {
        LOG.info("start new heartbeat, peers={}", peerMap.keySet());
        for (final RaftPeer peer : peerMap.values()) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    appendEntries(peer);
                }
            });
        }
        resetHeartbeatTimer();
    }

    private void appendEntries(RaftPeer peer) {
        StoreProto.AppendEntriesRequest.Builder requestBuilder = StoreProto.AppendEntriesRequest.newBuilder();
        long prevLogIndex;
        long numEntries;
        boolean isNeedInstallSnapshot = false;
        lock.lock();
        try {
            long firstLogIndex = raftLog.getFirstIndex();
            if (peer.getNextIndex() < firstLogIndex) {
                isNeedInstallSnapshot = true;
            }
        } finally {
            lock.unlock();
        }
        LOG.info("is need snapshot={}, peer={}", isNeedInstallSnapshot, peer.getServer().getServerId());
        if (isNeedInstallSnapshot) {
            if (!installSnapshot(peer)) {
                return;
            }
        }
        long lastSnapshotIndex;
        long lastSnapshotTerm;
        snapshot.getLock().lock();
        try {
            lastSnapshotIndex = snapshot.getMetaData().getLastIncludedIndex();
            lastSnapshotTerm = snapshot.getMetaData().getLastIncludedTerm();
        } finally {
            snapshot.getLock().unlock();
        }
        lock.lock();
        try {
            long firstLogIndex = raftLog.getFirstIndex();
            Validate.isTrue(peer.getNextIndex() >= firstLogIndex);
            prevLogIndex = peer.getNextIndex() - 1;
            long prevLogTerm;
            if (prevLogIndex == 0) {
                prevLogTerm = 0;
            } else if (prevLogIndex == lastSnapshotIndex) {
                prevLogTerm = lastSnapshotTerm;
            } else {
                prevLogTerm = raftLog.getEntryTerm(prevLogIndex);
            }
            requestBuilder.setServerId(localServer.getServerId());
            requestBuilder.setTerm(currentTerm);
            requestBuilder.setPrevLogTerm(prevLogTerm);
            requestBuilder.setPrevLogIndex(prevLogIndex);
            numEntries = packEntries(peer.getNextIndex(), requestBuilder);
            requestBuilder.setCommitIndex(Math.min(commitIndex, prevLogIndex + numEntries));
        } finally {
            lock.unlock();
        }
        StoreProto.AppendEntriesRequest request = requestBuilder.build();
        StoreProto.AppendEntriesResponse response = peer.getRaftConsensusServiceAsync().appendEntries(request);

        lock.lock();
        try {
            // error handing
            if (response == null) {
                LOG.warn("appendEntries with peer[{}:{}] failed",
                        peer.getServer().getEndpoint().getHost(),
                        peer.getServer().getEndpoint().getPort());
                if (!ConfigurationUtils.containsServer(configuration, peer.getServer().getServerId())) {
                    peerMap.remove(peer.getServer().getServerId());
                    peer.getRpcClient().stop();
                }
                return;
            }
            LOG.info("AppendEntries response[{}] from server {} " +
                            "in term {} (my term is {})",
                    response.getResCode(), peer.getServer().getServerId(),
                    response.getTerm(), currentTerm);
            if (response.getTerm() > currentTerm) {
                // 任期变更
                stepDown(response.getTerm());
            } else {
                if (response.getResCode() == StoreProto.ResCode.RES_CODE_SUCCESS) {
                    peer.setMatchIndex(prevLogIndex + numEntries);
                    peer.setNextIndex(peer.getMatchIndex() + 1);
                    if (ConfigurationUtils.containsServer(configuration, peer.getServer().getServerId())) {
                        advanceCommitIndex();
                    } else {
                        if (raftLog.getLastLogIndex() - peer.getMatchIndex() <= raftOptions.getCatchupMargin()) {
                            LOG.debug("peer catch up the leader");
                            peer.setCatchUp(true);
                            // signal the caller thread
                            catchUpCondition.signalAll();
                        }
                    }
                } else {
                    peer.setNextIndex(response.getLastLogIndex() + 1);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void advanceCommitIndex() {
        int peerNum = configuration.getServersList().size();
        long[] matchIndexes = new long[peerNum];
        int i = 0;
        // 获取每个matchIndex
        for (StoreProto.Server server : configuration.getServersList()) {
            if (server.getServerId() != localServer.getServerId()) {
                RaftPeer peer = peerMap.get(server.getServerId());
                matchIndexes[i++] = peer.getMatchIndex();
            }
        }
        // leader index
        matchIndexes[i] = raftLog.getLastLogIndex();
        Arrays.sort(matchIndexes);
        long newCommitIndex = matchIndexes[peerNum/2];
        LOG.debug("newCommitIndex={}, oldCommitIndex={}", newCommitIndex, commitIndex);
        // term error
        if (raftLog.getEntryTerm(newCommitIndex) != currentTerm) {
            LOG.debug("newCommitIndexTerm={}, currentTerm={}",
                    raftLog.getEntryTerm(newCommitIndex), currentTerm);
            return;
        }
        if (commitIndex >= newCommitIndex) {
            return;
        }
        long oldCommitIndex = commitIndex;
        commitIndex = newCommitIndex;
        // 同步到状态机
        for (long index = oldCommitIndex + 1; index <= newCommitIndex; index++) {
            StoreProto.LogEntry entry = raftLog.getEntry(index);
            if (entry.getType() == StoreProto.LogType.ENTRY_TYPE_DATA) {
                stateMachine.apply(entry.getData().toByteArray());
            } else if (entry.getType() == StoreProto.LogType.ENTRY_TYPE_CONFIGURATION) {
                applyConfiguration(entry);
            }
        }
        lastAppliedIndex = commitIndex;
        LOG.debug("commitIndex={} lastAppliedIndex={}", commitIndex, lastAppliedIndex);
        commitIndexCondition.signalAll();
    }

    // in lock
    private long packEntries(long nextIndex, StoreProto.AppendEntriesRequest.Builder requestBuilder) {
        long lastIndex = Math.min(raftLog.getLastLogIndex(),
                nextIndex + raftOptions.getMaxLogEntriesPerRequest() - 1);
        for (long index = nextIndex; index <= lastIndex; index++) {
            StoreProto.LogEntry entry = raftLog.getEntry(index);
            requestBuilder.addEntries(entry);
        }
        return lastIndex - nextIndex + 1;
    }

    private boolean installSnapshot(RaftPeer peer) {
        return true;
    }

    private class PreVoteResponseCallback implements RpcCallback<StoreProto.VoteResponse> {
        private RaftPeer peer;
        private StoreProto.VoteRequest request;

        public PreVoteResponseCallback(RaftPeer peer, StoreProto.VoteRequest request) {
            this.peer = peer;
            this.request = request;
        }

        @Override
        public void success(StoreProto.VoteResponse response) {
            lock.lock();
            try {
                peer.setVoteGranted(response.getGranted());
                if (currentTerm != request.getTerm() || state != RaftNodeState.STATE_PRE_CANDIDATE) {
                    LOG.info("ignore preVote RPC result");
                    return;
                }
                if (response.getTerm() > currentTerm) {
                    LOG.info("Received pre vote response from server {} " +
                                    "in term {} (this server's term was {})",
                            peer.getServer().getServerId(),
                            response.getTerm(),
                            currentTerm);
                    stepDown(response.getTerm());
                } else {
                    if (response.getGranted()) {
                        LOG.info("get pre vote granted from server {} for term {}",
                                peer.getServer().getServerId(), currentTerm);
                        int voteGrantedNum = 1;
                        for (StoreProto.Server server : configuration.getServersList()) {
                            if (server.getServerId() == localServer.getServerId()) {
                                continue;
                            }
                            RaftPeer peer1 = peerMap.get(server.getServerId());
                            if (peer1.getVoteGranted() != null && peer1.getVoteGranted() == true) {
                                voteGrantedNum += 1;
                            }
                        }
                        LOG.info("preVoteGrantedNum={}", voteGrantedNum);
                        if (voteGrantedNum > configuration.getServersCount() / 2) {
                            // 超过半数开始真正投票
                            LOG.info("get majority pre vote, serverId={} when pre vote, start vote",
                                    localServer.getServerId());
                            startVote();
                        }
                    } else {
                        LOG.info("pre vote denied by server {} with term {}, my term is {}",
                                peer.getServer().getServerId(), response.getTerm(), currentTerm);
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void fail(Throwable e) {
            LOG.warn("pre vote with peer[{}:{}] failed",
                    peer.getServer().getEndpoint().getHost(),
                    peer.getServer().getEndpoint().getPort());
            peer.setVoteGranted(new Boolean(false));
        }
    }

    public Lock getLock() {
        return lock;
    }

    public void setLock(Lock lock) {
        this.lock = lock;
    }

    public long getCurrentTerm() {
        return currentTerm;
    }

    public void setCurrentTerm(long currentTerm) {
        this.currentTerm = currentTerm;
    }

    public StoreProto.Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(StoreProto.Configuration configuration) {
        this.configuration = configuration;
    }

    public SegmentLog getRaftLog() {
        return raftLog;
    }

    public void setRaftLog(SegmentLog raftLog) {
        this.raftLog = raftLog;
    }

    public int getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(int votedFor) {
        this.votedFor = votedFor;
    }

    public int getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(int leaderId) {
        this.leaderId = leaderId;
    }

    public long getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(long commitIndex) {
        this.commitIndex = commitIndex;
    }

    public long getLastAppliedIndex() {
        return lastAppliedIndex;
    }

    public void setLastAppliedIndex(long lastAppliedIndex) {
        this.lastAppliedIndex = lastAppliedIndex;
    }

    public RaftStateMachine getStateMachine() {
        return stateMachine;
    }

    public void setStateMachine(RaftStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }
}
