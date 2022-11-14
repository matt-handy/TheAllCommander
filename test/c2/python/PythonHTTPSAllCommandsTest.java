package c2.python;

import java.io.File;

import org.junit.jupiter.api.Test;

import util.Time;
import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConstants;
import util.test.TestConfiguration.OS;

class PythonHTTPSAllCommandsTest extends ClientServerTest {

	@Test
	void testHTTPS() {
		TestConfiguration.OS osConfig = null;
		if (System.getProperty("os.name").contains("Windows")) {
			osConfig = TestConfiguration.OS.WINDOWS;
		} else {
			osConfig = TestConfiguration.OS.LINUX;
		}

		TestConfiguration testConfig = new TestConfiguration(osConfig, "python", "HTTPS");
		executeStandardTest(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE, testConfig);
	}
	
	@Test
	void testHTTPSTwoSessions() {
		initiateServer();
		TestConfiguration.OS osConfig = null;
		if (System.getProperty("os.name").contains("Windows")) {
			osConfig = TestConfiguration.OS.WINDOWS;
		} else {
			osConfig = TestConfiguration.OS.LINUX;
		}
		
		spawnClient(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);
		Time.sleepWrapped(1000);
		spawnClient(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);
		
		TestConfiguration testConfig = new TestConfiguration(osConfig, "python", "HTTPS");
		testConfig.setTestTwoClients(true);
		testConfig.setTestSecondaryClient(true);
		RunnerTestGeneric.test(testConfig);
		
		TestConfiguration configParent = new TestConfiguration(osConfig, "python", "HTTPS");
		configParent.setTestTwoClients(true);
		RunnerTestGeneric.test(configParent);
		
		awaitClient();
		teardown();
	}

}
