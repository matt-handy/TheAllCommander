package c2.filereceiver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import util.test.ClientServerTest;
import util.test.HarvestTestHelper;
import util.test.TestConfiguration;
import util.test.TestConstants;

class HarvesterIntegrationTest  extends ClientServerTest {

	@AfterEach
	@BeforeEach
	void clean() {
		HarvestTestHelper.cleanup();
	}
	
	@Test
	void testHTTPSPythonDataExfil() {
		if (System.getProperty("os.name").contains("Windows")) {
			initiateServer();
		} else {
			initiateServer("test_linux.properties");
		}
		spawnClient(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);

		TestConfiguration.OS osConfig = TestConfiguration.getThisSystemOS();
		
		TestConfiguration testConfig = new TestConfiguration(osConfig, "python", "HTTPS");
		HarvestTestHelper.testDataExfilBody(testConfig);
		
		awaitClient();
		teardown();
	}
	
	@Test
	void testDNSPythonDataExfil() {
		if (System.getProperty("os.name").contains("Windows")) {
			initiateServer();
		} else {
			return;
			//No need to test on linux at this time. The platform dependent piece is tested with the HTTPS element, and the 
			//remaining control flow logic here is platform independent.
			//initiateServer("test_linux.properties");
		}
		spawnClient(TestConstants.PYTHON_DNSDAEMON_TEST_EXE);

		TestConfiguration.OS  osConfig = TestConfiguration.getThisSystemOS();
		
		TestConfiguration testConfig = new TestConfiguration(osConfig, "python", "DNS");
		HarvestTestHelper.testDataExfilBody(testConfig);
		
		awaitClient();
		teardown();
	}

	

	
}
