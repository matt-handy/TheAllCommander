package c2.python;

import java.io.File;

import org.junit.jupiter.api.Test;

import c2.smtp.EmailHandlerTester;
import util.Time;
import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConstants;
import util.test.TestConfiguration.OS;

public class RunnerTestPython extends ClientServerTest {

	@Test
	void testLocal() {
		testHTTPS();
		testDNS();
		testHTTPSTwoSessions();
		testDNSTwoSessions();
	}
	
	public static void testHTTPS() {
		initiateServer();
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "httpsAgent.py\"";
		System.out.println("Spawning: " + clientCmd);
		spawnClient(clientCmd);
		
		TestConfiguration testConfig = new TestConfiguration(TestConfiguration.OS.WINDOWS, "python", "HTTPS");
		RunnerTestGeneric.test(testConfig);
		
		teardown();
	}
	
	public static void testHTTPSTwoSessions() {
		initiateServer();
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "httpsAgent.py\"";
		spawnClient(clientCmd);
		Time.sleepWrapped(1000);
		spawnClient(clientCmd);
		
		TestConfiguration testConfig = new TestConfiguration(TestConfiguration.OS.WINDOWS, "python", "HTTPS");
		testConfig.setTestTwoClients(true);
		testConfig.setTestSecondaryClient(true);
		RunnerTestGeneric.test(testConfig);
		
		TestConfiguration configParent = new TestConfiguration(OS.WINDOWS, "python", "HTTPS");
		configParent.setTestTwoClients(true);
		RunnerTestGeneric.test(configParent);
		
		teardown();
	}

	public static void testDNS() {
		initiateServer();
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "dnsSimpleAgent.py\"";
		spawnClient(clientCmd);
		
		TestConfiguration testConfig = new TestConfiguration(TestConfiguration.OS.WINDOWS, "python", "DNS");
		RunnerTestGeneric.test(testConfig);
		
		teardown();
	}
	
	public static void testDNSTwoSessions() {
		initiateServer();
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "dnsSimpleAgent.py\"";
		spawnClient(clientCmd);
		Time.sleepWrapped(1000);
		spawnClient(clientCmd);
		
		TestConfiguration testConfig = new TestConfiguration(TestConfiguration.OS.WINDOWS, "python", "DNS");
		testConfig.setTestTwoClients(true);
		testConfig.setTestSecondaryClient(true);
		RunnerTestGeneric.test(testConfig);
		
		TestConfiguration configParent = new TestConfiguration(OS.WINDOWS, "python", "DNS");
		configParent.setTestTwoClients(true);
		RunnerTestGeneric.test(configParent);
		
		teardown();
	}
	
}
