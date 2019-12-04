package com.zstore.consensus.raft.utils;

import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.CRC32;

public class StoreFileUtils {
    private static final Logger LOG = LoggerFactory.getLogger(StoreFileUtils.class);
    public static RandomAccessFile openFile(String dir, String fileName, String mode) {
        try {
            String fullFileName = dir + File.separator + fileName;
            File file = new File(fullFileName);
            return new RandomAccessFile(file, mode);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("file not found, file=" + fileName);
        }
    }

    public static  <T extends Message> void writeProtoToFile(RandomAccessFile raf, T message) {
        byte[] messageBytes = message.toByteArray();
        long crc32 = getCRC32(messageBytes);
        try {
            raf.writeLong(crc32);
            raf.writeInt(messageBytes.length);
            raf.write(messageBytes);
        } catch (IOException ex) {
            throw new RuntimeException("write proto to file error");
        }
    }

    public static <T extends Message> T readProtoFromFile(RandomAccessFile raf, Class<T> clazz) {
        try {
            long crc32FromFile = raf.readLong();
            int dataLen = raf.readInt();
            int hasReadLen = (Long.SIZE + Integer.SIZE) / Byte.SIZE;
            if (raf.length() - hasReadLen < dataLen) {
                LOG.warn("file remainLength < dataLen");
                return null;
            }
            byte[] data = new byte[dataLen];
            int readLen = raf.read(data);
            if (readLen != dataLen) {
                LOG.warn("readLen != dataLen");
                return null;
            }
            long crc32FromData = getCRC32(data);
            if (crc32FromFile != crc32FromData) {
                LOG.warn("crc32 check failed");
                return null;
            }
            Method  method = clazz.getMethod("parseFrom", byte[].class);
            T message = (T) method.invoke(clazz, data);
            return message;
        } catch (Exception ex) {
            LOG.warn("readProtoFromFile meet exception, {}", ex.getMessage());
            return null;
        }
    }
    public static long getCRC32(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }
    public static List<String> getSortedFilesInDirectory(
            String dirName, String rootDirName) throws IOException {
        List<String> fileList = new ArrayList<>();
        File rootDir = new File(rootDirName);
        File dir = new File(dirName);
        if (!rootDir.isDirectory() || !dir.isDirectory()) {
            return fileList;
        }
        String rootPath = rootDir.getCanonicalPath();
        if (!rootPath.endsWith("/")) {
            rootPath = rootPath + "/";
        }
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                fileList.addAll(getSortedFilesInDirectory(file.getCanonicalPath(), rootPath));
            } else {
                fileList.add(file.getCanonicalPath().substring(rootPath.length()));
            }
        }
        Collections.sort(fileList);
        return fileList;
    }

    public static void closeFile(RandomAccessFile randomAccessFile) {
        try {
            if (randomAccessFile != null) {
                randomAccessFile.close();
            }
        } catch (IOException ex) {
            LOG.warn("close file error, msg={}", ex.getMessage());
        }
    }

    public static void mkdir(String dir) {
        File file = new File(dir);
        if (!file.exists()) {
            file.mkdirs();
        }
    }


    public static void createFile(File file) {
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            LOG.error("create file error msg={}", e.getMessage());
            throw new RuntimeException("create file error");
        }
    }
}
