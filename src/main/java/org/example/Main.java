package org.example;

import org.example.server.Server;

public class Main {
    public static void main(String[] args) {
        Server server = new Server();
        System.out.println("Starting server");
        server.start();
    }
}