package c2.nativeshell;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;

public class RunnerTestNativeWindowsMSFUnstaged extends ClientServerTest{

	
	@AfterEach
	void cleanup() {
		//teardown();
	}
	
	@Test
	void test(){
		System.out.println("This test must be manually enabled if a windows/x64/shell_reverse_tcp has been generated");
		/*
		//This test is designed to validate that MSF windows/x64/shell_reverse_tcp payloads
		//will be received and work with this system
		initiateServer();
		System.out.println("Transmitting commands");
		
		TestConfiguration config = new TestConfiguration(OS.WINDOWS, "Native", "TCP");
		config.setRemote(true);
		RunnerTestGeneric.test(config);
		*/
	}

}
