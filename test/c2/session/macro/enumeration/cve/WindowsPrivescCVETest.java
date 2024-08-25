package c2.session.macro.enumeration.cve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import c2.Commands;
import c2.Constants;
import c2.WindowsConstants;
import c2.admin.LocalConnection;
import c2.session.CommandLoader;
import c2.session.IOManager;
import c2.session.log.IOLogger;
import c2.session.macro.MacroOutcome;
import c2.win.WindowsPatchLevelCVECheckerTest;
import util.Time;
import util.test.ClientServerTest;
import util.test.OutputStreamWriterHelper;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConstants;

class WindowsPrivescCVETest extends ClientServerTest {

	IOManager io;
	int sessionId;

	@BeforeEach
	void setUp() throws Exception {
		Properties prop = ClientServerTest.getDefaultSystemTestProperties();
		CommandLoader cl = new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>());
		io = new IOManager(new IOLogger(Paths.get(prop.getProperty(Constants.HUBLOGGINGPATH))), cl);

		sessionId = io.determineAndGetCorrectSessionId("noone", "testHost", "protocol", false, null);
	}

	@AfterEach
	void shutdown() {
		awaitClient();
		teardown();
	}

	@Test
	void testDetectsCommand() {
		WindowsPrivescCVE macro = new WindowsPrivescCVE();
		assertTrue(macro.isCommandMatch(WindowsPrivescCVE.CMD));
		assertFalse(macro.isCommandMatch(WindowsPrivescCVE.CMD + " arg"));
		assertFalse(macro.isCommandMatch("something_else"));
	}

	@Test
	void testGivesCorrectCVEAudit() {
		io.sendIO(sessionId, WindowsPatchLevelCVECheckerTest.SAMPLE_SYSTEMINFO_CVE_2019_0836);
		WindowsPrivescCVE macro = new WindowsPrivescCVE();
		macro.initialize(io, null);

		MacroOutcome outcome = macro.processCmd(WindowsPrivescCVE.CMD, sessionId, "ignored");
		assertEquals(1, outcome.getAuditFindings().size());
		assertEquals("Audit Finding: 'Applicable CVE: CVE-2019-0836'", outcome.getAuditFindings().get(0));
		assertEquals(1, outcome.getOutput().size());
		assertEquals("Audit Finding: 'Applicable CVE: CVE-2019-0836'", outcome.getAuditFindings().get(0));
	}

	@Test
	void testReturnsNoCVEs() {
		// Note: this test assumes the dev machine is fully patched and there are no
		// audit findings. If the test fails, PATCH YOUR SYSTEM!!!

		TestConfiguration.OS osConfig = TestConfiguration.getThisSystemOS();
		if (osConfig == TestConfiguration.OS.WINDOWS) {
			initiateServer();
			spawnClient(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);
			try {
				Socket remote = LocalConnection.getSocket("127.0.0.1", 8012,
						ClientServerTest.getDefaultSystemTestProperties());
				OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
				BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

				// Ensure that python client has connected
				Time.sleepWrapped(500);

				RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, osConfig == TestConfiguration.OS.LINUX, false);
				
				OutputStreamWriterHelper.writeAndSend(bw, WindowsPrivescCVE.CMD);
				assertEquals("Macro Executor: 'No findings'", br.readLine());

				OutputStreamWriterHelper.writeAndSend(bw, Commands.CLIENT_CMD_SHUTDOWN_DAEMON);
			} catch (Exception ex) {
				fail(ex.getMessage());
			}
		}

	}

}
