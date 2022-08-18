package c2.python;

import org.junit.jupiter.api.Test;

import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConstants;

class PythonDNSAllCommandsTest extends ClientServerTest {

	@Test
	void testDNS() {
		if (System.getProperty("os.name").contains("Windows")) {
			initiateServer();
		} else {
			initiateServer("test_linux.properties");
		}
		spawnClient(TestConstants.PYTHON_DNSDAEMON_TEST_EXE);

		TestConfiguration.OS osConfig = null;
		if (System.getProperty("os.name").contains("Windows")) {
			osConfig = TestConfiguration.OS.WINDOWS;
		} else {
			osConfig = TestConfiguration.OS.LINUX;
		}

		TestConfiguration testConfig = new TestConfiguration(osConfig, "python", "DNS");
		RunnerTestGeneric.test(testConfig);
		teardown();
	}

}
