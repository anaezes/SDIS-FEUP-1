package Common.messages;

public class RemovedMessage extends Message {
    public RemovedMessage(Version version, int senderId, String fileId, int chunkNo) {
        super(MessageType.REMOVED);
        this.setVersion(version);
        this.setSenderId(senderId);
        this.setFileId(fileId);
        this.setChunkNo(chunkNo);
    }

    @Override
    public byte[] getBytes() {
        return getHeader().getBytes();
    }
}
