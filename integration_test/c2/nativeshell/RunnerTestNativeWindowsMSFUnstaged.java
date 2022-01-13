package c2.nativeshell;


import org.junit.jupiter.api.Test;

import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;

public class RunnerTestNativeWindowsMSFUnstaged extends ClientServerTest{

	
	@Test
	void testLocal() {
		test();
	}
	
	public static void test(){
		//This test is designed to validate that MSF windows/x64/shell_reverse_tcp payloads
		//will be received and work with this system
		initiateServer();
		System.out.println("Transmitting commands");
		
		TestConfiguration config = new TestConfiguration(OS.WINDOWS, "Native", "TCP");
		config.setRemote(true);
		RunnerTestGeneric.test(config);
		teardown();
	}

}
