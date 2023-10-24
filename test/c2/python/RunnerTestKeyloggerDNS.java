package c2.python;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import util.Time;
import util.test.ClientServerTest;
import util.test.KeyLoggerTest;
import util.test.TestCommons;
import util.test.TestConstants;

public class RunnerTestKeyloggerDNS extends ClientServerTest {

	@AfterEach
	void cleanup() {
		teardown();
	}

	@Test
	void test() {
		if (TestConstants.KEYLOGGER_TEST_ENABLE) {
			TestCommons.cleanupKeylogger();

			Properties prop = new Properties();
			try (InputStream input = new FileInputStream(
					"config" + File.separator + ClientServerTest.DEFAULT_SERVER_CONFIG)) {

				// load a properties file
				prop.load(input);

			} catch (IOException ex) {
				System.out.println("Unable to load config file");
				fail(ex.getMessage());
			}

			initiateServer();
			spawnClient(TestConstants.PYTHON_DNSDAEMON_TEST_EXE);

			Time.sleepWrapped(5000);

			KeyLoggerTest.testGenericKeylogger(prop, false);
		}
	}

}
