package c2.portforward;

import static org.junit.jupiter.api.Assertions.*;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import c2.Constants;
import c2.RunnerTestGeneric;
import util.Time;
import util.test.ClientServerTest;
import util.test.TestConfiguration;
import util.test.TestConstants;

class PythonPortForwardTest  extends ClientServerTest {

	public static final String INCOMING_TEST_STR = "This is an incoming transmission";
	public static final String OUTGOING_TEST_STR = "This is an outgoing transmission";
	
	static class DummyRemoteService implements Runnable {

		final int port;

		DummyRemoteService(int port) {
			this.port = port;
		}

		@Override
		public void run() {

			try {
				ServerSocket ss = new ServerSocket(port);
				Socket incoming = ss.accept();
				
				byte[] incomingData = new byte[4096];
				int bytesRead = incoming.getInputStream().read(incomingData);
				String dataFeed = new String(Arrays.copyOf(incomingData, bytesRead));
				assertEquals(INCOMING_TEST_STR, dataFeed);
				
				incoming.getOutputStream().write(OUTGOING_TEST_STR.getBytes());
				incoming.getOutputStream().flush();
				
				incoming.close();
				ss.close();
			} catch (IOException ex) {
				ex.printStackTrace();
				fail(ex.getMessage());
			}
		}

	}

	@Test
	void test() {
		testLocal();
		testDNS();
	}

	static void testLocal() {
		initiateServer();
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "httpsAgent.py\"";
		spawnClient(clientCmd);
		
		TestConfiguration testConfig = new TestConfiguration(TestConfiguration.OS.WINDOWS, "python", "HTTPS");
		testProxy(testConfig);
		
		teardown();
	}
	
	static void testDNS() {
		initiateServer();
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "dnsAgent.py\"";
		spawnClient(clientCmd);
		
		TestConfiguration testConfig = new TestConfiguration(TestConfiguration.OS.WINDOWS, "python", "DNS");
		testProxy(testConfig);
		
		teardown();
	}

	public static void testProxy(TestConfiguration config) {

		Properties prop = new Properties();
		try (InputStream input = new FileInputStream("test" + File.separator + config.getServerConfigFile())) {

			// load a properties file
			prop.load(input);

		} catch (IOException ex) {
			System.out.println("Unable to load config file");
			fail(ex.getMessage());
		}
		
		ExecutorService service = Executors.newCachedThreadPool();
		DummyRemoteService drs = new DummyRemoteService(9001);
		service.submit(drs);

		try {
			// This hack is b/c for some reason the C++ daemon doesn't create the dir on my
			// laptop
			Files.createDirectories(Paths.get(prop.getProperty(Constants.DAEMONLZHARVEST),
					InetAddress.getLocalHost().getHostName().toUpperCase() + "-screen", "matte"));
			// end hack

			Files.deleteIfExists(Paths.get("System.Net.Sockets.SocketException"));
			Files.deleteIfExists(Paths.get("localAgent", "csc", "System.Net.Sockets.SocketException"));

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

			bw.write("proxy 127.0.0.1 9001 9002" + System.lineSeparator());
			bw.flush();
			
			Time.sleepWrapped(1500);
			
			Socket socket = new Socket(InetAddress.getLocalHost(), 9002);
			assertTrue(socket.isConnected());
			socket.getOutputStream().write(INCOMING_TEST_STR.getBytes());
			socket.getOutputStream().flush();
			
			byte[] incomingData = new byte[4096];
			int bytesRead = socket.getInputStream().read(incomingData);
			String dataFeed = new String(Arrays.copyOf(incomingData, bytesRead));
			assertEquals(OUTGOING_TEST_STR, dataFeed);
			
			socket.close();
			
			//TODO Test killproxy on client side
			bw.write("killproxy 127.0.0.1 9001" + System.lineSeparator());
			bw.flush();
			Time.sleepWrapped(1000);
			try {
				socket = new Socket(InetAddress.getLocalHost(), 9002);
				fail();//This socket can't connect here
			}catch(ConnectException ex) {
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

	
}
