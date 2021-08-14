package c2.portforward;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import c2.session.CommandLoader;
import c2.session.IOManager;
import util.Time;

class LocalPortListenerTest {

	final String REMOTE_FORWARD_NAME = "localhost:8000";
	final int LOCAL_LISTEN_PORT = 9001;

	@Test
	void test() {
		IOManager io = new IOManager(Paths.get("test", "log"),
				new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>()));
		int testSessionId = io.addSession("fake", "fake", "fake");
		LocalPortListener listener = new LocalPortListener(io, testSessionId, REMOTE_FORWARD_NAME, LOCAL_LISTEN_PORT);
		ExecutorService service = Executors.newFixedThreadPool(2);
		service.submit(listener);

		Time.sleepWrapped(1000);

		try {
			Socket socket = new Socket("localhost", LOCAL_LISTEN_PORT);
			byte[] sampleData = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
			socket.getOutputStream().write(sampleData);
			socket.getOutputStream().flush();

			Time.sleepWrapped(1000);

			String base64Data = io.grabForwardedTCPTraffic(testSessionId, REMOTE_FORWARD_NAME);
			byte[] receivedData = Base64.getDecoder().decode(base64Data);
			assertArrayEquals(sampleData, receivedData);
			base64Data = Base64.getEncoder().encodeToString(sampleData);
			System.out.println("Queuing " + testSessionId + " " + REMOTE_FORWARD_NAME);
			io.queueForwardedTCPTraffic(testSessionId, REMOTE_FORWARD_NAME, base64Data);

			Time.sleepWrapped(1000);

			System.out.println("Reading test socket");
			byte[] mirroredData = new byte[4096];
			int bytesRead = socket.getInputStream().read(mirroredData);
			assertArrayEquals(sampleData, Arrays.copyOf(mirroredData, bytesRead));

			socket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			fail();
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

}
