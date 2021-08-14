package c2;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import c2.nativeshell.RunnerTestNativeLinuxDaemon;
import c2.nativeshell.RunnerTestNativeWindowsDaemon;
import c2.portforward.PythonPortForwardTest;
import c2.python.RunnerTestKeyloggerDNS;
import c2.python.RunnerTestKeyloggerEmail;
import c2.python.RunnerTestKeyloggerHTTPS;
import c2.python.RunnerTestPython;
import c2.rdp.RunnerTestPythonHTTPSDaemonWinRDP;
import c2.smtp.EmailHandlerTester;
import c2.udp.UDPServerTest;
import c2.win.RunnerTestDaemonHarvestCookiesNative;

class MasterLinearTest {

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test() {
		CommanderInterfaceTest.test();
		
		//Daemon tests
		//Protocol verification tests
		UDPServerTest.test();
		EmailHandlerTester.test();
		
		//Test Keyloggers
		RunnerTestKeyloggerHTTPS.test();
		RunnerTestKeyloggerDNS.test();
		RunnerTestKeyloggerEmail.test();
		
		//Testing Linux
		System.out.println("Testing Linux Native Shell");
		RunnerTestNativeLinuxDaemon.test();
		
		//Native daemon tests
		System.out.println("Testing Windows Native");
		RunnerTestNativeWindowsDaemon.test();
		
		//Python
		System.out.println("Testing Python");
		RunnerTestPython.testHTTPS();
		RunnerTestPython.testDNS();
		RunnerTestPython.testEmail();
		
		//Port forward
		PythonPortForwardTest.testHTTPS();
		PythonPortForwardTest.testDNS();
		PythonPortForwardTest.testEmail();
		
		//Macro command execution tests
		try {
			RunnerTestDaemonHarvestCookiesNative.testAll();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		RunnerTestPythonHTTPSDaemonWinRDP.test();
	}

}
