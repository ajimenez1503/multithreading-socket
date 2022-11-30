package org.example.server;

import org.example.server.exception.SocketInputException;

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

    private int getNumber(String input) throws SocketInputException {
        int inputNumber;

        if (input.length() != EXPECTED_INPUT_LENGTH) {
            shutdown();
            throw new SocketInputException(input, " Length different than " + EXPECTED_INPUT_LENGTH);
        }
        try {
            inputNumber = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            shutdown();
            throw new SocketInputException(input, "Could not be parsed into Integer");
        }
        if (inputNumber < 0) {
            shutdown();
            throw new SocketInputException(input, "Is a negative number '" + inputNumber + "'");
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
                System.out.println("ERROR: could no read from the socket");
                System.out.println(e);
                shutdown();
                return;
            }
            if (inputString != null) {
                if (isTerminateCommand(inputString)) {
                    return;
                }
                try {
                    inputNumber = getNumber(inputString);
                } catch (SocketInputException e) {
                    System.out.println("ERROR: The input '" + inputString + " is not valid");
                    System.out.println(e);
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
            System.out.println("ERROR: Could not close the socket");
            System.out.println(e);
        }
    }
}
