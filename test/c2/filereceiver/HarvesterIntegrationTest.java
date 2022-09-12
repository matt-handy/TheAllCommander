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
			return;
			//initiateServer("test_linux.properties");
		}
		spawnClient(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);

		TestConfiguration.OS osConfig = null;
		if (System.getProperty("os.name").contains("Windows")) {
			osConfig = TestConfiguration.OS.WINDOWS;
		} else {
			osConfig = TestConfiguration.OS.LINUX;
		}
		TestConfiguration testConfig = new TestConfiguration(osConfig, "python", "HTTPS");
		HarvestTestHelper.testDataExfilBody(testConfig);
		
		teardown();
	}
	
	@Test
	void testDNSPythonDataExfil() {
		if (System.getProperty("os.name").contains("Windows")) {
			initiateServer();
		} else {
			return;
			//initiateServer("test_linux.properties");
		}
		spawnClient(TestConstants.PYTHON_DNSDAEMON_TEST_EXE);

		TestConfiguration.OS osConfig = null;
		if (System.getProperty("os.name").contains("Windows")) {
			osConfig = TestConfiguration.OS.WINDOWS;
		} else {
			osConfig = TestConfiguration.OS.LINUX;
		}
		TestConfiguration testConfig = new TestConfiguration(osConfig, "python", "DNS");
		HarvestTestHelper.testDataExfilBody(testConfig);
		
		teardown();
	}

	

	
}
