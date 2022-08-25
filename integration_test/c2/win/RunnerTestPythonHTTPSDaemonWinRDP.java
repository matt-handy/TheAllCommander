package c2.win;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import c2.session.CommandMacroManager;
import util.Time;
import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;
import util.test.TestConstants;

public class RunnerTestPythonHTTPSDaemonWinRDP extends ClientServerTest {

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
	
	@Test
	void testLocal() {
		test();
	}

	public static void test() {
		initiateServer();
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "httpsAgent.py\"";
		spawnClient(clientCmd);

		System.out.println("Transmitting commands");

		RDPTestRunner();

	}
	
	public static void RDPTestRunner() {
		try {
			ExecutorService service = Executors.newCachedThreadPool();
			DummyRemoteService drs = new DummyRemoteService(3389, 1);
			service.submit(drs);
			
			//Path clientChiselBin;
			//Path remoteChiselDir;
			try {
				Thread.sleep(5000);// allow both commander and daemon to start
			} catch (InterruptedException e) {
				// Ensure that python client has connected
			}
			System.out.println("Connecting test commander...");
			Socket remote = new Socket("localhost", 8111);
			System.out.println("Locking test commander streams...");
			OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

			System.out.println("Setting up test commander session...");
			try {
				RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, false, false);
			} catch (Exception ex) {
				fail(ex.getMessage());
			}

			bw.write(CommandMacroManager.ACTIVATE_RDP_CMD + " barfington" + System.lineSeparator());
			bw.flush();

			Time.sleepWrapped(20000);

			// Is my "remote" persistence reg key in place? (roll it back)
			/*
			List<String> regSettings = WindowsCmdLineHelper
					.runRegistryQuery("reg query " + WindowsRDPManager.PERSIST_REG_KEY);
			boolean foundChisel = false;
			for (String line : regSettings) {
				if (line.contains("chisel.exe client 127.0.0.1:40000 R:40001:127.0.0.1:3389")) {
					foundChisel = true;
				}
			}
			assertTrue(foundChisel);
			String regCleanup = "reg delete HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run /v Chisel /f";
			WindowsCmdLineHelper.runMultilineCmd(regCleanup);
			
			// Is my "remote" chisel exe in place (roll it back)
			String appData = System.getenv().get("APPDATA");
			String exe = WindowsRDPManager.CLIENT_CHISEL_DIR.replace("%APPDATA%", appData) + "\\"
					+ WindowsRDPManager.CHISEL_EXE;
			clientChiselBin = Paths.get(exe);
			assertTrue(Files.exists(clientChiselBin));

			// Is my "local" chisel running?
			// We're going to trust that it is b/c it's tested in another class

			// Is my "remote" chisel folder in place? (roll it back)
			remoteChiselDir = Paths.get(WindowsRDPManager.CLIENT_CHISEL_DIR.replace("%APPDATA%", appData));
			assertTrue(Files.exists(remoteChiselDir));

			// Is my "remote" chisel running?
			validateClientsideChisel();
			*/
			
			//Try connecting to local server port and xmitting data to receiver
			TestConfiguration config = new TestConfiguration(OS.WINDOWS, "N/A", "N/A");
			testProxyMessage(40000, 1, config);
			
			// Test that the "log" is output to the client at the end, and that the RDP
			// group and user were not added
			// due to a lack of elevated privs
			// Read the log that came back, it should error out b/c we did not run the
			// daemon with admin rights
			String line = br.readLine();
			assertEquals(line, "Cannot enable RDP, errors encountered: ");
			line = br.readLine();
			assertEquals(line, "I need elevate privileges to enable RDP:");

			bw.write("die" + System.lineSeparator());
			bw.flush();

			Time.sleepWrapped(2000);

			teardown();

			//Files.deleteIfExists(clientChiselBin);
			//Files.deleteIfExists(remoteChiselDir);
		} catch (Exception ex) {
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
	
	private static boolean validateClientsideChisel() {

		List<String> processes = WindowsCmdLineHelper.listRunningProcesses();
		for (String process : processes) {
			if (process.contains("chisel.exe")) {
				return true;
			}
		}
		return false;
	}
}
