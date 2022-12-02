import org.example.server.LoggingThread;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoggingThreadTest {

    private static final String FILE_NAME = "/tmp/numbers.log";
    private FileReader file;
    private BufferedReader reader;

    private void openFile() {
        try {
            file = new FileReader(FILE_NAME);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        reader = new BufferedReader(file);
    }

    private void closeFile() {
        try {
            reader.close();
            file.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checkContentFile(String expectedNumber) {
        openFile();
        boolean contentFound = reader.lines().filter(line -> line.equals(expectedNumber)).findFirst().isPresent();
        closeFile();

        return contentFound;
    }

    @Test
    void givenLoggingThread_whenReceiveANumber_thenCheckStatistic() {
        Integer number = 123456789;
        BlockingQueue<Integer> blockingQueue = new LinkedBlockingQueue<>();
        blockingQueue.add(number);

        LoggingThread loggingThread = new LoggingThread(blockingQueue);
        assertNotNull(loggingThread);
        Thread thread = new Thread(loggingThread);
        thread.start();

        await().until(() -> loggingThread.getStatistic().getUniqueTotal() == 1);

        loggingThread.shutdown();
        await().until(() -> loggingThread.isShutDown());
        thread.interrupt();

        assertTrue(checkContentFile(number.toString()));
    }

    @Test
    void givenLoggingThread_whenReceiveDuplicatedNumbers_thenCheckStatistic() {
        Integer number = 123456789;
        BlockingQueue<Integer> blockingQueue = new LinkedBlockingQueue<>();
        blockingQueue.add(number);
        blockingQueue.add(number);

        LoggingThread loggingThread = new LoggingThread(blockingQueue);
        assertNotNull(loggingThread);
        Thread thread = new Thread(loggingThread);
        thread.start();

        await().until(() -> loggingThread.getStatistic().getUniqueTotal() == 1);
        await().until(() -> loggingThread.getStatistic().getDuplicateTotal() == 1);

        loggingThread.shutdown();
        await().until(() -> loggingThread.isShutDown());
        thread.interrupt();

        assertTrue(checkContentFile(number.toString()));
    }
}