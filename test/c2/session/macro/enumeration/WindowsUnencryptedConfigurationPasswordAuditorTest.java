package c2.session.macro.enumeration;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import c2.Commands;
import c2.admin.LocalConnection;
import util.Time;
import util.test.ClientServerTest;
import util.test.OutputStreamWriterHelper;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConstants;

class WindowsUnencryptedConfigurationPasswordAuditorTest extends ClientServerTest{

	@AfterEach
	void clean() {
		awaitClient();
		teardown();
		cleanupSamplePasswordFile();
	}
	
	public static final Path testPasswordPath = Paths.get("TAC_PASSWORD_TEST.xml");
	
	@Test
	void testCommandMatches() {
		WindowsUnencryptedConfigurationPasswordAuditor macro = new WindowsUnencryptedConfigurationPasswordAuditor();
		assertTrue(macro.isCommandMatch(WindowsUnencryptedConfigurationPasswordAuditor.CMD));
		assertFalse(macro.isCommandMatch(WindowsUnencryptedConfigurationPasswordAuditor.CMD + " narf"));
		assertFalse(macro.isCommandMatch("barf"));
	}

	public static void writeSamplePasswordFile() throws IOException {
		Files.writeString(testPasswordPath, "<password>THIS_IS_FAKE_TEST_DATA</password>");
	}
	
	public static void cleanupSamplePasswordFile() {
		try {
			Files.deleteIfExists(testPasswordPath);
		} catch (IOException e) {
			//keep going!
		}
	}
	
	void testLiveRunner(String client) {
		TestConfiguration.OS osConfig = TestConfiguration.getThisSystemOS();
		if (osConfig == TestConfiguration.OS.WINDOWS) {
			initiateServer();
			spawnClient(client);
			try {
				writeSamplePasswordFile();
				Socket remote = LocalConnection.getSocket("127.0.0.1", 8012,
						ClientServerTest.getDefaultSystemTestProperties());
				OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
				BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

				// Ensure that python client has connected
				Time.sleepWrapped(500);

				RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, osConfig == TestConfiguration.OS.LINUX, false);

				OutputStreamWriterHelper.writeAndSend(bw, WindowsUnencryptedConfigurationPasswordAuditor.CMD);
				
				String line = br.readLine();
				boolean haveSeenTestData = false;
				while(!line.equals("Macro Executor: 'Unencrypted Password Audit Complete'")) {
					if(line.contains("Cannot execute, errors encountered:")) {
						fail("Macro error'd out");
					}else if(line.startsWith("Audit Finding") && line.contains(testPasswordPath.toString())) {
						haveSeenTestData = true;
					}
					line = br.readLine();
				}
				assertTrue(haveSeenTestData, "We didn't find the test data that we were looking for: " + testPasswordPath.toString());

				OutputStreamWriterHelper.writeAndSend(bw, Commands.CLIENT_CMD_SHUTDOWN_DAEMON);
				//Client receive message
				Time.sleepWrapped(2000);
			} catch (Exception ex) {
				fail(ex.getMessage());
			}
		}
	}
	
	@Test
	void testLiveWithPython() {
		testLiveRunner(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);
	}
	
	@Test
	void testLiveWithText() {
		testLiveRunner(TestConstants.WINDOWSNATIVE_TEST_EXE);
	}
	
}
