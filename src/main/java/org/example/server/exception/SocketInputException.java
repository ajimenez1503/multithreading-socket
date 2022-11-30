package org.example.server.exception;

public class SocketInputException extends Exception {
    public SocketInputException(String input, String errorMessage) {
        super("Input '" + input + "' " + errorMessage);
    }
}
