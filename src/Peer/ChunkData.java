package Peer;

import java.io.Serializable;

public class ChunkData implements Serializable {
    private final int chunkNo;
    private final byte[] chunkData;

    public ChunkData(int chunkNo, byte[] chunkData) {
        this.chunkNo = chunkNo;
        this.chunkData = chunkData;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public byte[] getChunkData() {
        return chunkData;
    }
}
