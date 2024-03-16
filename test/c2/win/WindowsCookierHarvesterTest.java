package c2.win;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import util.test.ClientServerTest;
import util.test.HarvestTestHelper;
import util.test.TestCommons;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;
import util.test.TestConstants;

class WindowsCookierHarvesterTest extends ClientServerTest {

	@AfterEach
	void stop(){
		awaitClient();
		teardown();
	}
	
	@Test
	void testPythonHTTPS() {
		if (TestConfiguration.getThisSystemOS() == OS.WINDOWS && HarvestTestHelper.canAttemptTest() && HarvestTestHelper.systemHasAllFirefoxDependencies()) {
			initiateServer();
			spawnClient(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);

			try {
				HarvestTestHelper.testCookieHarvestBody(TestCommons.LANGUAGE.PYTHON);
			}catch(InterruptedException ex) {
				fail(ex.getMessage());
			}
		}
	}
	
	@Test
	void testNative() {
		if (TestConfiguration.getThisSystemOS() == OS.WINDOWS && HarvestTestHelper.canAttemptTest()
				&& HarvestTestHelper.systemHasAllFirefoxDependencies()) {
			initiateServer();
			spawnClient(TestConstants.WINDOWSNATIVE_TEST_EXE);

			try {
				HarvestTestHelper.testCookieHarvestBody(TestCommons.LANGUAGE.WINDOWS_NATIVE);
			}catch(InterruptedException ex) {
				fail(ex.getMessage());
			}
		}
	}

	
	
}
