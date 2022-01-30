package put.sk.messenger_client.sockets;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Queue;

public class SocketSenderThread implements Runnable{
    private boolean running;
    private final SocketService socketService;
    private final Queue<String> messagesToSend;

    public SocketSenderThread(final SocketService socketService, final Queue<String> messagesToSend) {
        this.socketService = socketService;
        this.messagesToSend = messagesToSend;
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
        final OutputStream os;
        try {
            os = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();

            return;
        }

        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));

        while (running) {
            try {
                String msg = null;

                do {
                    msg = messagesToSend.poll();

                    if (msg != null) {
                        writer.write(msg);
                        writer.flush();
                    }
                } while (msg != null);
            } catch (Exception e) {
                System.out.println("Failed socket listen");
            }
        }

        try {
            writer.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}