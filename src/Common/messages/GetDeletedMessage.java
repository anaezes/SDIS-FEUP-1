package Common.messages;

public class GetDeletedMessage extends Message{
    public GetDeletedMessage(Version version, int senderId, String fileId) {
        super(MessageType.GET_DELETED);
        this.setVersion(version);
        this.setSenderId(senderId);
        this.setFileId(fileId);
    }

    @Override
    public byte[] getBytes() {
        return getHeader().getBytes();
    }
}
