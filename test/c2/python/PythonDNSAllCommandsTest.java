package c2.python;

import org.junit.jupiter.api.Test;

import util.Time;
import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConstants;

class PythonDNSAllCommandsTest extends ClientServerTest {

	@Test
	void testDNS() {
		TestConfiguration.OS osConfig = TestConfiguration.getThisSystemOS();

		TestConfiguration testConfig = new TestConfiguration(osConfig, "python", "DNS");
		executeStandardTest(TestConstants.PYTHON_DNSDAEMON_TEST_EXE, testConfig);
	}

	@Test
	void testDNSTwoSessions() {
		TestConfiguration.OS osConfig = TestConfiguration.getThisSystemOS();

		initiateServer();
		spawnClient(TestConstants.PYTHON_DNSDAEMON_TEST_EXE);
		Time.sleepWrapped(1000);
		spawnClient(TestConstants.PYTHON_DNSDAEMON_TEST_EXE);

		TestConfiguration testConfig = new TestConfiguration(osConfig, "python", "DNS");
		testConfig.setTestTwoClients(true);
		testConfig.setTestSecondaryClient(true);
		RunnerTestGeneric.test(testConfig);

		TestConfiguration configParent = new TestConfiguration(osConfig, "python", "DNS");
		configParent.setTestTwoClients(true);
		RunnerTestGeneric.test(configParent);

		awaitClient();
		teardown();

	}

}
