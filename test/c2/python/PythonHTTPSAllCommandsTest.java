package c2.python;

import org.junit.jupiter.api.Test;

import util.Time;
import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConstants;

class PythonHTTPSAllCommandsTest extends ClientServerTest {

	@Test
	void testHTTPS() {
		TestConfiguration.OS osConfig = TestConfiguration.getThisSystemOS();
		
		TestConfiguration testConfig = new TestConfiguration(osConfig, "python", "HTTPS");
		executeStandardTest(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE, testConfig);
	}
	
	@Test
	void testHTTPSTwoSessions() {
		initiateServer();
		TestConfiguration.OS osConfig = TestConfiguration.getThisSystemOS();
		
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
