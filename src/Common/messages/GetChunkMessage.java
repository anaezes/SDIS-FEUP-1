package Common.messages;

public class GetChunkMessage extends Message {
    public GetChunkMessage(Version version, int senderId, String fileId, int chunkNo) {
        super(MessageType.PUTCHUNK);
        this.setVersion(version);
        this.setSenderId(senderId);
        this.setFileId(fileId);
        this.setChunkNo(chunkNo);
    }

    public byte[] getBytes() {
        return getHeader().getBytes();
    }
}

