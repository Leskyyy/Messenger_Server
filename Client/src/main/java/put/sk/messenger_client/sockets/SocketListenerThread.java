package put.sk.messenger_client.sockets;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import javafx.beans.property.SimpleStringProperty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class SocketListenerThread implements Runnable{
    private boolean running;
    private final SocketService socketService;
    private final SimpleStringProperty lastMessage;

    public SocketListenerThread(final SocketService socketService, final SimpleStringProperty lastMessage) {
        this.socketService = socketService;
        this.lastMessage = lastMessage;
        this.running = false;
    }

    public void start() {
        this.running = true;
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        final Socket socket = socketService.getSocket();
        final InputStream is;
        try {
            is = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();

            return;
        }

        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        while (running) {
            try {
                while (reader.ready()) {
                    byte[] buffer = new byte[500];
                    int len = is.read(buffer);
                    String msg = new String(Arrays.copyOfRange(buffer, 0, len - 1), StandardCharsets.UTF_8);
                    this.lastMessage.set(msg);
                }
            } catch (Exception e) {
                System.out.println("Failed socket listen");
            }
        }

        try {
            reader.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
