package org.example.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class SocketHandler implements Runnable {
    private static final String TERMINATE = "terminate";
    private final Server server;
    BufferedReader in;
    private BlockingQueue<Integer> blockingQueue;
    private Socket socket;

    public SocketHandler(Socket socket, BlockingQueue<Integer> blockingQueue, Server server) {
        this.socket = socket;
        this.blockingQueue = blockingQueue;
        this.server = server;

        try {
            in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        String inputString;
        int inputNumber;

        while (true) {
            try {
                inputString = in.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (inputString == null) {
                break;
            }
            if (inputString.equals(TERMINATE)) {
                server.shutdown();
                break;
            }
            if (inputString.length() != 9) {
                break;
            }
            inputNumber = Integer.parseInt(inputString);
            System.out.println("Read number " + inputNumber);
            if (inputNumber < 0) {
                break;
            }
            blockingQueue.add(inputNumber);
        }
        shutdown();
    }

    private void shutdown() {
        try {
            in.close();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
