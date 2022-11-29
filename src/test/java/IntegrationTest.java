import org.example.server.Server;
import org.example.server.Statistic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntegrationTest {

    private final static int MAX_NUMBER = 999999999;
    private final static int MIN_NUMBER = 0;
    private static final String FILE_NAME = "/tmp/numbers.log";

    private static Random random;
    private FileReader file;
    private BufferedReader reader;

    @BeforeAll
    static void setup() {
        random = new Random();
    }

    private int getRandomNumber(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min) + min;
    }

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

    private boolean checkContentFile(String expected) {
        openFile();
        boolean isPresent = reader.lines().filter(line -> line.equals(expected)).findFirst().isPresent();
        closeFile();

        return isPresent;
    }

    @Test
    void givenServerAndClient_whenSendNumber_thenCheckLogFile() {
        // Given server
        Server server = new Server();
        assertNotNull(server);
        Runnable runnable = () -> {
            server.start();
        };
        Thread thread = new Thread(runnable);
        thread.start();

        // Given client
        Client client = new Client();

        // When send number
        Integer number = getRandomNumber(MIN_NUMBER, MAX_NUMBER);
        client.sendNumber(number);
        await().until(() -> server.getStatistic().getUniqueTotal() == 1);
        assertEquals(0, server.getStatistic().getDuplicateTotal());

        // Close client and server
        client.shutdown();
        server.shutdown();
        thread.interrupt();

        // Then check log file
        assertTrue(checkContentFile(number.toString()));
    }

    @Test
    void givenServerAndClient_whenSendTerminate_thenServerShutDown() {
        // Given server
        Server server = new Server();
        assertNotNull(server);
        Runnable runnable = () -> {
            server.start();
        };
        Thread thread = new Thread(runnable);
        thread.start();

        // Given client
        Client client = new Client();

        // When send terminate
        client.sendTerminateCommand();
        await().until(() -> server.isShutDown());

        // Close client and server
        client.shutdown();
        thread.interrupt();
    }

    @Test
    void givenServerAndClient_whenSendingAMessageWithout9digits_thenClientIsShutDown() {
        // Given server
        Server server = new Server();
        assertNotNull(server);
        Runnable runnable = () -> {
            server.start();
        };
        Thread thread = new Thread(runnable);
        thread.start();

        // Given client
        Client client = new Client();

        // When send number
        Integer number = getRandomNumber(MIN_NUMBER, MAX_NUMBER);
        client.send("invalid message");
        await().until(() -> client.getSocket().isConnected());

        // Close client and server
        client.shutdown();
        server.shutdown();
        thread.interrupt();
    }

    @Test
    void givenServerAndClient_whenSendingAnEmptyMessage_thenClientIsShutDown() {
        // Given server
        Server server = new Server();
        assertNotNull(server);
        Runnable runnable = () -> {
            server.start();
        };
        Thread thread = new Thread(runnable);
        thread.start();

        // Given client
        Client client = new Client();

        // When send number
        Integer number = getRandomNumber(MIN_NUMBER, MAX_NUMBER);
        client.send("");
        await().until(() -> client.getSocket().isConnected());

        // Close client and server
        client.shutdown();
        server.shutdown();
        thread.interrupt();
    }

}
