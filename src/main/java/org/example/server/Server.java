package org.example.server;

import lombok.Getter;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Server {
    private static final int PORT = 4000;
    private static final int MAX_CLIENTS = 5;
    static Logger log = Logger.getLogger(Server.class.getName());
    private BlockingQueue<Integer> blockingQueue;
    private ExecutorService executorService;
    private ServerSocket serverSocket;
    private LoggingThread loggingThread;
    private volatile boolean continueLoop;

    @Getter
    private boolean isShutDown;

    public Server() {
        continueLoop = true;
        isShutDown = false;
        blockingQueue = new LinkedBlockingQueue<>();
        // Number of thread: MAX_CLIENTS + 1 (for logging into the file)
        executorService = Executors.newFixedThreadPool(MAX_CLIENTS + 1);
        loggingThread = new LoggingThread(blockingQueue);
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            log.severe("Could not create a socket in the port '" + PORT + "'. Exception: " + e);
            throw new RuntimeException(e);
        }
    }

    public void start() {
        executorService.execute(loggingThread);

        while (continueLoop) {
            SocketHandler socketHandler;
            try {
                socketHandler = new SocketHandler(serverSocket.accept(), blockingQueue, this);
            } catch (IOException e) {
                // If 'isShutDown' socket is already closed.
                if (continueLoop && !isShutDown) {
                    log.severe("Could not manage the socket. Exception: " + e);
                }
                return;
            }
            executorService.execute(socketHandler);
        }
    }

    public Statistic getStatistic() {
        return loggingThread.getStatistic();
    }

    public void shutdown() {
        continueLoop = false;

        loggingThread.shutdown();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warning("Thread '" + Thread.currentThread().getName() + "' was interrupted.");
            Thread.currentThread().interrupt();
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            log.severe("Could not close the socket. Exception: " + e);
        }

        isShutDown = true;
    }
}
