package c2.portforward.socks;

import org.junit.jupiter.api.Test;

import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;
import util.test.TestConstants;
import util.test.socks5.TestProcessor;

class PythonIntegrationTest {

	@Test
	void testHTTPSNominal() {
		TestConfiguration.OS thisOs = TestConfiguration.getThisSystemOS();
		//Tests not yet validated on Linux
				if (thisOs == OS.WINDOWS) {
					System.out.println("Testing nominal HTTPS SOCKS5");
		TestProcessor.testDaemonConnection(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE, false, false);
				}else if(thisOs == OS.MAC) {
					System.out.println("SOCKS5 not yet supported on Mac");
				}
	}
	
	@Test
	void testHTTPSBrokenConnections() {
		TestConfiguration.OS thisOs = TestConfiguration.getThisSystemOS();
		//Tests not yet validated on Linux
		if (thisOs == OS.WINDOWS) {
					System.out.println("Testing Broken HTTPS SOCKS5");
		//TestProcessor.testDaemonConnection(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE, true, false);
				}else if(thisOs == OS.MAC) {
					System.out.println("SOCKS5 not yet supported on Mac");
				}
	}
	
	@Test
	void testDNSNominal() {
		TestConfiguration.OS thisOs = TestConfiguration.getThisSystemOS();
		//Tests not yet validated on Linux
		if (thisOs == OS.WINDOWS) {
					System.out.println("Testing nominal DNS SOCKS5");
		TestProcessor.testDaemonConnection(TestConstants.PYTHON_DNSDAEMON_TEST_EXE, false, false);
				}
	else if(thisOs == OS.MAC) {
		System.out.println("SOCKS5 not yet supported on Mac");
	}
	}
	
	@Test
	void testDNSBrokenConnections() {
		TestConfiguration.OS thisOs = TestConfiguration.getThisSystemOS();
		//Tests not yet validated on Linux
		if (thisOs == OS.WINDOWS) {
		//TestProcessor.testDaemonConnection(TestConstants.PYTHON_DNSDAEMON_TEST_EXE, true, false);
		}else if(thisOs == OS.MAC) {
			System.out.println("SOCKS5 not yet supported on Mac");
		}
	}
	
	@Test
	void testEmailDaemonNominal() {
		/*
		if (System.getProperty("os.name").contains("Windows") && EmailHelper.setup().getProperty(Constants.COMMSERVICES).contains("EmailHandler")) {
			EmailHelper.flushC2Emails();
			TestProcessor.testDaemonConnection(TestConstants.PYTHON_SMTPDAEMON_TEST_EXE, false, true);
		}
		*/
	}
	
	@Test
	void testEmailDaemonBrokenConnections() {
		/*
		if (System.getProperty("os.name").contains("Windows") && EmailHelper.setup().getProperty(Constants.COMMSERVICES).contains("EmailHandler")) {
			EmailHelper.flushC2Emails();
			TestProcessor.testDaemonConnection(TestConstants.PYTHON_SMTPDAEMON_TEST_EXE, true, true);
		}
		*/
	}

}
