import org.example.server.Server;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrationTest {

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

    private boolean checkContentFile(String expectedNumber) {
        openFile();
        boolean contentFound = reader.lines().filter(line -> line.equals(expectedNumber)).findFirst().isPresent();
        closeFile();

        return contentFound;
    }

    private boolean checkContentFile(List<String> expectedNumbers) {
        openFile();
        boolean contentFound = reader.lines().sorted().collect(Collectors.toList()).equals(
                expectedNumbers.stream().sorted().collect(Collectors.toList()));
        closeFile();

        return contentFound;
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
    void givenServerAndClient_whenSend1000Numbers_thenCheckLogFile() {
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

        // When send  1000 number
        final int MAX_SEND = 1000;
        List<String> writtenNumbers = new ArrayList<>(MAX_SEND);
        Integer number;
        for (int i = 0; i < MAX_SEND; i++) {
            number = getRandomNumber(MIN_NUMBER, MAX_NUMBER);
            while (writtenNumbers.contains(number)) {
                number = getRandomNumber(MIN_NUMBER, MAX_NUMBER);
            }
            writtenNumbers.add(number.toString());
            client.sendNumber(number);
        }

        await().until(() -> server.getStatistic().getUniqueTotal() == MAX_SEND);
        assertEquals(0, server.getStatistic().getDuplicateTotal());

        // Close client and server
        client.shutdown();
        server.shutdown();
        thread.interrupt();

        // Then check log file
        assertTrue(checkContentFile(writtenNumbers));
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

    private void givenServerAndClient_whenSendingAInvalidMessage_thenClientIsShutDown(String message) {
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
        client.send(message);
        await().until(() -> client.getSocket().isConnected());

        // Close client and server
        client.shutdown();
        server.shutdown();
        thread.interrupt();
    }

    @Test
    void givenServerAndClient_whenSendingAMessageWithout9digits_thenClientIsShutDown() {
        givenServerAndClient_whenSendingAInvalidMessage_thenClientIsShutDown("invalid message");
    }

    @Test
    void givenServerAndClient_whenSendingAnEmptyMessage_thenClientIsShutDown() {
        givenServerAndClient_whenSendingAInvalidMessage_thenClientIsShutDown("");
    }

    @Test
    void givenServerAndClient_whenNotSendingANumberButItHas9Characters_thenClientIsShutDown() {
        givenServerAndClient_whenSendingAInvalidMessage_thenClientIsShutDown("aaaaaaaaa");
    }

    @Test
    void givenServerAndClient_whenSendingNegativeNumber_thenClientIsShutDown() {
        givenServerAndClient_whenSendingAInvalidMessage_thenClientIsShutDown("-12345678");
    }

    @Test
    void givenServerAndClient_whenSendTheSameNumberTwice_thenDuplicateTotal1AndCheckLogFile() {
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
        client.sendNumber(number);
        await().until(() -> server.getStatistic().getUniqueTotal() == 1);
        assertEquals(1, server.getStatistic().getDuplicateTotal());

        // Close client and server
        client.shutdown();
        server.shutdown();
        thread.interrupt();

        // Then check log file
        assertTrue(checkContentFile(number.toString()));
    }

    @Test
    void givenServerAnd5Clients_whenSend1000Numbers_thenCheckLogFile() {
        // Given server
        Server server = new Server();
        assertNotNull(server);
        Runnable runnable = () -> {
            server.start();
        };
        Thread thread = new Thread(runnable);
        thread.start();

        // Given 5 clients
        final int MAX_CLIENT = 5;
        List<Client> clients = new ArrayList<>(MAX_CLIENT);
        for (int i = 0; i < MAX_CLIENT; i++) {
            clients.add(new Client());
        }

        // When send  1000 number between the different clients
        final int MAX_SEND = 1000;
        List<String> writtenNumbers = new ArrayList<>(MAX_SEND);
        Integer number;
        for (int i = 0; i < MAX_SEND; i++) {
            number = getRandomNumber(MIN_NUMBER, MAX_NUMBER);
            while (writtenNumbers.contains(number)) {
                number = getRandomNumber(MIN_NUMBER, MAX_NUMBER);
            }
            writtenNumbers.add(number.toString());
            clients.get(i % MAX_CLIENT).sendNumber(number);
        }

        await().until(() -> server.getStatistic().getUniqueTotal() == MAX_SEND);
        assertEquals(0, server.getStatistic().getDuplicateTotal());

        // Close client and server
        for (Client c : clients) {
            c.shutdown();
        }
        server.shutdown();
        thread.interrupt();

        // Then check log file
        assertTrue(checkContentFile(writtenNumbers));
    }

    @Test
    void givenServerAnd6Clients_whenSendNumbers_thenClient6CouldNotConnect() {
        // Given server
        Server server = new Server();
        assertNotNull(server);
        Runnable runnable = () -> {
            server.start();
        };
        Thread thread = new Thread(runnable);
        thread.start();

        // Given 6 clients
        final int MAX_CLIENT = 6;
        final int MAX_CLIENT_CONNECTED = 5;
        List<Client> clients = new ArrayList<>(MAX_CLIENT);
        for (int i = 0; i < MAX_CLIENT; i++) {
            clients.add(new Client());
        }

        // When send  6 number between the different clients
        final int MAX_SEND = MAX_CLIENT_CONNECTED;
        List<String> writtenNumbers = new ArrayList<>(MAX_SEND);
        for (Integer i = 0; i < MAX_SEND; i++) {
            writtenNumbers.add(i.toString());
            clients.get(i).sendNumber(i);
        }
        clients.get(MAX_CLIENT - 1).sendNumber(MAX_CLIENT);

        await().until(() -> server.getStatistic().getUniqueTotal() == MAX_CLIENT_CONNECTED);
        assertEquals(0, server.getStatistic().getDuplicateTotal());

        // Close server first and clients
        server.shutdown();
        thread.interrupt();
        await().until(() -> server.isShutDown());
        for (Client c : clients) {
            c.shutdown();
        }

        // Then check log file does not contain the last number from the last client.
        assertTrue(checkContentFile(writtenNumbers));
    }

    // Performance test
    @Test
    void givenServerAnd5Clients_whenSend2MNumbers_thenCheckStatistic() {
        // Given server
        Server server = new Server();
        assertNotNull(server);
        Runnable runnableServer = () -> {
            server.start();
        };
        Thread thread = new Thread(runnableServer);
        thread.start();

        // Given 5 clients
        final int MAX_CLIENT = 5;
        final int MAX_SEND = 400000;
        List<Client> clients = new ArrayList<>(MAX_CLIENT);
        for (int i = 0; i < MAX_CLIENT; i++) {
            clients.add(new Client());
        }

        // In parallel send messages from the different clients
        List<Thread> threadsClients = new ArrayList<>(MAX_CLIENT);
        for (int i = 0; i < MAX_CLIENT; i++) {
            final int index = i;
            threadsClients.add(new Thread(
                    () -> {
                        for (int j = MAX_SEND * index; j < MAX_SEND * (index + 1); j++) {
                            clients.get(index).sendNumber(j);
                        }
                    }));
        }
        for (Thread t : threadsClients) {
            t.start();
        }

        // Then check statistic
        await().atMost(3, TimeUnit.MINUTES).until(() -> server.getStatistic().getUniqueTotal() == MAX_SEND * MAX_CLIENT);
        assertEquals(0, server.getStatistic().getDuplicateTotal());

        // Close client and server
        server.shutdown();
        thread.interrupt();
        for (Client c : clients) {
            c.shutdown();
        }
        for (Thread t : threadsClients) {
            t.interrupt();
        }
    }

    @Test
    void givenServer_whenCreatingAnExtraService_thenExceptionPortIsInUse() {
        // Given server
        Server server1 = new Server();
        assertNotNull(server1);

        RuntimeException exception = assertThrows(java.lang.RuntimeException.class, () -> {
            new Server();
        });
        assertEquals("java.net.BindException: Address already in use", exception.getMessage());

        server1.shutdown();
    }
}
