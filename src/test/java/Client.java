import lombok.Getter;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private static final int PORT = 4000;
    private static final String IP_LOCAL_HOST = "localhost";
    private static final String TERMINATE_COMMAND = "terminate";

    @Getter
    private Socket socket;
    private PrintWriter out;

    public Client() {
        this(IP_LOCAL_HOST, PORT);
    }

    public Client(String ip, int port) {
        try {
            socket = new Socket(IP_LOCAL_HOST, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(String msg) {
        out.println(msg);
    }

    public void sendTerminateCommand() {
        send(TERMINATE_COMMAND);
    }

    public void sendNumber(int number) {
        send(String.format("%09d", number));
    }

    public void shutdown() {
        out.close();
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
