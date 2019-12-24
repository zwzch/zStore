import com.zstore.api.utils.CommonUtils;
import com.zstore.storge.FileData;

import java.io.File;

public class FileTest {
    public static void main(String[] args) {
        File dataFile = new File("data.dat");
        CommonUtils.createFile(dataFile);
        File inxFile = new File("data.inx");
        CommonUtils.createFile(inxFile);
        FileData fileData = new FileData("test", dataFile.length(), dataFile, inxFile);
    }
}
