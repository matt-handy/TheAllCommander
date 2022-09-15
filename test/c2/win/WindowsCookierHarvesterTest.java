package c2.win;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import c2.session.CommandMacroManager;
import util.Time;
import util.test.ClientServerTest;
import util.test.HarvestTestHelper;
import util.test.RunnerTestGeneric;
import util.test.TestCommons;
import util.test.TestConstants;

class WindowsCookierHarvesterTest extends ClientServerTest {

	//Are all cookies available?
	boolean canAttemptTest() {
		return Files.exists(Paths.get(CookiesCommandHelper.getChromeCookiesFilename())) && Files.exists(Paths.get(CookiesCommandHelper.getEdgeCookiesFilename())) && Files.exists(Paths.get(CookiesCommandHelper.getFirefoxCookiesFilename()));
	}
	
	@Test
	void testPythonHTTPS() {
		if (System.getProperty("os.name").contains("Windows") && canAttemptTest()) {
			initiateServer();
			spawnClient(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);

			try {
				HarvestTestHelper.testCookieHarvestBody(TestCommons.LANGUAGE.PYTHON);
			}catch(InterruptedException ex) {
				fail(ex.getMessage());
			}
			
			teardown();
		}
	}
	
	@Test
	void testNative() {
		if (System.getProperty("os.name").contains("Windows") && canAttemptTest()) {
			initiateServer();
			spawnClient(TestConstants.WINDOWSNATIVE_TEST_EXE);

			try {
				HarvestTestHelper.testCookieHarvestBody(TestCommons.LANGUAGE.WINDOWS_NATIVE);
			}catch(InterruptedException ex) {
				fail(ex.getMessage());
			}
			
			teardown();
		}
	}

	
	
}
