package c2.portforward.socks;

import java.io.File;

import org.junit.jupiter.api.Test;

import c2.Constants;
import util.test.EmailHelper;
import util.test.TestConstants;
import util.test.socks5.TestProcessor;

class PythonIntegrationTest {

	@Test
	void testHTTPSNominal() {
		//Tests not yet validated on Linux
				if (System.getProperty("os.name").contains("Windows")) {
		TestProcessor.testDaemonConnection(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE, false, false);
				}
	}
	
	@Test
	void testHTTPSBrokenConnections() {
		//Tests not yet validated on Linux
				if (System.getProperty("os.name").contains("Windows")) {
		TestProcessor.testDaemonConnection(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE, true, false);
				}
	}
	
	@Test
	void testDNSNominal() {
		//Tests not yet validated on Linux
				if (System.getProperty("os.name").contains("Windows")) {
		TestProcessor.testDaemonConnection(TestConstants.PYTHON_DNSDAEMON_TEST_EXE, false, false);
				}
	}
	
	@Test
	void testDNSBrokenConnections() {
		if (System.getProperty("os.name").contains("Windows")) {
		//TestProcessor.testDaemonConnection(TestConstants.PYTHON_DNSDAEMON_TEST_EXE, true, false);
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
