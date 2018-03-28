package Peer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    public static boolean deleteFile(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteFile(children[i]);
                if (!success) {
                    return false;
                }
            }
        }

        return file.delete();
    }

    /**
     * Split file in array of chunks
     * @param fileContent
     * @return array of byte array with all chunks of fileContent
     */
    public static byte[][] getFileChunks(byte[] fileContent, int chunksize) {
        byte buffer[][] = new byte[fileContent.length/chunksize+1][chunksize];

        int initialPos = 0;
        int lastPos = chunksize;

        for(int i = 0; i < buffer.length; i++) {
            buffer[i] = getChunk(fileContent, initialPos, lastPos, chunksize);
            initialPos += chunksize;
            lastPos += chunksize;
        }

        return buffer;
    }

    /**
     * Get chunk from a given position
     * @param fileContent
     * @param initPos
     * @param lastPos
     * @return byte array with chunk
     */
    public static byte[] getChunk(byte[] fileContent, int initPos, int lastPos, int chunksize) {
        byte[] chunk = new byte[chunksize];

        int i = 0;
        while(initPos < lastPos && initPos < fileContent.length ){
            chunk[i] = fileContent[initPos];
            i++; initPos++;
        }
        return chunk;
    }

    /**
     * Get file id encoded
     * @param text
     * @return text encoded
     */
    public static String getEncodeHash(String text)  {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder(hash.length);
        for(byte b : hash)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
