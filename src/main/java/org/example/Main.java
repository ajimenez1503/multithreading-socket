package org.example;

import org.example.server.Server;

public class Main {
    public static void main(String[] args) {
        Server server = null;
        server = new Server();
        server.start();
    }
}