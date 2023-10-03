package c2;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import c2.python.RunnerTestKeyloggerDNS;
import c2.python.RunnerTestKeyloggerEmail;
import c2.python.RunnerTestKeyloggerHTTPS;
import c2.python.RunnerTestPythonEmail;
import c2.win.RunnerTestPythonHTTPSDaemonWinRDP;

class MasterLinearTest {

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test() {
		//Test Keyloggers
		RunnerTestKeyloggerHTTPS.test();
		RunnerTestKeyloggerDNS.test();
		RunnerTestKeyloggerEmail.test();
		
		//Python
		System.out.println("Testing Python");
		RunnerTestPythonEmail.testEmail();
		
		RunnerTestPythonHTTPSDaemonWinRDP.test();
	}

}
