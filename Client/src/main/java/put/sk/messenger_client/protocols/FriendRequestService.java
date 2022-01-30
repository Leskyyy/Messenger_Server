package put.sk.messenger_client.protocols;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class FriendRequestService {

    public String encode(String name) throws IOException {

        ByteArrayOutputStream outputMessage = new ByteArrayOutputStream();
        byte[] chunkLength = intToByteArray(name.length());
        outputMessage.write('r');
        outputMessage.write(chunkLength);
        outputMessage.write(name.getBytes());
        return outputMessage.toString();
    }

    public String decode(byte[] encodedMessage) {
        System.out.println("EM: " + Arrays.toString(encodedMessage));
        byte[] messageType = Arrays.copyOfRange(encodedMessage, 0, 1);
        int nameLength = byteArrayToInt(Arrays.copyOfRange(encodedMessage, 1, 5));

        String name = new String(Arrays.copyOfRange(encodedMessage, 5, 5+nameLength),
                StandardCharsets.UTF_8);
        return name;
    }

    public byte[] intToByteArray(int value) {
        return new byte[]{
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value};
    }

    public int byteArrayToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8) |
                ((bytes[3] & 0xFF));
    }
}