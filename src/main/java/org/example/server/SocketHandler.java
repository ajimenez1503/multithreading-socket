package org.example.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class SocketHandler implements Runnable {
    private static final String TERMINATE_COMMAND = "terminate";
    private static final int EXPECTED_INPUT_LENGTH = 9;
    private final Server server;
    BufferedReader in;
    private volatile boolean shutdown;
    private BlockingQueue<Integer> blockingQueue;
    private Socket socket;

    public SocketHandler(Socket socket, BlockingQueue<Integer> blockingQueue, Server server) {
        shutdown = false;
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

        while (!shutdown) {
            try {
                inputString = in.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (inputString == null) {
                continue;
            }
            if (inputString.equals(TERMINATE_COMMAND)) {
                System.out.println("INFO: '" + TERMINATE_COMMAND + "' has been requested");
                server.shutdown();
                break;
            }
            if (inputString.length() != EXPECTED_INPUT_LENGTH) {
                System.out.println("ERROR: Input '" + inputString + "' has a length different than " + EXPECTED_INPUT_LENGTH);
                break;
            }
            inputNumber = Integer.parseInt(inputString);
            if (inputNumber < 0) {
                System.out.println("ERROR: Input '" + inputString + "' could not be parsed into string");
                break;
            }
            // System.out.println("Add number to blockingQueue " + inputNumber);
            blockingQueue.add(inputNumber);
        }
        shutdown();
    }

    private void shutdown() {
        shutdown = true;
        try {
            in.close();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
