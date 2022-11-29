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
        String data;
        System.out.println(expected);

        try {
            while ((data = reader.readLine()) != null) {
                System.out.println(data);
                if (data.equals(expected)) {
                    return true;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @Test
    void givenServerAndClient_whenSendNumber_thenCheckLogFile() {
        Server server = new Server();
        assertNotNull(server);
        Runnable runnable = () -> {
            server.start();
        };
        Thread thread = new Thread(runnable);
        thread.start();

        Client client = new Client();

        Integer number = getRandomNumber(MIN_NUMBER, MAX_NUMBER);
        client.sendNumber(number);

        await().until(() -> server.getStatistic().getUniqueTotal() == 1);

        client.shutdown();
        server.shutdown();
        thread.interrupt();

        openFile();
        assertTrue(checkContentFile(number.toString()));
        closeFile();
    }

}
