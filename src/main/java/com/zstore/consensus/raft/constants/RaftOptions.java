package com.zstore.consensus.raft.constants;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RaftOptions {

    /**
     * 如果没有收到任何讯息，则追随者将成为候选人
     * 来自选举超时领导者的毫秒数
     * */
    private int electionTimeoutMilliseconds = 5000;

    /**
     * 领导者至少经常发送RPC，即使没有数据要发送
     * */
    private int heartbeatPeriodMilliseconds = 500;

    /**
     * snapshot定时器执行间隔
     */
    private int snapshotPeriodSeconds = 3600;

    /**
     *  log entry大小达到snapshotMinLogSize，才做snapshot
     * */
    private int snapshotMinLogSize = 100 * 1024 * 1024;

    private int maxSnapshotBytesPerRequest = 500 * 1024;

    private int maxLogEntriesPerRequest = 5000;

    private int maxSegmentFileSize = 100 * 1000 * 1000;

    private long catchupMargin = 500;

    /**
     * replicate最大等待超时时间，单位ms
     */
    private long maxAwaitTimeout = 1000;

    /**
     *  与其他节点进行同步、选主等操作的线程池大小
     */
    private int raftConsensusThreadNum = 20;

    /**
     *  是否异步写数据；true表示主节点保存后就返回，然后异步同步给从节点；
     *  false表示主节点同步给大多数从节点后才返回。
     */
    private boolean asyncWrite = false;

    /**
     * raft的log和snapshot父目录，绝对路径
     * */
    private String dataDir = System.getProperty("com.github.wenweihu86.raft.data.dir");

}
