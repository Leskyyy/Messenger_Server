package put.sk.messenger_client.protocols;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class CredentialsService {
    public CredentialsService(){}

    public String encode(Credentials credentials, boolean register) throws IOException {

        ByteArrayOutputStream outputMessage = new ByteArrayOutputStream();
        String login = credentials.getLogin();
        String password = credentials.getPassword();

        if (register){
            outputMessage.write('s');
        }
        else{
            outputMessage.write('l');
        }

        String[] data = {login, password};

        for (String field : data) {
            byte[] chunkLength = intToByteArray(field.length());
            byte[] chunkContent = field.getBytes();
            outputMessage.write(chunkLength);
            outputMessage.write(chunkContent);
        }
        return outputMessage.toString();
    }

    public Credentials decode(byte[] encodedCredentials) {
        int loginLength, passwordLength;
        loginLength = byteArrayToInt(Arrays.copyOfRange(encodedCredentials, 0, 4));
        String login = new String(Arrays.copyOfRange(encodedCredentials, 4, 4+loginLength),
                StandardCharsets.UTF_8);

        passwordLength = byteArrayToInt(Arrays.copyOfRange(encodedCredentials, 4+loginLength, 8+loginLength));
        String password = new String(Arrays.copyOfRange(encodedCredentials, 8+loginLength,
                8+loginLength+passwordLength), StandardCharsets.UTF_8);

        return new Credentials(login, password);
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
