package Peer;

import java.io.Serializable;
import java.util.HashSet;

public class ChunkMetadata implements Serializable {
    private HashSet<Integer> peerIds;
    private String fileId;
    private int chunkNo;
    private int repDeg;
    private int chunkSize;

    public ChunkMetadata(String fileId, int chunkNo, int repDeg, int chunkSize) {
        this.peerIds = new HashSet<>();
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.repDeg = repDeg;
        this.chunkSize = chunkSize;
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

    @Override
    public String toString() {
        String text = fileId + "\n" ;
        text += "FileId: " + fileId + "\n" ;
        text += "Replication degree: " + Integer.toString(repDeg) + "\n" ;
        text += "ChunkNo: " + Integer.toString(chunkNo) + "\n" ;
        text += "ChunkSize: " + Double.toString(chunkSize/1000.0) + "KB\n" ;
        return text;
    }
}
