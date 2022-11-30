package org.example.server;

import lombok.Getter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;

public class LoggingThread implements Runnable {

    private static final int WAIT = 10 * 100;
    private static final String FILE_NAME = "/tmp/numbers.log";
    private final Object lock;

    private volatile boolean runFunctionHasFinished;

    private BitSet bitSet;
    private BlockingQueue<Integer> blockingQueue;

    @Getter
    private Statistic statistic;
    private Timer timer;
    private FileWriter file;
    private BufferedWriter out;

    //  currentThread will be initialized in the run() function
    private Thread currentThread;

    public LoggingThread(BlockingQueue<Integer> blockingQueue) {
        lock = new Object();
        runFunctionHasFinished = false;
        bitSet = new BitSet(1000000000);
        this.blockingQueue = blockingQueue;

        statistic = new Statistic();

        timer = new Timer();
        timer.scheduleAtFixedRate(new Summary(), 0, WAIT);

        try {
            file = new FileWriter(FILE_NAME);
        } catch (Exception e) {
            e.getStackTrace();
        }

        out = new BufferedWriter(file);
    }

    public void shutdown() {
        currentThread.interrupt();
        timer.cancel();

        // Not close the file until the run() function has finish
        while (!runFunctionHasFinished) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                System.out.println("WARN: thread '" + Thread.currentThread().getName() + "' was interrupted.");
                // Restore interrupted state...
                Thread.currentThread().interrupt();
            }
        }

        try {
            out.close();
        } catch (IOException e) {
            System.out.println("ERROR: Could not close writer buffer");
            System.out.println(e);
        }
        try {
            file.close();
        } catch (IOException e) {
            System.out.println("ERROR: Could not close file '" + FILE_NAME + "'");
            System.out.println(e);
        }
    }

    @Override
    public void run() {
        currentThread = Thread.currentThread();
        Integer number;
        while (!currentThread.isInterrupted()) {
            try {
                number = blockingQueue.take();
            } catch (InterruptedException e) {
                // It is expected with the thread is interrupted in the function shutdown()
                System.out.println("INFO: The thread has been interrupted");
                runFunctionHasFinished = true;
                // Restore interrupted state...
                Thread.currentThread().interrupt();
                return;
            }
            synchronized (lock) {
                if (bitSet.get(number)) {
                    statistic.incrementDuplicate();
                } else {
                    try {
                        out.write(number.toString());
                        out.newLine();
                        out.flush();
                    } catch (IOException e) {
                        System.out.println("ERROR: Could not write '" + number + "' into the file '" + FILE_NAME + "'");
                        System.out.println(e);
                    }

                    bitSet.set(number);
                    statistic.incrementUnique();
                }
            }
        }
        runFunctionHasFinished = true;
    }

    final class Summary extends TimerTask {

        @Override
        public void run() {
            synchronized (lock) {
                System.out.printf(statistic.toString());
                statistic.resetUniqueCount();
                statistic.resetDuplicateCount();
            }
        }
    }
}
