package Common.messages;

public class StoredMessage extends Message {
    public StoredMessage(Version version, int senderId, byte[] fileId, int chunkNo) {
        super(MessageType.STORED);
        this.setVersion(version);
        this.setSenderId(senderId);
        this.setFileId(fileId);
        this.setChunkNo(chunkNo);
    }
}
