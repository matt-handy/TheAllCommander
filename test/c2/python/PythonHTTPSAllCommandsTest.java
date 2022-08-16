package c2.python;

import org.junit.jupiter.api.Test;

import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConstants;

class PythonHTTPSAllCommandsTest extends ClientServerTest {

	@Test
	void testHTTPS() {
		if (System.getProperty("os.name").contains("Windows")) {
			initiateServer();
		} else {
			initiateServer("test_linux.properties");
		}
		spawnClient(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);

		TestConfiguration.OS osConfig = null;
		if (System.getProperty("os.name").contains("Windows")) {
			osConfig = TestConfiguration.OS.WINDOWS;
		} else {
			osConfig = TestConfiguration.OS.LINUX;
		}

		TestConfiguration testConfig = new TestConfiguration(osConfig, "python", "HTTPS");
		RunnerTestGeneric.test(testConfig);

		teardown();
	}

}
