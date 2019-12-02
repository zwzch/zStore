import com.google.protobuf.ByteString;
import com.zstore.consensus.raft.proto.StoreProto;
import com.zstore.consensus.raft.storge.segment.SegmentLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SegmentLogTest {

    public static void main(String[] args) throws IOException {
        String raftDataDir = "./data";
        SegmentLog segmentedLog = new SegmentLog(raftDataDir, 32);
        System.out.println(segmentedLog);
        // write
        System.out.println(segmentedLog.getFirstIndex());
        List<StoreProto.LogEntry> entries = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            StoreProto.LogEntry entry = StoreProto.LogEntry.newBuilder()
                    .setData(ByteString.copyFrom(("testEntryData" + i).getBytes()))
                    .setType(StoreProto.LogType.ENTRY_TYPE_DATA)
                    .setIndex(i)
                    .setTerm(i)
                    .build();
            entries.add(entry);
        }
        long lastLogIndex = segmentedLog.append(entries);
        System.out.println(lastLogIndex);
//        segmentedLog.updateMetaData(1L,null, segmentedLog.getFirstIndex());

        segmentedLog.truncatePrefix(5);
//        FileUtils.deleteDirectory(new File(raftDataDir));
    }
}
