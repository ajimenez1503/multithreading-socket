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
    static Logger log = Logger.getLogger(Server.class.getName());
    private static final int PORT = 4000;
    private static final int MAX_CLIENTS = 5;
    private BlockingQueue<Integer> blockingQueue;
    private ExecutorService executorService;
    private ServerSocket serverSocket;
    private LoggingThread loggingThread;
    private volatile boolean continueLoop;
    @Getter
    private boolean isShutDown;

    /**
     * Constructor:
     * - Create a blocking queue to share between the threads.
     * - Create a pool of threads for the LOGGING_THREAD and the SOCKET_THREAD
     * - Open a ServerSocket
     */
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

    /**
     * - Start the server
     * - Run the LOGGING_THREAD.
     * - Listen for connection in the socket and run the SOCKET_THREAD
     * - Stop when the server is shutdown
     */
    public void start() {
        executorService.execute(loggingThread);

        while (continueLoop) {
            SocketThread socketHandler;
            try {
                socketHandler = new SocketThread(serverSocket.accept(), blockingQueue, this);
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

    /**
     * - Shutdown the server
     * - Stop the LOGGING_THREAD.
     * - Stop all the thread pool.
     * - Close the socket
     */
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
