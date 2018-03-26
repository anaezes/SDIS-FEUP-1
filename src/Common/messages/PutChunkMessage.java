package Common.messages;

public class PutChunkMessage extends Message {
    public PutChunkMessage(Version version, int senderId, String fileId, int chunkNo, int replicationDeg, byte[] body) {
        super(MessageType.PUTCHUNK);
        this.setVersion(version);
        this.setSenderId(senderId);
        this.setFileId(fileId);
        this.setChunkNo(chunkNo);
        this.setReplicationDeg(replicationDeg);
        this.setBody(body);
    }

    public byte[] getBytes() {
        byte[] header = getHeader().getBytes();
        byte[] message = new byte[header.length + this.getBody().length];
        System.arraycopy(header, 0, message, 0, header.length);
        System.arraycopy(this.getBody(), 0, message, header.length, this.getBody().length);
        return message;
    }
}
