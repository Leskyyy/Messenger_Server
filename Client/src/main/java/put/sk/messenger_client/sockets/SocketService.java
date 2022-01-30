package put.sk.messenger_client.sockets;

import java.io.IOException;
import java.net.Socket;

public class SocketService {
    private final String address;
    private final int port;
    private Socket socket;

    public SocketService(final String address, final int port){
        this.address = address;
        this.port = port;
    }

    public void init() throws IOException {
        this.socket = new Socket(address, port);
    }

    public Socket getSocket(){
        return socket;
    }
}
