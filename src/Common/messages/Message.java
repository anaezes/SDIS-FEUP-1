package Common.messages;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.util.Arrays;

public abstract class Message implements Serializable {
    /*
     * This is the type of the message. Each subprotocol specifies its own message types. This field determines the
     * format of the message and what actions its receivers should perform.
     * This is encoded as a variable length sequence of ASCII characters.
     */
    private MessageType messageType;

    /*
     * This is the version of the protocol. It is a three ASCII char sequence with the format <n>'.'<m>,
     * where <n> and <m> are the ASCII codes of digits. For example, version 1.0, the one specified in this document,
     * should be encoded as the char sequence '1''.''0'.
     */
    private Version version;

    /*
     * This is the id of the server that has sent the message. This field is useful in many subprotocols.
     * This is encoded as a variable length sequence of ASCII digits.
     */
    private int senderId;

    /*
     * This is the id of the server that has sent the message. This field is useful in many subprotocols.
     * This is encoded as a variable length sequence of ASCII digits.
     */
    private String fileId;

    /*
     * This is the file identifier for the backup service. As stated above, it is supposed to be obtained by using the
     * SHA256 cryptographic hash function. As its name indicates its length is 256 bit, i.e. 32 bytes,
      * and should be encoded as a 64 ASCII character sequence. The encoding is as follows:
      * each byte of the hash value is encoded by the two ASCII characters corresponding to the
      * hexadecimal representation of that byte. E.g., a byte with value 0xB2 should be represented by the
      * two char sequence 'B''2' (or 'b''2', it does not matter).
      * The entire hash is represented in big-endian order, i.e. from the MSB (byte 31) to the LSB (byte 0).
     */
    private int chunkNo;

    /*
     * This field together with the FileId specifies a chunk in the file.
     * The chunk numbers are integers and should be assigned sequentially starting at 0.
     * It is encoded as a sequence of ASCII characters corresponding to the decimal representation of that number,
     * with the most significant digit first. The length of this field is variable, but should not be larger than 6 chars.
     * Therefore, each file can have at most one million chunks. Given that each chunk is 64 KByte,
     * this limits the size of the files to backup to 64 GByte.
     */
    private int replicationDeg;

    private byte[] body;

    /*
     * Constructor
     */
    public Message(MessageType messageType) {
        //this.fileId = new byte[32];
        setMessageType(messageType);
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public void setChunkNo(int chunkNo) {
        this.chunkNo = chunkNo;
    }

    public int getReplicationDeg() {
        return replicationDeg;
    }

    public void setReplicationDeg(int replicationDeg) {
        this.replicationDeg = replicationDeg;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public String getHeader() {
        return this.messageType.toString() + " " +
                this.version.toString() + " " +
                this.senderId + " " +
                this.fileId + " " +
                this.chunkNo + " " +
                this.replicationDeg + " " +
                "\r\n\r\n";
    }

    public abstract byte[] getBytes();

    public enum MessageType {
        PUTCHUNK("PUTCHUNK"),
        STORED("STORED"),
        GETCHUNK("GETCHUNK"),
        CHUNK("CHUNK"),
        DELETE("DELETE"),
        REMOVED("REMOVED"),
        GET_DELETED("GET_DELETED");

        private final String type;

        MessageType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }

    public static Message parseMessage(DatagramPacket packet) {
        Message msg = null;

        try {
            byte[] data = new byte[packet.getLength()];
            System.arraycopy(packet.getData(), 0, data, 0, data.length);

            String value = new String(data, "UTF-8");
            String[] parameters = value.split(" ");
            String[] headerBody = value.split("\r\n\r\n", 2);
            String[] version = parameters[1].split("\\.");

            int headerSize = headerBody[0].getBytes().length + new String("\r\n\r\n").getBytes().length;
            byte chunk[];

            switch (parameters[0]) {
                case "PUTCHUNK":
                    chunk = Arrays.copyOfRange(data,headerSize,data.length);
                    msg = new PutChunkMessage(new Version(Integer.parseInt(version[0]), Integer.parseInt(version[1]))
                            , Integer.parseInt(parameters[2]), parameters[3], Integer.parseInt(parameters[4]),
                            Integer.parseInt(parameters[5]), chunk);
                    break;
                case "STORED":
                    msg = new StoredMessage(new Version(Integer.parseInt(version[0]), Integer.parseInt(version[1])),
                            Integer.parseInt(parameters[2]), parameters[3], Integer.parseInt(parameters[4]));
                    break;
                case "DELETE":
                    msg = new DeleteMessage(new Version(Integer.parseInt(version[0]), Integer.parseInt(version[1])),
                            Integer.parseInt(parameters[2]), parameters[3]);
                    break;
                case "CHUNK":
                    chunk = Arrays.copyOfRange(data,headerSize,data.length);
                    msg = new ChunkMessage(new Version(Integer.parseInt(version[0]), Integer.parseInt(version[1]))
                            , Integer.parseInt(parameters[2]), parameters[3], Integer.parseInt(parameters[4]), chunk);
                    break;
                case "GETCHUNK":
                    msg = new GetChunkMessage(new Version(Integer.parseInt(version[0]), Integer.parseInt(version[1])),
                            Integer.parseInt(parameters[2]), parameters[3], Integer.parseInt(parameters[4]));
                    break;
                case "REMOVED":
                    msg = new RemovedMessage(new Version(Integer.parseInt(version[0]), Integer.parseInt(version[1])),
                            Integer.parseInt(parameters[2]), parameters[3], Integer.parseInt(parameters[4]));
                    break;
                case "GET_DELETED":
                    msg = new GetDeletedMessage(new Version(Integer.parseInt(version[0]), Integer.parseInt(version[1])),
                            Integer.parseInt(parameters[2]), parameters[3]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return msg;
    }

    /**
     * Concatenates the fileId and ChunkNo
     * @return the chunk unique identifier
     */
    public String getChunkUID() {
        return fileId + chunkNo;
    }
}


