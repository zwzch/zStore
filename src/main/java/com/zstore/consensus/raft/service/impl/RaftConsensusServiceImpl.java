package com.zstore.consensus.raft.service.impl;

import com.googlecode.protobuf.format.JsonFormat;
import com.zstore.consensus.raft.RaftNode;
import com.zstore.consensus.raft.constants.CommonConstants;
import com.zstore.consensus.raft.proto.StoreProto;
import com.zstore.consensus.raft.service.RaftConsensusService;
import com.zstore.consensus.raft.utils.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
}
