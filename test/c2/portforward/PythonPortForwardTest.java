package c2.portforward;

import org.junit.jupiter.api.Test;

import util.test.ClientServerTest;
import util.test.TestConfiguration;
import util.test.TestConstants;
import util.test.socks5.TestProcessor;

public class PythonPortForwardTest extends ClientServerTest {

	@Test
	void testHTTPS() {
		//Tests not yet validated on Linux
		/*
		if (System.getProperty("os.name").contains("Windows")) {
		initiateServer();
		spawnClient(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);

		TestConfiguration testConfig = new TestConfiguration(TestConfiguration.OS.WINDOWS, "python", "HTTPS");
		TestProcessor.testProxy(testConfig);
		awaitClient();
		teardown();
		}
		*/
	}

	@Test
	void testDNS() {
		//Tests not yet validated on Linux/Mac
		if (System.getProperty("os.name").contains("Windows")) {
		initiateServer();
		spawnClient(TestConstants.PYTHON_DNSDAEMON_TEST_EXE);

		TestConfiguration testConfig = new TestConfiguration(TestConfiguration.OS.WINDOWS, "python", "DNS");
		TestProcessor.testProxy(testConfig);
		awaitClient();
		teardown();
		}
	}

	
	@Test
	void testEmail() {
		//Disable Email test for now, noted to address later in CLI 21.7
		/*
		EmailHelper.flushC2Emails();
		initiateServer();
		spawnClient(TestConstants.PYTHON_SMTPDAEMON_TEST_EXE);

		TestConfiguration testConfig = new TestConfiguration(TestConfiguration.OS.WINDOWS, "python", "SMTP");
		testProxy(testConfig);
		awaitClient();
		teardown();
		*/
	}

	
}
