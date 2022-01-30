package put.sk.messenger_client.protocols;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MessageService {
    public MessageService() {

    }

    public String encode(Message message) throws IOException {

        ByteArrayOutputStream outputMessage = new ByteArrayOutputStream();
        String sender = message.getSender();
        String receiver = message.getReceiver();
        String content = message.getContent();
        int totalLen = message.getTotalLen();

        String[] information = {sender, receiver, content};

        byte[] chunkLength = intToByteArray(totalLen);
        outputMessage.write(chunkLength);

        for (String info : information) {
            chunkLength = intToByteArray(info.length());
            byte[] chunkContent = info.getBytes();
            outputMessage.write(chunkLength);
            outputMessage.write(chunkContent);
        }
        return outputMessage.toString();
    }

    public Message decode(byte[] encodedMessage) {
        System.out.println("EM: " + Arrays.toString(encodedMessage));
        int senderLength, receiverLength, contentLength, totalLen;
        totalLen = byteArrayToInt(Arrays.copyOfRange(encodedMessage, 0, 4));
        System.out.println("Total len: " + totalLen);
        senderLength = byteArrayToInt(Arrays.copyOfRange(encodedMessage, 4, 8));
        receiverLength = byteArrayToInt(Arrays.copyOfRange(encodedMessage, 8+senderLength, 12+senderLength));
        contentLength = byteArrayToInt(Arrays.copyOfRange(encodedMessage, 12+senderLength+receiverLength,
                16+senderLength+receiverLength));

        String sender = new String(Arrays.copyOfRange(encodedMessage, 8, 8+senderLength),
                StandardCharsets.UTF_8);
        String receiver = new String(Arrays.copyOfRange(encodedMessage, 12+senderLength,
                12+senderLength+receiverLength), StandardCharsets.UTF_8);
        String content = new String(Arrays.copyOfRange(encodedMessage, 16+senderLength+receiverLength,
                16+senderLength+receiverLength+contentLength), StandardCharsets.UTF_8);
        return new Message(sender, receiver, content);
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