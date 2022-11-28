package org.example.server;

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

    private BitSet bitSet;
    private BlockingQueue<Integer> blockingQueue;

    private int uniqueCount;
    private int duplicateCount;
    private int uniqueTotal;
    private FileWriter file;
    private BufferedWriter out;

    private boolean continueLoop;


    public LoggingThread(BlockingQueue<Integer> blockingQueue) {
        lock = new Object();

        continueLoop = true;
        bitSet = new BitSet(1000000000);
        uniqueCount = 0;
        duplicateCount = 0;
        uniqueTotal = 0;

        this.blockingQueue = blockingQueue;

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new Summary(), 0, WAIT);

        try {
            file = new FileWriter(FILE_NAME);
        } catch (Exception e) {
            e.getStackTrace();
        }

        out = new BufferedWriter(file);
    }

    public void close() {
        continueLoop = false;

        // TODO check if the thread has finished
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
        Integer number;
        while (continueLoop) {
            try {
                number = blockingQueue.take();
                System.out.println("Take number from blockingQueue " + number);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            synchronized (lock) {
                if (bitSet.get(number)) {
                    duplicateCount++;
                    return;
                }

                bitSet.set(number);
                uniqueCount++;
                uniqueTotal++;

                try {
                    out.write(number.toString());
                    out.newLine();
                    out.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    final class Summary extends TimerTask {

        @Override
        public void run() {
            synchronized (lock) {
                System.out.printf("Received %d unique numbers, %d duplicates. Unique total: %d\n",
                        uniqueCount, duplicateCount, uniqueTotal);
                uniqueCount = 0;
                duplicateCount = 0;
            }
        }
    }
}
