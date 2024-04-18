package c2.session.macro.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import c2.Commands;
import c2.Constants;
import c2.admin.LocalConnection;
import util.test.ClientServerTest;
import util.test.OutputStreamWriterHelper;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;
import util.test.TestConstants;

class WinlogonMacroTest extends ClientServerTest {

	@AfterEach
	void clean() {
		awaitClient();
		teardown();
	}

	@Test
	void testRealFailureOnOS() {
		if (TestConstants.WINLOGON_REG_LIVE_DENIAL_ENABLE && TestConfiguration.getThisSystemOS() == OS.WINDOWS) {
			initiateServer();
			spawnClient(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);

			try {
				System.out.println("Connecting test commander...");
				Socket remote = LocalConnection.getSocket("127.0.0.1", Integer.parseInt(
						ClientServerTest.getDefaultSystemTestProperties().getProperty(Constants.SECURECOMMANDERPORT)),
						getDefaultSystemTestProperties());
				System.out.println("Locking test commander streams...");
				OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
				BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

				RunnerTestGeneric.connectionSetupGeneric(remote, bw, br,
						TestConfiguration.getThisSystemOS() == OS.LINUX, false);
				
				OutputStreamWriterHelper.writeAndSend(bw, WinlogonMacro.COMMAND);
				
				assertEquals("Cannot execute, errors encountered: ", br.readLine());
				assertEquals("Unable to modify registry key, insufficient permissions.", br.readLine());
				assertEquals("Sent Command: 'get_daemon_start_cmd'", br.readLine());
				String response = br.readLine();
				assertTrue(response.contains("python.exe"));
				assertTrue(response.endsWith("httpsAgent.py"));
				assertEquals("", br.readLine());
				assertEquals("'", br.readLine());
				response = br.readLine();
				assertTrue(response.contains("python.exe"));
				assertTrue(response.contains("httpsAgent.py"));
				assertTrue(response.startsWith("Sent Command: 'reg add \"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Winlogon"));
				assertEquals("Received response: '", br.readLine());
				assertEquals("", br.readLine());
				assertEquals("'", br.readLine());
				assertEquals("Error: Unable to modify registry key, insufficient permissions.", br.readLine());
				
				OutputStreamWriterHelper.writeAndSend(bw, Commands.CLIENT_CMD_SHUTDOWN_DAEMON);
			} catch (Exception ex) {
				fail("Cannot communicate with daemon");
			}
		}
	}

}
