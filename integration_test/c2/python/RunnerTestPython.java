package c2.python;

import java.io.File;

import org.junit.jupiter.api.Test;

import c2.RunnerTestGeneric;
import c2.smtp.EmailHandlerTester;
import util.test.ClientServerTest;
import util.test.TestConfiguration;
import util.test.TestConstants;

public class RunnerTestPython extends ClientServerTest {

	@Test
	void testLocal() {
		testHTTPS();
		testDNS();
		testEmail();
	}
	
	public static void testHTTPS() {
		initiateServer();
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "httpsAgent.py\"";
		spawnClient(clientCmd);
		
		TestConfiguration testConfig = new TestConfiguration(TestConfiguration.OS.WINDOWS, "python", "HTTPS");
		RunnerTestGeneric.test(testConfig);
		
		teardown();
	}

	public static void testDNS() {
		initiateServer();
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "dnsAgent.py\"";
		spawnClient(clientCmd);
		
		TestConfiguration testConfig = new TestConfiguration(TestConfiguration.OS.WINDOWS, "python", "DNS");
		RunnerTestGeneric.test(testConfig);
		
		teardown();
	}
	
	public static void testEmail() {
		EmailHandlerTester.flushC2Emails();
		initiateServer();
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "emailAgent.py\"";
		spawnClient(clientCmd);
		
		TestConfiguration testConfig = new TestConfiguration(TestConfiguration.OS.WINDOWS, "python", "SMTP");
		RunnerTestGeneric.test(testConfig);
		
		teardown();
	}
}
