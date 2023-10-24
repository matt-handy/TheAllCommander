package c2.python;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import c2.Constants;
import util.Time;
import util.test.ClientServerTest;
import util.test.EmailHelper;
import util.test.KeyLoggerTest;
import util.test.TestCommons;
import util.test.TestConfiguration;
import util.test.TestConstants;
import util.test.TestConfiguration.OS;

public class RunnerTestKeyloggerEmail extends ClientServerTest {

	@AfterEach
	void testLocal() {
		//teardown();
	}

	@Test
	void test() {
		System.out.println("Windows Email Keylogger test currently disabled for stability reasons");
		/*
		if (TestConstants.KEYLOGGER_TEST_ENABLE) {
			if (TestConfiguration.getThisSystemOS() == OS.WINDOWS) {
				Properties prop = EmailHelper.setup();
				if (!prop.getProperty(Constants.COMMSERVICES).contains("EmailHandler")) {
					System.out.println("TheAllCommander not configured for email operations, skipping test.");
					return;
				}
			} else {
				System.out.println("SMTP integration not currently supported on Linux and Mac.");
				return;
			}

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
			spawnClient(TestConstants.PYTHON_SMTPDAEMON_TEST_EXE);

			Time.sleepWrapped(5000);

			KeyLoggerTest.testGenericKeylogger(prop, true);
			awaitClient();
		}
	*/
	}
}
