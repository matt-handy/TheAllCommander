package c2.portforward.socks;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import util.Time;
import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConstants;
import util.test.socks5.SocksClientEmulator;
import util.test.socks5.TargetServerEmulator;

public class PythonSocks5Test extends ClientServerTest {

	public static void testAll() {
		testHTTPSDaemonNominalConnectionS();
		testHTTPSDaemonConnectionBrokenWithServerS();
	}
	
	
	@Test
	void testHTTPSDaemonNominalConnection() {
		testHTTPSDaemonNominalConnectionS();
	}
	
	static void testHTTPSDaemonNominalConnectionS() {
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "httpsAgent.py\"";
		testDaemonConnection(clientCmd, false, false);
	}
	
	@Test
	void testDNSDaemonNominalConnection() {
		testDNSDaemonNominalConnectionS();
	}
	
	static void testDNSDaemonNominalConnectionS() {
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "dnsSimpleAgent.py\"";
		testDaemonConnection(clientCmd, false, false);
	}
	
	@Test
	void testEmailDaemonNominalConnection() {
		testEmailDaemonNominalConnectionS();
	}
	
	static void testEmailDaemonNominalConnectionS() {
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "emailAgent.py\"";
		testDaemonConnection(clientCmd, false, true);
	}
	
	static void testDaemonConnection(String clientStartArg, boolean testBreak, boolean isEmailTest) {
		initiateServer();
		System.out.println("Spawning: " + clientStartArg);
		spawnClient(clientStartArg);
		Time.sleepWrapped(6000);
		ExecutorService service = Executors.newCachedThreadPool();
		
		Properties prop = new Properties();
		try (InputStream input = new FileInputStream("config" + File.separator + "test.properties")) {

			// load a properties file
			prop.load(input);
			
			//System.out.println("Connecting test commander...");
			Socket remote = new Socket("localhost", 8111);
			System.out.println("Locking test commander streams...");
			OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));
			
			RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, false, false);
			
			bw.write("startSocks5 9000" + System.lineSeparator());
			bw.flush();
			//br.readLine();//Proxy is started

			TargetServerEmulator targetService = new TargetServerEmulator(4096, testBreak);
			service.submit(targetService);
			targetService.awaitStartup();
			
			SocksClientEmulator clientEmulator = new SocksClientEmulator(9000, true, null, testBreak, isEmailTest);
			service.submit(clientEmulator);
			//System.out.println("Awaiting client end");
			assertTrue(clientEmulator.isComplete());
			//System.out.println("Client ended");
			
			bw.write("killSocks5" + System.lineSeparator());
			bw.write("die" + System.lineSeparator());
			bw.flush();
			remote.close();
			Time.sleepWrapped(3000);//Give time for client to receive kill order
		} catch (Exception ex) {
			System.out.println("Unable to load config file");
			fail(ex.getMessage());
		}
		
		teardown();
	}
	
	@Test
	void testHTTPSDaemonConnectionBrokenWithServer() {
		testHTTPSDaemonConnectionBrokenWithServerS();
	}
	
	static void testHTTPSDaemonConnectionBrokenWithServerS() {
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "httpsAgent.py\"";
		testDaemonConnection(clientCmd, true, false);
	}
	
	@Test
	void testDNSDaemonConnectionBrokenWithServer() {
		testDNSDaemonConnectionBrokenWithServerS();
	}
	
	static void testDNSDaemonConnectionBrokenWithServerS() {
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "dnsSimpleAgent.py\"";
		testDaemonConnection(clientCmd, true, false);
	}

	@Test
	void testEmailDaemonConnectionBrokenWithServer() {
		testEmailDaemonConnectionBrokenWithServerS();
	}
	
	static void testEmailDaemonConnectionBrokenWithServerS() {
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "emailAgent.py\"";
		testDaemonConnection(clientCmd, true, true);
	}
	
}
