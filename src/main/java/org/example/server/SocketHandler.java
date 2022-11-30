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

    public SocketHandler(Socket socket, BlockingQueue<Integer> blockingQueue, Server server) throws IOException {
        shutdown = false;
        this.socket = socket;
        this.blockingQueue = blockingQueue;
        this.server = server;

        try {
            in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        } catch (IOException e) {
            System.out.println("ERROR: Could not write in the socket");
            System.out.println(e);
            throw e;
        }
    }

    private boolean isTerminateCommand(String input) {
        if (input.equals(TERMINATE_COMMAND)) {
            System.out.println("INFO: '" + TERMINATE_COMMAND + "' has been requested");
            shutdown();
            server.shutdown();
            return true;
        }
        return false;
    }

    private int getNumber(String input) throws Exception {
        int inputNumber;

        if (input.length() != EXPECTED_INPUT_LENGTH) {
            shutdown();
            throw new Exception("ERROR: Input '" + input + "' has a length different than " + EXPECTED_INPUT_LENGTH);
        }
        try {
            inputNumber = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            shutdown();
            throw new Exception("ERROR: Input '" + input + "' could not be parsed into string");
        }
        if (inputNumber < 0) {
            shutdown();
            throw new Exception("ERROR: Input '" + input + "' is negative");
        }
        return inputNumber;
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
            if (inputString != null) {
                if (isTerminateCommand(inputString)) {
                    return;
                }
                try {
                    inputNumber = getNumber(inputString);
                } catch (Exception e) {
                    return;
                }
                blockingQueue.add(inputNumber);
            }
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
