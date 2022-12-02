import org.example.server.Server;
import org.example.server.SocketThread;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SocketThreadTest {
    private static final String TERMINATE_COMMAND = "terminate";
    @Mock
    private Socket socket;
    @Mock
    private Server server;

    @Test
    void givenSocketThread_whenReceiveNumbers_thenCheckBlockingQueue() throws IOException, InterruptedException {
        Integer number = 123456789;
        BlockingQueue<Integer> blockingQueue = new LinkedBlockingQueue<>();
        InputStream anyInputStream = new ByteArrayInputStream(number.toString().getBytes());
        when(socket.getInputStream()).thenReturn(anyInputStream);

        SocketThread socketThread = new SocketThread(socket, blockingQueue, server);
        assertNotNull(socketThread);
        Thread thread = new Thread(socketThread);
        thread.start();

        await().until(() -> blockingQueue.size() == 1);
        assertEquals(number, blockingQueue.take());

        thread.interrupt();
    }

    void givenSocketThread_whenReceiveInvalidInput_thenCheckShutDown(String input) throws IOException, InterruptedException {
        BlockingQueue<Integer> blockingQueue = new LinkedBlockingQueue<>();
        InputStream anyInputStream = new ByteArrayInputStream(input.getBytes());
        when(socket.getInputStream()).thenReturn(anyInputStream);

        SocketThread socketThread = new SocketThread(socket, blockingQueue, server);
        assertNotNull(socketThread);
        Thread thread = new Thread(socketThread);
        thread.start();

        await().until(() -> socketThread.isShutDown());

        thread.interrupt();
    }

    @Test
    void givenSocketThread_whenReceiveUnexpectedLengthInput_thenCheckShutDown() throws IOException, InterruptedException {
        givenSocketThread_whenReceiveInvalidInput_thenCheckShutDown("UnexpectedLengthInput");
    }

    @Test
    void givenSocketThread_whenReceiveNotInteger_thenCheckShutDown() throws IOException, InterruptedException {
        givenSocketThread_whenReceiveInvalidInput_thenCheckShutDown("aaaaaaaaa");
    }

    @Test
    void givenSocketThread_whenReceiveNegativetInteger_thenCheckShutDown() throws IOException, InterruptedException {
        givenSocketThread_whenReceiveInvalidInput_thenCheckShutDown("-12345678");
    }

    @Test
    void givenSocketThread_whenReceiveTerminate_thenCheckShutDown() throws IOException, InterruptedException {
        givenSocketThread_whenReceiveInvalidInput_thenCheckShutDown(TERMINATE_COMMAND);
    }
}
