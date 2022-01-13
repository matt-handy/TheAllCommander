package c2.python;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;

import c2.smtp.EmailHandlerTester;
import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConstants;

public class RunnerTestPythonEmail  extends ClientServerTest {

	@Test
	void test() {
		testEmail();
	}

	public static void testEmail() {
		System.out.println("Warning: The email based protocol is currently partially lossy, and will often not make it through the full automated sequence");
		EmailHandlerTester.flushC2Emails();
		initiateServer();
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "emailAgent.py\"";
		spawnClient(clientCmd);
		
		TestConfiguration testConfig = new TestConfiguration(TestConfiguration.OS.WINDOWS, "python", "SMTP");
		RunnerTestGeneric.test(testConfig);
		
		teardown();
	}
}
