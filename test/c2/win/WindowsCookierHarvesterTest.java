package c2.win;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import util.test.ClientServerTest;
import util.test.HarvestTestHelper;
import util.test.TestCommons;
import util.test.TestConfiguration;
import util.test.TestConstants;
import util.test.TestConfiguration.OS;

class WindowsCookierHarvesterTest extends ClientServerTest {

	@Test
	void testPythonHTTPS() {
		if (TestConfiguration.getThisSystemOS() == OS.WINDOWS && HarvestTestHelper.canAttemptTest()) {
			initiateServer();
			spawnClient(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);

			try {
				HarvestTestHelper.testCookieHarvestBody(TestCommons.LANGUAGE.PYTHON);
			}catch(InterruptedException ex) {
				fail(ex.getMessage());
			}
			
			awaitClient();
			teardown();
		}
	}
	
	@Test
	void testNative() {
		if (TestConfiguration.getThisSystemOS() == OS.WINDOWS && HarvestTestHelper.canAttemptTest()) {
			initiateServer();
			spawnClient(TestConstants.WINDOWSNATIVE_TEST_EXE);

			try {
				HarvestTestHelper.testCookieHarvestBody(TestCommons.LANGUAGE.WINDOWS_NATIVE);
			}catch(InterruptedException ex) {
				fail(ex.getMessage());
			}
			
			awaitClient();
			teardown();
		}
	}

	
	
}
