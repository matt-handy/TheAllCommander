package util.test.socks5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Time;
import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConstants;

public class TestProcessor extends ClientServerTest {

	public static final String INCOMING_TEST_STR = "This is an incoming transmission";
	public static final String OUTGOING_TEST_STR = "This is an outgoing transmission";
	
	public static class DummyRemoteService implements Runnable {

		final int port;
		final int counter;

		public DummyRemoteService(int port, int counter) {
			this.port = port;
			this.counter = counter;
		}

		@Override
		public void run() {

			try {
				ServerSocket ss = new ServerSocket(port);
				Socket incoming = ss.accept();

				byte[] incomingData = new byte[4096];
				System.out.println("Dummy thread reading");
				int bytesRead = incoming.getInputStream().read(incomingData);
				System.out.println("Dummy thread read");
				String dataFeed = new String(Arrays.copyOf(incomingData, bytesRead));
				assertEquals(INCOMING_TEST_STR + counter, dataFeed);

				incoming.getOutputStream().write((OUTGOING_TEST_STR + counter).getBytes());
				incoming.getOutputStream().flush();

				System.out.println("Closing dummy");
				incoming.close();
				ss.close();
				System.out.println("Closed dummy");
			} catch (IOException ex) {
				ex.printStackTrace();
				fail(ex.getMessage());
			}
		}

	}
	
	public static void testProxy(TestConfiguration config) {

		Properties prop = new Properties();
		try (InputStream input = new FileInputStream("config" + File.separator + config.getServerConfigFile())) {

			// load a properties file
			prop.load(input);

		} catch (IOException ex) {
			System.out.println("Unable to load config file");
			fail(ex.getMessage());
		}
		
		Random rnd = new Random();
		int remoteListen = 40000 + rnd.nextInt(4000);
		int localListen = 40000 + rnd.nextInt(4000);

		ExecutorService service = Executors.newCachedThreadPool();
		DummyRemoteService drs = new DummyRemoteService(remoteListen, 1);
		service.submit(drs);

		try {
			Time.sleepWrapped(5000);

			System.out.println("Connecting test commander...");
			Socket remote = new Socket("localhost", 8111);
			System.out.println("Locking test commander streams...");
			OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

			Time.sleepWrapped(500);
			System.out.println("Setting up test commander session...");

			try {
				RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, config.os == TestConfiguration.OS.LINUX,
						config.isRemote());
			} catch (Exception ex) {
				fail(ex.getMessage());
			}

			if (config.protocol.equals("SMTP")) {
				String output = br.readLine();
				assertEquals(output, "Daemon alive");
			}

			String testIp = null;
			if (config.os == TestConfiguration.OS.LINUX) {
				testIp = TestConstants.PORT_FORWARD_TEST_IP_LINUX;
			} else {
				testIp = TestConstants.PORT_FORWARD_TEST_IP_LOCAL;
			}

			bw.write("proxy " + testIp + " " + remoteListen +" " + localListen + System.lineSeparator());
			bw.flush();

			String confirm = br.readLine();
			assertEquals("Proxy established", confirm);

			bw.write("confirm_client_proxy " + testIp + ":" + remoteListen + System.lineSeparator());
			bw.flush();
			confirm = br.readLine();
			assertEquals("yes", confirm);

			Time.sleepWrapped(1500);

			testProxyMessage(localListen, 1, config);

			Time.sleepWrapped(3000);

			System.out.println("Starting next listener");
			// The old DummyRemoteService will die once it has ack'd the first command.
			// start a new one, and see if there will be a reconnect.
			drs = new DummyRemoteService(remoteListen, 2);
			service.submit(drs);

			Time.sleepWrapped(1000);// Let the new dummy connect and let the client reconnect to it

			testProxyMessage(localListen, 2, config);

			bw.write("killproxy " + testIp + " " + remoteListen + System.lineSeparator());
			bw.flush();

			confirm = br.readLine();
			assertEquals("proxy terminated", confirm);

			bw.write("confirm_client_proxy " + testIp + ":" + remoteListen + System.lineSeparator());
			bw.flush();
			confirm = br.readLine();
			assertEquals("no", confirm);

			Time.sleepWrapped(1000);
			try {
				@SuppressWarnings("unused") // We expect not to use that, socket exists only to throw exception
				Socket socket = new Socket(InetAddress.getLocalHost(), localListen);
				fail();// This socket can't connect here
			} catch (ConnectException ex) {
				assertEquals("Connection refused: connect", ex.getMessage());
			}

			bw.write("die" + System.lineSeparator());
			bw.flush();

			Time.sleepWrapped(2500);

			bw.close();
			br.close();
			remote.close();

			Files.deleteIfExists(Paths.get("System.Net.Sockets.SocketException"));
			Files.deleteIfExists(Paths.get("localAgent", "csc", "System.Net.Sockets.SocketException"));
		} catch (IOException ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}

	public static void testProxyMessage(int port, int counter, TestConfiguration config) throws IOException {
		Socket socket = new Socket(InetAddress.getLocalHost(), port);
		assertTrue(socket.isConnected());

		Time.sleepWrapped(500);// Let all the threads start

		System.out.println("Firing test message");
		socket.getOutputStream().write((INCOMING_TEST_STR + counter).getBytes());
		socket.getOutputStream().flush();

		byte[] incomingData = new byte[4096];
		System.out.println("Reading Test Return");
		int bytesRead = socket.getInputStream().read(incomingData);
		System.out.println("Read Test Return");
		String dataFeed = new String(Arrays.copyOf(incomingData, bytesRead));
		assertEquals(OUTGOING_TEST_STR + counter, dataFeed);

		Time.sleepWrapped(1000);// Let the other thread die, then let the client detect that this message can't
								// be sent
		socket.getOutputStream().write(INCOMING_TEST_STR.getBytes());
		socket.getOutputStream().flush();

		if (config.os == TestConfiguration.OS.LINUX) {
			// Linux needs a second message to figure out the socket is dead.
			Time.sleepWrapped(1000);
			socket.getOutputStream().write(INCOMING_TEST_STR.getBytes());
			socket.getOutputStream().flush();
		}

		socket.close();
	}
	
	public static void testDaemonConnection(String clientStartArg, boolean testBreak, boolean isEmailTest) {
		if (System.getProperty("os.name").contains("Windows")) {
			initiateServer();
		} else {
			initiateServer("test_linux.properties");
		}
		spawnClient(clientStartArg);
		Time.sleepWrapped(6000);
		ExecutorService service = Executors.newCachedThreadPool();
		
		try {			
			//System.out.println("Connecting test commander...");
			Socket remote = new Socket("localhost", 8111);
			System.out.println("Locking test commander streams...");
			OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));
			
			RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, false, false);
		
			Random rnd = new Random();
			int socksPort = 40000 + rnd.nextInt(1000);
			int targetServicePort = 40000 + rnd.nextInt(1000);
			
			bw.write("startSocks5 " + socksPort + System.lineSeparator());
			bw.flush();
			//br.readLine();//Proxy is started

			TargetServerEmulator targetService = new TargetServerEmulator(targetServicePort, testBreak);
			service.submit(targetService);
			targetService.awaitStartup();
			
			SocksClientEmulator clientEmulator = new SocksClientEmulator(socksPort, true, null, testBreak, isEmailTest, targetServicePort);
			service.submit(clientEmulator);
			//System.out.println("Awaiting client end");
			assertTrue(clientEmulator.isComplete());
			//System.out.println("Client ended");
			
			bw.write("killSocks5" + System.lineSeparator());
			bw.write("die" + System.lineSeparator());
			bw.flush();
			remote.close();
			awaitClient();
		}catch(Exception ex) {
			fail(ex.getMessage());
		}
		
		teardown();
	}
}
