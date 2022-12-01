package org.example.server;

import org.example.server.exception.SocketInputException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

public class SocketHandler implements Runnable {
    private static final String TERMINATE_COMMAND = "terminate";
    private static final int EXPECTED_INPUT_LENGTH = 9;
    static Logger log = Logger.getLogger(SocketHandler.class.getName());
    private final Server server;
    BufferedReader in;
    private volatile boolean isShutDown;
    private BlockingQueue<Integer> blockingQueue;
    private Socket socket;

    public SocketHandler(Socket socket, BlockingQueue<Integer> blockingQueue, Server server) throws IOException {
        isShutDown = false;
        this.socket = socket;
        this.blockingQueue = blockingQueue;
        this.server = server;

        try {
            in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        } catch (IOException e) {
            log.severe("Could not write in the socket. Exception: " + e);
            throw e;
        }
    }

    private boolean isTerminateCommand(String input) {
        if (input.equals(TERMINATE_COMMAND)) {
            log.info("'" + TERMINATE_COMMAND + "' has been requested");
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

        while (!isShutDown) {
            try {
                inputString = in.readLine();
            } catch (IOException e) {
                log.severe("Could no read from the socket. Exception: " + e);
                break;
            }
            if (inputString != null) {
                if (isTerminateCommand(inputString)) {
                    return;
                }
                try {
                    inputNumber = getNumber(inputString);
                } catch (SocketInputException e) {
                    log.severe("The input '" + inputString + "' is not valid. Exception: " + e);
                    return;
                }
                blockingQueue.add(inputNumber);
            }
        }
        shutdown();
    }

    private void shutdown() {
        isShutDown = true;
        try {
            in.close();
            socket.close();
        } catch (IOException e) {
            log.severe("Could not close the socket. Exception: " + e);
        }
    }
}
