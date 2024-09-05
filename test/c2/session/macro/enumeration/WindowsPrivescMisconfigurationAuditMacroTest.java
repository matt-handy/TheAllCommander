package c2.session.macro.enumeration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import c2.Commands;
import c2.Constants;
import c2.admin.LocalConnection;
import c2.session.CommandLoader;
import c2.session.IOManager;
import c2.session.log.IOLogger;
import c2.session.macro.MacroOutcome;
import c2.win.WindowsQuotedServiceChecker;
import c2.win.WindowsQuotedServiceCheckerTest;
import c2.win.WindowsUserPriviledgeParserTest;
import util.Time;
import util.test.ClientServerTest;
import util.test.OutputStreamWriterHelper;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConstants;

class WindowsPrivescMisconfigurationAuditMacroTest extends ClientServerTest {

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

	private class ClientStartCmdEmulator implements Runnable {

		private IOManager session;
		private int sessionId;
		private boolean alive = true;
		private boolean giveCleanServices;
		private boolean giveCleanPrivs;

		public ClientStartCmdEmulator(int sessionid, IOManager session, boolean giveCleanServices,
				boolean giveCleanPrivs) {
			this.session = session;
			this.sessionId = sessionid;
			this.giveCleanServices = giveCleanServices;
			this.giveCleanPrivs = giveCleanPrivs;
		}

		public void kill() {
			alive = false;
		}

		public void run() {
			while (alive) {
				String command = session.pollCommand(sessionId);
				if (command == null) {
					// continue
					Time.sleepWrapped(10);
				} else if (command.equalsIgnoreCase(WindowsQuotedServiceChecker.SERVICE_PATH_QUERY)) {
					if (giveCleanServices) {
						session.sendIO(sessionId, WindowsQuotedServiceCheckerTest.ALL_QUOTED_SVCS);
					} else {
						session.sendIO(sessionId, WindowsQuotedServiceCheckerTest.MISSING_QUOTED_SVCS);
					}
				} else {
					// We got the command for the first FileInfoCall. We can close now after sending
					// results.
					if (giveCleanPrivs) {
						// We want to trigger the warning for MS10-092 and MS14-058
						session.sendIO(sessionId, WindowsUserPriviledgeParserTest.NO_PRIV);
					} else {
						// These numbers don't really matter when we aren't trying to trigger the vuln
						// detection
						session.sendIO(sessionId, WindowsUserPriviledgeParserTest.WITH_PRIV);
					}

				}
			}
		}

	}

	@Test
	void testDetectsCommand() {
		WindowsPrivescMisconfigurationAuditMacro macro = new WindowsPrivescMisconfigurationAuditMacro();
		assertTrue(macro.isCommandMatch(WindowsPrivescMisconfigurationAuditMacro.CMD));
		assertFalse(macro.isCommandMatch(WindowsPrivescMisconfigurationAuditMacro.CMD + " arg"));
		assertFalse(macro.isCommandMatch("something_else"));
	}

	@Test
	void testFindsMisquotedService() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io, false, true);
		exec.submit(em);

		WindowsPrivescMisconfigurationAuditMacro macro = new WindowsPrivescMisconfigurationAuditMacro();
		macro.initialize(io, null);

		MacroOutcome outcome = macro.processCmd(WindowsPrivescMisconfigurationAuditMacro.CMD, sessionId, "ignored");
		assertEquals(1, outcome.getAuditFindings().size());
		assertEquals("Audit Finding: 'Warning, service path is unquoted and may be used for priviledge escalation: DellClientManagementService               C:\\Program Files (x86)\\Dell\\UpdateService\\ServiceShell.exe'", outcome.getAuditFindings().get(0));
		assertEquals(1, outcome.getOutput().size());
		assertEquals("Audit Finding: 'Warning, service path is unquoted and may be used for priviledge escalation: DellClientManagementService               C:\\Program Files (x86)\\Dell\\UpdateService\\ServiceShell.exe'", outcome.getOutput().get(0));
	}

	@Test
	void testFindsSeImpersonate() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io, true, false);
		exec.submit(em);

		WindowsPrivescMisconfigurationAuditMacro macro = new WindowsPrivescMisconfigurationAuditMacro();
		macro.initialize(io, null);

		MacroOutcome outcome = macro.processCmd(WindowsPrivescMisconfigurationAuditMacro.CMD, sessionId, "ignored");
		assertEquals(1, outcome.getAuditFindings().size());
		assertEquals("Audit Finding: 'Warning: user has SeImpersonatePriviledge. This may be used to hijack the identity of another user that authenticates to a process controlled by this user.'", outcome.getAuditFindings().get(0));
		assertEquals(1, outcome.getOutput().size());
		assertEquals("Audit Finding: 'Warning: user has SeImpersonatePriviledge. This may be used to hijack the identity of another user that authenticates to a process controlled by this user.'", outcome.getOutput().get(0));
	}

	@Test
	void testHasNoFindingsOnCleanSystem() {
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

				OutputStreamWriterHelper.writeAndSend(bw, WindowsPrivescMisconfigurationAuditMacro.CMD);
				assertEquals("Macro Executor: '" + WindowsPrivescMisconfigurationAuditMacro.ALL_CLEAR_MSG + "'",
						br.readLine());

				OutputStreamWriterHelper.writeAndSend(bw, Commands.CLIENT_CMD_SHUTDOWN_DAEMON);
			} catch (Exception ex) {
				fail(ex.getMessage());
			}
		}
	}

}
