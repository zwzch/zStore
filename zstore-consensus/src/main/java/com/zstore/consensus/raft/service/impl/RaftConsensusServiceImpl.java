package com.zstore.consensus.raft.service.impl;

import com.googlecode.protobuf.format.JsonFormat;
import com.zstore.consensus.raft.RaftNode;
import com.zstore.consensus.raft.constants.CommonConstants;
import com.zstore.consensus.raft.proto.StoreProto;
import com.zstore.consensus.raft.service.RaftConsensusService;
import com.zstore.consensus.raft.utils.ConfigurationUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RaftConsensusServiceImpl implements RaftConsensusService {
    private static final Logger LOG = LoggerFactory.getLogger(RaftConsensusServiceImpl.class);
    private static final JsonFormat PRINTER = new JsonFormat();

    private RaftNode raftNode;

    public RaftConsensusServiceImpl(RaftNode node) {
        this.raftNode = node;
    }
    @Override
    public StoreProto.VoteResponse preVote(StoreProto.VoteRequest request) {
        raftNode.getLock().lock();
        try {
            StoreProto.VoteResponse.Builder responseBuilder = StoreProto.VoteResponse.newBuilder();
            responseBuilder.setGranted(false);
            responseBuilder.setTerm(raftNode.getLastLogTerm());
            if (!ConfigurationUtils.containsServer(raftNode.getConfiguration(), request.getServerId())) {
                return responseBuilder.build();
            }
            if (request.getTerm() < raftNode.getCurrentTerm()) {
                // 任期比自己小
                return responseBuilder.build();
            }
            boolean isLogOk = request.getLastLogTerm() > raftNode.getLastLogTerm()
                    || (request.getLastLogTerm() == raftNode.getLastLogTerm()
                    && request.getLastLogIndex() >= raftNode.getRaftLog().getLastLogIndex());
            if (!isLogOk) {
                // 日志不合格
                return responseBuilder.build();
            } else {
                // 参加竞选
                responseBuilder.setGranted(true);
                responseBuilder.setTerm(raftNode.getCurrentTerm());
            }
            LOG.info("preVote request from server {} " +
                            "in term {} (my term is {}), granted={}",
                    request.getServerId(), request.getTerm(),
                    raftNode.getCurrentTerm(), responseBuilder.getGranted());
            return responseBuilder.build();
        } finally {
            raftNode.getLock().unlock();
        }
    }

    @Override
    public StoreProto.VoteResponse requestVote(StoreProto.VoteRequest request) {
        raftNode.getLock().lock();
        String message = "";
        try {
            StoreProto.VoteResponse.Builder responseBuilder = StoreProto.VoteResponse.newBuilder();
            responseBuilder.setGranted(false);
            responseBuilder.setTerm(raftNode.getCurrentTerm());
            // 配置错误
            if (!ConfigurationUtils.containsServer(raftNode.getConfiguration(), request.getServerId())) {
                message = CommonConstants.VOTE_REQ_CONFIG_ERR;
                return responseBuilder.build();
            }
            // 拒绝任期比自己小
            if (request.getTerm() < raftNode.getCurrentTerm()) {
                message = CommonConstants.VOTE_REQ_SHORT_TERM;
                return responseBuilder.build();
            }
            // 任期比自己大 给他投票
            if (request.getTerm() < raftNode.getCurrentTerm()) {
                message = CommonConstants.VOTE_REQ_LONG_TERM;
                raftNode.stepDown(request.getTerm());
            }
            // 日志比较任期和index
            boolean logIsOk = request.getLastLogTerm() > raftNode.getLastLogTerm()
                    || (request.getLastLogTerm() == raftNode.getLastLogTerm()
                    && request.getLastLogIndex() >= raftNode.getRaftLog().getLastLogIndex());
            // 日志合格且自身还没有投票
            if (raftNode.getVotedFor() == 0 && logIsOk) {
                message = CommonConstants.VOTE_REQ_VOTE_ELECT;
                raftNode.stepDown(request.getTerm());
                raftNode.setVotedFor(request.getServerId());
                // 更新元数据
                raftNode.getRaftLog().updateMetaData(raftNode.getCurrentTerm(), raftNode.getVotedFor(), null);
                responseBuilder.setGranted(true);
                responseBuilder.setTerm(raftNode.getCurrentTerm());
            }
            LOG.info("RequestVote request from server {} {} " +
                            "in term {} (my term is {}), granted={}",
                    message,
                    request.getServerId(), request.getTerm(),
                    raftNode.getCurrentTerm(), responseBuilder.getGranted());
            return responseBuilder.build();
        } finally {
            raftNode.getLock().unlock();
        }
    }

    @Override
    public StoreProto.AppendEntriesResponse appendEntries(StoreProto.AppendEntriesRequest request) {
       raftNode.getLock().lock();
       try {
           StoreProto.AppendEntriesResponse.Builder responseBuilder = StoreProto.AppendEntriesResponse.newBuilder();
           responseBuilder.setTerm(raftNode.getCurrentTerm());
           responseBuilder.setResCode(StoreProto.ResCode.RES_CODE_FAIL);
           responseBuilder.setLastLogIndex(raftNode.getRaftLog().getLastLogIndex());
           // 请求任期小 拒绝写入
           if (request.getTerm() < raftNode.getCurrentTerm()) {
               return responseBuilder.build();
           }
           // 更新任期 参加选举
           raftNode.stepDown(request.getTerm());
           if (raftNode.getLeaderId() == 0) {
               // 没有leader 更新leader
               raftNode.setLeaderId(request.getServerId());
               LOG.info("new leaderId={}, conf={}",
                       raftNode.getLeaderId(),
                       PRINTER.printToString(raftNode.getConfiguration()));
           }
           if (raftNode.getLeaderId() != request.getServerId()) {
               // leader 不同 Term 加一 参加选举
               LOG.warn("Another peer={} declares that it is the leader " +
                               "at term={} which was occupied by leader={}",
                       request.getServerId(), request.getTerm(), raftNode.getLeaderId());
               raftNode.stepDown(request.getTerm() + 1);
               responseBuilder.setResCode(StoreProto.ResCode.RES_CODE_FAIL);
               responseBuilder.setTerm(request.getTerm() + 1);
               return responseBuilder.build();
           }
           if (request.getPrevLogIndex() > raftNode.getRaftLog().getLastLogIndex()) {
               // 日志进度不同 将日志进度减一 返回
               LOG.info("Rejecting AppendEntries RPC would leave gap, " +
                               "request prevLogIndex={}, my lastLogIndex={}",
                       request.getPrevLogIndex(), raftNode.getRaftLog().getLastLogIndex());
               Validate.isTrue(request.getPrevLogIndex() > 0);
               responseBuilder.setLastLogIndex(request.getPrevLogIndex() - 1);
               return responseBuilder.build();
           }
           if (request.getEntriesCount() == 0) {
               // 心跳
               LOG.info("heartbeat request from peer={} at term={}, my term={}",
                       request.getServerId(), request.getTerm(), raftNode.getCurrentTerm());
               responseBuilder.setResCode(StoreProto.ResCode.RES_CODE_SUCCESS);
               responseBuilder.setTerm(raftNode.getCurrentTerm());
               responseBuilder.setLastLogIndex(raftNode.getRaftLog().getLastLogIndex());
               advanceCommitIndex(request);
               return responseBuilder.build();
           }
           responseBuilder.setResCode(StoreProto.ResCode.RES_CODE_SUCCESS);
           List<StoreProto.LogEntry> entries = new ArrayList<>();
           long index = request.getPrevLogIndex();
           // 写入日志
           for (StoreProto.LogEntry entry : request.getEntriesList()) {
               index++;
               if (index < raftNode.getRaftLog().getFirstIndex()) {
                   continue;
               }
               // 本地日志比写入日志index大
               if (raftNode.getRaftLog().getLastLogIndex() >= index) {
                   if (raftNode.getRaftLog().getEntryTerm(index) == entry.getTerm()) {
                       // term 相同跳过
                       continue;
                   }
                   // 删除错误日志
                   // truncate segment log from index
                   long lastIndexKept = index - 1;
                   raftNode.getRaftLog().truncatePrefix(lastIndexKept);
               }
               entries.add(entry);
           }
           raftNode.getRaftLog().append(entries);
           // 更新元数据
           raftNode.getRaftLog().updateMetaData(raftNode.getCurrentTerm(),
                   null, raftNode.getRaftLog().getFirstIndex());
           advanceCommitIndex(request);
           advanceCommitIndex(request);
           LOG.info("AppendEntries request from server {} " +
                           "in term {} (my term is {}), entryCount={} resCode={}",
                   request.getServerId(), request.getTerm(), raftNode.getCurrentTerm(),
                   request.getEntriesCount(), responseBuilder.getResCode());
           return responseBuilder.build();
       } finally {
           raftNode.getLock().unlock();
       }
    }

    private void advanceCommitIndex(StoreProto.AppendEntriesRequest request) {
        long newCommitIndex = Math.min(request.getCommitIndex(),
                request.getPrevLogIndex() + request.getEntriesCount());
        raftNode.setCommitIndex(newCommitIndex);
        // applay index 小于 commit Index
        if (raftNode.getLastAppliedIndex() < raftNode.getCommitIndex()) {
            // apply state machine
            for (long index = raftNode.getLastAppliedIndex() + 1;
                 index <= raftNode.getCommitIndex(); index++) {
                StoreProto.LogEntry entry = raftNode.getRaftLog().getEntry(index);
                if (entry != null) {
                    if (entry.getType() == StoreProto.LogType.ENTRY_TYPE_DATA) {
                        raftNode.getStateMachine().apply(entry.getData().toByteArray());
                    } else if (entry.getType() == StoreProto.LogType.ENTRY_TYPE_CONFIGURATION) {
                        raftNode.applyConfiguration(entry);
                    }
                }
                raftNode.setLastAppliedIndex(index);
            }
        }
    }
}
