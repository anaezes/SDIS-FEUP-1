package Common.messages;

public class PutChunkMessage extends Message {
    public PutChunkMessage(Version version, int senderId, byte[] fileId, int chunkNo, int replicationDeg, byte[] body) {
        super(MessageType.PUTCHUNK);
        this.setVersion(version);
        this.setSenderId(senderId);
        this.setFileId(fileId);
        this.setChunkNo(chunkNo);
        this.setReplicationDeg(replicationDeg);
        this.setBody(body);
    }
}
