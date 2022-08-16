package c2.nativeshell;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;
import util.test.TestConstants;

class NcatIntegrationTest extends ClientServerTest {

	@Test
	void test() {
		try {
			Runtime.getRuntime().exec("ncat");
		} catch (IOException e) {
			System.out.println("ncat not available on this host, skipping ncat integration test sequence");
			return;
		}
		if (System.getProperty("os.name").contains("Windows")) {
			initiateServer();
			spawnClient(TestConstants.WINDOWSNATIVE_TEST_EXE);
			TestConfiguration config = new TestConfiguration(OS.WINDOWS, "Native", "TCP");
			RunnerTestGeneric.test(config);
			teardown();
		} else {
			// initiateServer();
			// spawnClient("ncat localhost 8003 -e /bin/bash");
			// TestConfiguration config = new TestConfiguration(OS.LINUX, "Native", "TCP");
			// RunnerTestGeneric.test(config);
			// teardown();
		}
	}

}
