package Peer;

import java.io.Serializable;
import java.util.HashSet;

public class ChunkMetadata implements Serializable {
    private HashSet<Integer> peerIds;
    private String fileId;
    private int chunkNo;
    private int repDeg;

    public ChunkMetadata(String fileId, int chunkNo, int repDeg) {
        this.peerIds = new HashSet<>();
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.repDeg = repDeg;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public void setChunkNo(int chunkNo) {
        this.chunkNo = chunkNo;
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

    public HashSet<Integer> getPeerIds() {
        return peerIds;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ChunkMetadata) {
            ChunkMetadata o = (ChunkMetadata) obj;
            return o.chunkNo == chunkNo && o.fileId == fileId && o.getRepDeg() == repDeg;
        }
        return false;
    }
}
