package Peer;

import java.util.Queue;

public class FileData {
    private String fileId;
    private int repDeg;
    private Queue<ChunkData> chunkData;

    public FileData(String fileId, int repDeg, Queue<ChunkData> chunkData) {
        this.fileId = fileId;
        this.repDeg = repDeg;
        this.chunkData = chunkData;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public int getRepDeg() {
        return repDeg;
    }

    public void setRepDeg(int repDeg) {
        this.repDeg = repDeg;
    }

    public Queue<ChunkData> getChunkData() {
        return chunkData;
    }

    public void setChunkData(Queue<ChunkData> chunkData) {
        this.chunkData = chunkData;
    }
}
