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
                throw new RuntimeException(e);
            }
        }

        try {
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            file.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        currentThread = Thread.currentThread();
        Integer number;
        while (!currentThread.isInterrupted()) {
            System.out.println("Take numbers from blockingQueue");

            try {
                number = blockingQueue.take();
                System.out.println("Take number from blockingQueue " + number);
            } catch (InterruptedException e) {
                // It is expected with the thread is interrupted in the function shutdown()
                System.out.println("INFO: The thread has been interrupted");
                break;
            }
            synchronized (lock) {
                if (bitSet.get(number)) {
                    statistic.incrementDuplicateCount();
                    continue;
                }

                try {
                    out.write(number.toString());
                    out.newLine();
                    out.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                bitSet.set(number);
                statistic.incrementUniqueCount();
                statistic.incrementUniqueTotal();
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
