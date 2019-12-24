package com.zstore.storge;
import com.zstore.api.storge.MetaStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zstore.api.storge.Message;
import com.zstore.api.storge.Store;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
public class MetaStorge implements MetaStore {
    public static final Logger log = LoggerFactory.getLogger(MetaStorge.class);
    /**
     * ROCKS JNI LOAD
     * */
    static {
        RocksDB.loadLibrary();
    }

    private RocksDB db;
    private String metaDataDir;
    private boolean isBackUp;

    public MetaStorge(String metaDataDir, boolean isBackUp) {
        this.metaDataDir = metaDataDir;
        this.isBackUp = isBackUp;
    }

    @Override
    public void start() throws RocksDBException {
        try {
            Options options = new Options();
            options.setCreateIfMissing(true);
            db = RocksDB.open(options, this.metaDataDir);
        } catch (RocksDBException exception) {
            throw exception;
        }
    }

    @Override
    public void put(byte[] key, byte[] value) throws RocksDBException {
        db.put(key, value);
    }

    @Override
    public byte[] get(byte[] key) throws RocksDBException {
        return db.get(key);
    }

    @Override
    public void delete(byte[] key) throws RocksDBException {
        db.delete(key);
    }

    @Override
    public boolean exist(byte[] key) {
       return false;
    }


}
