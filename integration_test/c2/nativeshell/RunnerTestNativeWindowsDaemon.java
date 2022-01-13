package c2.nativeshell;


import org.junit.jupiter.api.Test;

import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;
import util.test.TestConstants;

public class RunnerTestNativeWindowsDaemon extends ClientServerTest{

	
	@Test
	void testLocal() {
		test();
	}
	
	public static void test(){
		
		initiateServer();
		spawnClient(TestConstants.WINDOWSNATIVE_TEST_EXE);
		System.out.println("Transmitting commands");
		
		TestConfiguration config = new TestConfiguration(OS.WINDOWS, "Native", "TCP");
		RunnerTestGeneric.test(config);
		teardown();
	}

}
