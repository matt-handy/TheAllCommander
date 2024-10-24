package c2.session.macro.enumeration;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import c2.Commands;
import c2.admin.LocalConnection;
import c2.session.macro.enumeration.cve.WindowsPrivescCVE;
import util.Time;
import util.test.ClientServerTest;
import util.test.OutputStreamWriterHelper;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConstants;

class AuditAllTheThingsIntegrationTest extends ClientServerTest {

	@AfterEach
	void clean() {
		awaitClient();
		teardown();
		WindowsUnencryptedConfigurationPasswordAuditorTest.cleanupSamplePasswordFile();
	}
	
	@Test
	void testPython() {
		testLiveRunner(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);
	}
	
	@Test
	void testNativeShell() {
		testLiveRunner(TestConstants.WINDOWSNATIVE_TEST_EXE);
	}

	void testLiveRunner(String client) {
		TestConfiguration.OS osConfig = TestConfiguration.getThisSystemOS();
		if (osConfig == TestConfiguration.OS.WINDOWS) {
			initiateServer();
			spawnClient(client);
			try {
				WindowsUnencryptedConfigurationPasswordAuditorTest.writeSamplePasswordFile();
				Socket remote = LocalConnection.getSocket("127.0.0.1", 8012,
						ClientServerTest.getDefaultSystemTestProperties());
				OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
				BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

				// Ensure that python client has connected
				Time.sleepWrapped(500);

				RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, osConfig == TestConfiguration.OS.LINUX, false);

				OutputStreamWriterHelper.writeAndSend(bw, AuditAllTheThingsMacro.CMD);
				
				assertEquals("Macro Executor: '" + WindowsPrivescCVE.READABLE_NAME + ": No findings'", br.readLine());
				assertEquals("Macro Executor: 'Unauthorized Priviledge Escalation Audit Complete, No Findings'", br.readLine());
				//Assumes that dev systems do not have LSA protection enabled
				assertEquals("Audit Finding: 'Warning: LSA protection may not be enabled. LSA protection provides additional protection against credential dumping and should be enabled if possible. Check the following registry settings to validate REG QUERY HKLM\\SYSTEM\\CurrentControlSet\\Control\\LSA /v RunAsPPL'", br.readLine());
				assertEquals("Audit Finding: 'Warning: LSA Credential Guard protection may not be enabled. LSA protection provides additional protection against credential dumping and should be enabled if possible. Check the following registry settings to validate REG QUERY HKLM\\SYSTEM\\CurrentControlSet\\Control\\LSA /v LsaCfgFlags'", br.readLine());
				
				boolean haveSeenTestData = false;
				String line = br.readLine();
				while(!line.equals("Macro Executor: 'Unencrypted Password Audit Complete'")) {
					if(line.contains("Cannot execute, errors encountered:")) {
						fail("Macro error'd out");
					}else if(line.startsWith("Audit Finding") && line.contains(WindowsUnencryptedConfigurationPasswordAuditorTest.testPasswordPath.toString())) {
						haveSeenTestData = true;
					}
					line = br.readLine();
				}
				assertTrue(haveSeenTestData, "We didn't find the test data that we were looking for: " + WindowsUnencryptedConfigurationPasswordAuditorTest.testPasswordPath.toString());
				
				line = br.readLine();
				while(line != null && !line.equals("Macro Executor: 'Windows DLL Permissions Integrity Auditor Complete'")) {
					//Assumes that the test PC will have no vulnerable processes, but may have vulnerable PATH settings
					assertTrue(line.startsWith("Audit Finding: 'Warning: user can modify path directory:"), "The following is not a PATH warning: "+ line);
					line=br.readLine();
				}
				
				OutputStreamWriterHelper.writeAndSend(bw, Commands.CLIENT_CMD_SHUTDOWN_DAEMON);
				//Client receive message
				Time.sleepWrapped(2000);
			} catch (Exception ex) {
				fail(ex.getMessage());
			}
		}
	}
	
}
