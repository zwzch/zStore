package com.zstore.consensus.raft.storge.segment;

import com.zstore.consensus.raft.proto.StoreProto;
import com.zstore.consensus.raft.utils.StoreFileUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.TreeMap;

public class SegmentLog {
    private static final Logger LOG = LoggerFactory.getLogger(SegmentLog.class);
    private String logDir;
    private String logDataDir;
    private int maxSegmentFileSize;
    private StoreProto.LogMetaData metaData;
    private TreeMap<Long, Segment> startLogIndexSegmentMap = new TreeMap<Long, Segment>();
    // segment log占用的内存大小，用于判断是否需要做snapshot
    private volatile long totalSize;

    public SegmentLog(String storeDataDir, int maxSegmentFileSize) {
        this.logDir = storeDataDir + File.separator + "log";
        this.logDataDir = storeDataDir + File.separator + "data";
        this.maxSegmentFileSize = maxSegmentFileSize;
        StoreFileUtils.mkdir(logDataDir);
        StoreFileUtils.mkdir(logDir);
        readSegments();
        for (Segment segment : startLogIndexSegmentMap.values()) {
            this.loadSegmentData(segment);
        }
        readMetaData();
        metaData = this.readMetaData();
        if (metaData == null) {
            if (startLogIndexSegmentMap.size() > 0) {
                LOG.error("No readable metadata file but found segments in {}", logDir);
                throw new RuntimeException("No readable metadata file but found segments");
            }
            metaData = StoreProto.LogMetaData.newBuilder().setFirstLogIndex(1).build();
        }

    }
    public void loadSegmentData(Segment segment) {
        try {
            RandomAccessFile randomAccessFile = segment.getRandomAccessFile();
            long totalLenth = segment.getFileSize();
            long offset = 0;
            while (offset < totalLenth) {
                StoreProto.LogEntry entry = StoreFileUtils.readProtoFromFile(randomAccessFile, StoreProto.LogEntry.class);
                if (entry == null) {
                    throw new RuntimeException("read segment log failed");
                }
                Record record = new Record(offset, entry);
                segment.getEntries().add(record);
                offset = randomAccessFile.getFilePointer();
            }
            totalSize += totalLenth;
        } catch (Exception ex) {
            LOG.error("read segment meet exception, msg={}", ex.getMessage());
            throw new RuntimeException("file not found");
        }

        int entrySize = segment.getEntries().size();
        if (entrySize > 0) {
            segment.setStartIndex(segment.getEntries().get(0).entry.getIndex());
            segment.setEndIndex(segment.getEntries().get(entrySize - 1).entry.getIndex());
        }

    }
    public void readSegments() {
        try {
            List<String> fileNames = StoreFileUtils.getSortedFilesInDirectory(logDataDir, logDataDir);
            for (String fileName : fileNames) {
                String[] splitArray = fileName.split("-");
                if (splitArray.length != 2) {
                    LOG.warn("segment filename[{}] is not valid", fileName);
                    continue;
                }
                Segment segment = new Segment();
                segment.setFileName(fileName);
                if (splitArray[0].equals("open")) {
                    segment.setCanWrite(true);
                    segment.setStartIndex(Long.valueOf(splitArray[1]));
                    segment.setEndIndex(0);
                } else {
                    try {
                        segment.setCanWrite(false);
                        segment.setStartIndex(Long.parseLong(splitArray[0]));
                        segment.setEndIndex(Long.parseLong(splitArray[1]));
                    } catch (NumberFormatException ex) {
                        LOG.warn("segment filename[{}] is not valid", fileName);
                        continue;
                    }
                }
                segment.setRandomAccessFile(StoreFileUtils.openFile(logDataDir, fileName, "rw"));
                segment.setFileSize(segment.getRandomAccessFile().length());
                startLogIndexSegmentMap.put(segment.getStartIndex(), segment);
            }
        } catch (IOException ioException) {
            LOG.warn("readSegments exception:", ioException);
            throw new RuntimeException("open segment file error");
        }
    }
    public long append(List<StoreProto.LogEntry> entries) {
        long newLastLogIndex = this.getLastLogIndex();
        for (StoreProto.LogEntry entry: entries) {
            newLastLogIndex++;
            int entrySize = entry.getSerializedSize();
            int segmentSize = startLogIndexSegmentMap.size();
            boolean isNeetNewSegmentFile = false;
            try {
                if (segmentSize == 0) {
                    // 当前没有Segment
                    isNeetNewSegmentFile = true;
                } else {
                    Segment segment = startLogIndexSegmentMap.lastEntry().getValue();
                    if (!segment.isCanWrite()) {
                        // segment 不可写
                        isNeetNewSegmentFile = true;
                    } else if (segment.getFileSize() + entrySize >= maxSegmentFileSize){
                        // 超过单个文件最大限制
                        isNeetNewSegmentFile = true;
                        // 最后一个segment的文件close并改名
                        segment.getRandomAccessFile().close();
                        segment.setCanWrite(false);
                        String newFileName = String.format("%020d-%020d",
                                segment.getStartIndex(), segment.getEndIndex());
                        String newFullFileName =  logDataDir + File.separator + newFileName;
                        File newFile = new File(newFullFileName);
                        String oldFullFileName = logDataDir + File.separator + segment.getFileName();
                        File oldFile = new File(oldFullFileName);
                        FileUtils.moveFile(oldFile, newFile);
                        segment.setFileName(newFileName);
                        segment.setRandomAccessFile(StoreFileUtils.openFile(logDataDir, newFileName, "r"));
                    }
                }
                Segment newSegment;
                if (isNeetNewSegmentFile) {
                    String newSegmentFileName = String.format("open-%d", newLastLogIndex);
                    String newFullFileName = logDataDir + File.separator + newSegmentFileName;
                    File newSegmentFile = new File(newFullFileName);
                    if (!newSegmentFile.exists()) {
                        newSegmentFile.createNewFile();
                    }
                    Segment segment = new Segment();
                    segment.setCanWrite(true);
                    segment.setStartIndex(newLastLogIndex);
                    segment.setFileName(newSegmentFileName);
                    segment.setRandomAccessFile(StoreFileUtils.openFile(logDataDir, newSegmentFileName, "rw"));
                    newSegment = segment;
                } else {
                    newSegment = startLogIndexSegmentMap.lastEntry().getValue();
                }
                // proto数据写入segment
                if (entry.getIndex() == 0) {
                    entry = StoreProto.LogEntry.newBuilder(entry)
                            .setIndex(newLastLogIndex).build();
                }
                newSegment.setEndIndex(entry.getIndex());
                newSegment.getEntries().add(new Record(newSegment.getRandomAccessFile().getFilePointer(), entry));
                StoreFileUtils.writeProtoToFile(newSegment.getRandomAccessFile(), entry);
                newSegment.setFileSize(newSegment.getRandomAccessFile().length());
                if (!startLogIndexSegmentMap.containsKey(newSegment.getStartIndex())) {
                    startLogIndexSegmentMap.put(newSegment.getStartIndex(), newSegment);
                }
                totalSize += entrySize;
            } catch (IOException e) {
                throw new RuntimeException("append raft log exception, msg=" + e.getMessage());
            }
        }
        return newLastLogIndex;
    }

    public long getLastLogIndex() {
        // 有两种情况segment为空
        // 1、第一次初始化，firstLogIndex = 1，lastLogIndex = 0
        // 2、snapshot刚完成，日志正好被清理掉，firstLogIndex = snapshotIndex + 1， lastLogIndex = snapshotIndex
        if (startLogIndexSegmentMap.size() == 0) {
            return getFirstIndex();
        }
        Segment lastSegment = startLogIndexSegmentMap.lastEntry().getValue();
        return lastSegment.getEndIndex();
    }

    public StoreProto.LogMetaData readMetaData() {
        String fileName = logDir + File.separator + "metadata";
        File file = new File(fileName);
        StoreFileUtils.createFile(file);
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            StoreProto.LogMetaData metadata = StoreFileUtils.readProtoFromFile(
                    randomAccessFile, StoreProto.LogMetaData.class);
            return metadata;
        } catch (IOException ex) {
            LOG.warn("meta file not exist, name={}", fileName);
            return null;
        }
    }
    public long getFirstIndex() {
        return metaData.getFirstLogIndex();
    }

    public void truncatePrefix(long newFirstIndex) {
        if (newFirstIndex <= getFirstIndex()) {
            return;
        }
        long oldFirstIndex = getFirstIndex();
        while (!startLogIndexSegmentMap.isEmpty()) {
            Segment segment = startLogIndexSegmentMap.firstEntry().getValue();
            if (segment.isCanWrite()) {
                break;
            }
            if (newFirstIndex > segment.getEndIndex()) {
                File oldFile = new File(logDataDir + File.separator + segment.getFileName());
                try {
                    StoreFileUtils.closeFile(segment.getRandomAccessFile());
                    FileUtils.forceDelete(oldFile);
                    totalSize -= segment.getFileSize();
                    startLogIndexSegmentMap.remove(segment.getStartIndex());
                } catch (IOException e) {
                    LOG.warn("delete file exception:", e);
                }
            } else {
                break;
            }
        }
        long newActualFirstIndex;
        if (startLogIndexSegmentMap.size() == 0) {
            newActualFirstIndex = newFirstIndex;
        } else {
            newActualFirstIndex = startLogIndexSegmentMap.firstKey();
        }
        LOG.info("Truncating log from old first index {} to new first index {}",
                oldFirstIndex, newActualFirstIndex);
    }

    public void updateMetaData(Long currentTerm, Integer votedFor, Long firstLogIndex) {
        StoreProto.LogMetaData.Builder builder = StoreProto.LogMetaData.newBuilder(this.metaData);
        if (currentTerm != null) {
            builder.setCurrentTerm(currentTerm);
        }
        if (votedFor != null) {
            builder.setVotedFor(votedFor);
        }
        if (firstLogIndex != null) {
            builder.setFirstLogIndex(firstLogIndex);
        }
        this.metaData = builder.build();
        String fileName = logDir + File.separator + "metadata";
        File file = new File(fileName);
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            StoreFileUtils.writeProtoToFile(randomAccessFile, metaData);
            LOG.info("new segment meta info, currentTerm={}, votedFor={}, firstLogIndex={}",
                    metaData.getCurrentTerm(), metaData.getVotedFor(), metaData.getFirstLogIndex());
        } catch (IOException ex) {
            LOG.warn("meta file not exist, name={}", fileName);
        }
    }

    public StoreProto.LogMetaData getMetaData() {
        return metaData;
    }

    public StoreProto.LogEntry getEntry(long index) {
        long firstLogIndex = getFirstIndex();
        long lastLogIndex = getLastLogIndex();
        if (index == 0 || index < firstLogIndex || firstLogIndex > lastLogIndex) {
            LOG.debug("index out of range, index={}, firstLogIndex={}, lastLogIndex={}",
                    index, firstLogIndex, lastLogIndex);
            return null;
        }
        if (startLogIndexSegmentMap.size() == 0) {
            return null;
        }
        Segment segment = startLogIndexSegmentMap.floorEntry(index).getValue();
        return segment.getEntry(index);
    }

    public long getEntryTerm(long index) {
        StoreProto.LogEntry entry = getEntry(index);
        if (entry == null) {
            return 0;
        }
        return entry.getTerm();
    }
}
