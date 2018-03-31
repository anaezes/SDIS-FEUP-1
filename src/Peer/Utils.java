package Peer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Utils {
    private static final ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5);
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

        for(int i = 0; i < buffer.length; i++) {
            System.arraycopy(fileContent, chunksize*i, buffer[i], 0,
                    i < buffer.length -1 ? chunksize : fileContent.length % chunksize);
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

    /**
     * Removes all null bytes in the end of a byte array
     * @param bytes the array to be trimmed
     * @return byte[] trimmed array
     */
    public static byte[] trim(byte[] bytes)
    {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0)
        {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }

    public static void scheduleAction(Runnable run, long timeMs) {
        scheduler.schedule(run, timeMs, TimeUnit.MILLISECONDS);
    }
}
