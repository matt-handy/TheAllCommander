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
import c2.smtp.EmailHandler;
import c2.smtp.SimpleEmail;
import util.test.ClientServerTest;
import util.test.EmailHelper;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConstants;
import util.test.TestConfiguration.OS;

public class PythonEmailAllCommandsTest  extends ClientServerTest {

	@AfterEach
	void cleanup() {
		//teardown();
	}
	
	@Test
	void testEmail() {
		System.out.println("Windows Email test currently disabled for stability reasons");
		/*
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
		System.out.println("Warning: The email based protocol is currently partially lossy, and will often not make it through the full automated sequence");
		EmailHelper.flushC2Emails();
		initiateServer();
		spawnClient(TestConstants.PYTHON_SMTPDAEMON_TEST_EXE);
		
		TestConfiguration testConfig = new TestConfiguration(TestConfiguration.OS.WINDOWS, "python", "SMTP");
		RunnerTestGeneric.test(testConfig);
		awaitClient();
		*/
	}
	
}
