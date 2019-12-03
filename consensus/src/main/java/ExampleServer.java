import com.baidu.brpc.server.RpcServer;
import com.zstore.consensus.raft.RaftNode;
import com.zstore.consensus.raft.constants.RaftOptions;
import com.zstore.consensus.raft.proto.StoreProto;
import com.zstore.consensus.raft.service.RaftConsensusService;
import com.zstore.consensus.raft.service.impl.RaftConsensusServiceImpl;
import com.zstore.consensus.raft.service.impl.RaftStateMachineRocksImpl;

import java.util.ArrayList;
import java.util.List;

public class ExampleServer {
    public static void main(String[] args) {
//        String sequence = args[0];
        String datadir = "./data";
        String serverStr = "127.0.0.1:8051:1,127.0.0.1:8052:2";
        String localServerStr1 = "127.0.0.1:8051:1";
        String localServerStr2 = "127.0.0.1:8052:2";
        String localServerStr3 = "127.0.0.1:8053:3";
//        ArrayList<String> list = new ArrayList<>();
//        list.add(localServerStr1);
//        list.add(localServerStr2);
//        String localServerStr = list.get(Integer.valueOf(sequence));
        String localServerStr = localServerStr2;
        String[] arr = localServerStr.split(":");
        datadir += arr[arr.length-1];
        String[] splitArray = serverStr.split(",");
        List<StoreProto.Server> serverList = new ArrayList<>();
        for (String serverString : splitArray) {
            StoreProto.Server server = parseServer(serverString);
            serverList.add(server);
        }
        // local server
        StoreProto.Server localServer = parseServer(localServerStr);
        RpcServer server = new RpcServer(localServer.getEndpoint().getPort());
        RaftStateMachineRocksImpl raftStateMachineRocks = new RaftStateMachineRocksImpl(datadir);
        RaftOptions raftOptions = new RaftOptions();
        raftOptions.setDataDir(datadir);
        raftOptions.setSnapshotMinLogSize(10 * 1024);
        raftOptions.setSnapshotPeriodSeconds(30);
        raftOptions.setMaxSegmentFileSize(1024 * 1024);
        RaftNode raftNode = new RaftNode(raftOptions, serverList, localServer, raftStateMachineRocks);
        RaftConsensusService raftConsensusService = new RaftConsensusServiceImpl(raftNode);
        server.registerService(raftConsensusService);
        server.start();
        raftNode.init();
    }

    private static StoreProto.Server parseServer(String serverString) {
        String[] splitServer = serverString.split(":");
        String host = splitServer[0];
        Integer port = Integer.parseInt(splitServer[1]);
        Integer serverId = Integer.parseInt(splitServer[2]);
        StoreProto.Endpoint endPoint = StoreProto.Endpoint.newBuilder()
                .setHost(host).setPort(port).build();
        StoreProto.Server.Builder serverBuilder = StoreProto.Server.newBuilder();
        StoreProto.Server server = serverBuilder.setServerId(serverId).setEndpoint(endPoint).build();
        return server;
    }

}
