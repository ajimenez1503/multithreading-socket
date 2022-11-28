package org.example.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Server {
    private static final int PORT = 4000;
    private static final int MAX_CLIENTS = 5;
    private BlockingQueue<Integer> blockingQueue;
    private ExecutorService executorService;
    private ServerSocket serverSocket;
    private LoggingThread loggingThread;
    private boolean continueLoop;

    public Server() {
        continueLoop = true;
        blockingQueue = new LinkedBlockingQueue<>();
        // Number of thread: MAX_CLIENTS + 1 (for logging into the file)
        executorService = Executors.newFixedThreadPool(MAX_CLIENTS + 1);
        loggingThread = new LoggingThread(blockingQueue);
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        executorService.submit(loggingThread);

        while (continueLoop) {
            SocketHandler socketHandler;
            try {
                socketHandler = new SocketHandler(serverSocket.accept(), blockingQueue, this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            executorService.submit(socketHandler);
        }
    }

    public void shutdown() {
        blockingQueue.clear();
        loggingThread.close();
        executorService.shutdown();
        System.exit(0);
    }
}
