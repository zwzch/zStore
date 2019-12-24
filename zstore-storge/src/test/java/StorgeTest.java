import com.zstore.api.storge.MetaStore;
import com.zstore.storge.MetaStorge;
import org.apache.commons.collections4.map.LRUMap;

public class StorgeTest {
    public static void main(String[] args) throws Exception {
//        MetaStore metaStore = new MetaStorge("data", true);
//        byte[] key = "key".getBytes();
//        byte[] value = "xxx".getBytes();
//        metaStore.start();
//        metaStore.put(key, value);
//        System.out.println(new String(metaStore.get(key)));
//        metaStore.delete(key);
//        System.out.println(new String(metaStore.get(key)));
        LRUMap lruMap = new LRUMap<>(100);
        lruMap.put("xxx","aaa");
        System.out.println(lruMap.get("xxx"));
    }
}
