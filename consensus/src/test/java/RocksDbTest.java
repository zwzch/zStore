import com.zstore.consensus.raft.proto.ExampleProto;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class RocksDbTest {

    static {
        RocksDB.loadLibrary();
    }

    public static void main(String[] args) throws RocksDBException {
        RocksDB db;
        String dataDir = "./rocksData";
        Options options = new Options();
        options.setCreateIfMissing(true);
        db = RocksDB.open(options, dataDir);
        byte[] key = "hello".getBytes();
        byte[] value = "world".getBytes();
        db.put(key, value);
        System.out.println(new String(db.get(key)));

    }
}
