package Common.messages;

public class DeleteMessage extends Message {
    public DeleteMessage(Version version, int senderId, String fileId) {
        super(MessageType.DELETE);
        this.setVersion(version);
        this.setSenderId(senderId);
        this.setFileId(fileId);
    }

    public byte[] getBytes() {
        return getHeader().getBytes();
    }
}
