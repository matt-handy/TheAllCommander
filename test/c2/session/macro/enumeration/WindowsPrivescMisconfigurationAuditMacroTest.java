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
import c2.WindowsConstants;
import c2.admin.LocalConnection;
import c2.session.CommandLoader;
import c2.session.IOManager;
import c2.session.log.IOLogger;
import c2.session.macro.MacroOutcome;
import c2.win.WindowsQuotedServiceCheckerTest;
import c2.win.WindowsUserPriviledgeParser;
import c2.win.WindowsUserPriviledgeParserTest;
import c2.win.services.WindowsServiceParser;
import util.Time;
import util.test.ClientServerTest;
import util.test.OutputStreamWriterHelper;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConstants;

class WindowsPrivescMisconfigurationAuditMacroTest extends ClientServerTest {

	private static final String LM_KEY= "\r\n"
			+ "HKCU\\SOFTWARE\\Policies\\Microsoft\\Windows\\Installer\r\n"
			+ "    AlwaysInstallElevated    REG_DWORD    0x1\r\n"
			+ "\r\n"
			+ "";
	
	private static final String CU_KEY= "\r\n"
			+ "HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows\\Installer\r\n"
			+ "    AlwaysInstallElevated    REG_DWORD    0x1\r\n"
			+ "\r\n"
			+ "";
	
	private static final String HARD_ICACLS = "myexe.exe NT AUTHORITY\\SYSTEM:(I)(F)\r\n"
			+ "                BUILTIN\\Administrators:(I)(F)\r\n"
			+ "\r\n"
			+ "Successfully processed 1 files; Failed processing 0 files\r\n"
			+ "";
	
	private static final String WEAK_ICACLS = "C:\\Program Files\\Dell\\DellDataVault\\DDVCollectorSvcApi.exe NT AUTHORITY\\SYSTEM:(I)(F)\r\n"
			+ "                BUILTIN\\Administrators:(I)(F)\r\n"
			+ "                BUILTIN\\Everyone:(I)(F)\r\n"
			+ "\r\n"
			+ "Successfully processed 1 files; Failed processing 0 files\r\n"
			+ "";
	
	private static final String EXPECTED_BAD_ICACLS_MSG = "Audit Finding: 'Warning: Service DDVCollectorSvcApi has potentially insecure permissions, please review: C:\\Program Files\\Dell\\DellDataVault\\DDVCollectorSvcApi.exe NT AUTHORITY\\SYSTEM:(I)(F)\r\n"
			+ "                BUILTIN\\Administrators:(I)(F)\r\n"
			+ "                BUILTIN\\Everyone:(I)(F)\r\n"
			+ "\r\n"
			+ "Successfully processed 1 files; Failed processing 0 files\r\n"
			+ "'";
	
	
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
		private boolean giveNoRegCu;
		private boolean giveNoRegLm;
		private boolean giveOneWeakServiceACL;
		private boolean giveModifiableArgument;

		public ClientStartCmdEmulator(int sessionid, IOManager session, boolean giveCleanServices,
				boolean giveCleanPrivs, boolean giveNoRegCu, boolean giveNoRegLm, boolean giveOneWeakServiceACL, boolean giveModifiableArgument) {
			this.session = session;
			this.sessionId = sessionid;
			this.giveCleanServices = giveCleanServices;
			this.giveCleanPrivs = giveCleanPrivs;
			this.giveNoRegCu = giveNoRegCu;
			this.giveNoRegLm = giveNoRegLm;
			this.giveOneWeakServiceACL = giveOneWeakServiceACL;
			this.giveModifiableArgument = giveModifiableArgument;
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
				} else if (command.equalsIgnoreCase(WindowsServiceParser.SERVICE_PATH_QUERY)) {
					if (giveCleanServices) {
						session.sendIO(sessionId, WindowsQuotedServiceCheckerTest.ALL_QUOTED_SVCS);
					} else {
						session.sendIO(sessionId, WindowsQuotedServiceCheckerTest.MISSING_QUOTED_SVCS);
					}
				}else if (command.equalsIgnoreCase(WindowsServiceParser.CAT_CSV)) {
					if (giveCleanServices && !giveModifiableArgument) {
						session.sendIO(sessionId, WindowsQuotedServiceCheckerTest.ALL_QUOTED_SVCS_CSV);
					}else if(giveModifiableArgument) {
						session.sendIO(sessionId, WindowsQuotedServiceCheckerTest.ALL_QUOTED_SVCS_CSV_ACCESSIBLE_ARG);
					} else {
						session.sendIO(sessionId, WindowsQuotedServiceCheckerTest.MISSING_QUOTED_SVCS_CSV);
					}
				} else if (command.equalsIgnoreCase(WindowsUserPriviledgeParser.QUERY)){
					if (giveCleanPrivs) {
						// We want to trigger the warning for MS10-092 and MS14-058
						session.sendIO(sessionId, WindowsUserPriviledgeParserTest.NO_PRIV);
					} else {
						// These numbers don't really matter when we aren't trying to trigger the vuln
						// detection
						session.sendIO(sessionId, WindowsUserPriviledgeParserTest.WITH_PRIV);
					}

				}else if(command.equalsIgnoreCase(WindowsPrivescMisconfigurationAuditMacro.CHECK_AUTO_ELEVATE_INSTALLER_CU)) {
					if(giveNoRegCu) {
						session.sendIO(sessionId, WindowsConstants.WINDOWS_NO_REGISTRY_KEY + "\r\n");
					}else {
						session.sendIO(sessionId, CU_KEY);
					}
				}else if(command.equalsIgnoreCase(WindowsPrivescMisconfigurationAuditMacro.CHECK_AUTO_ELEVATE_INSTALLER_LM)) {
					if(giveNoRegLm) {
						session.sendIO(sessionId, WindowsConstants.WINDOWS_NO_REGISTRY_KEY + "\r\n");
					}else {
						session.sendIO(sessionId, LM_KEY);
					}
				}else if(command.startsWith("icacls")) {
					if(command.contains("DDVCollectorSvcApi") && giveOneWeakServiceACL) {
						session.sendIO(sessionId, WEAK_ICACLS);
					}else if(command.contains(".xml")) {//icacls queries for the XML file
						session.sendIO(sessionId, WEAK_ICACLS);
					}else {
						session.sendIO(sessionId, HARD_ICACLS);
					}
				}else if(command.startsWith(Commands.CLIENT_CMD_PWD)) {
					session.sendIO(sessionId, Paths.get("").toString());
				}else if(command.startsWith("echo %SYSTEMDRIVE%")) {
					session.sendIO(sessionId, "C:\r\n\r\n");
				}else if(command.startsWith("cd C:\\Windows")) {
					session.sendIO(sessionId, "C:\\Windows\r\n\r\n");
				}else if(command.startsWith(WindowsPrivescMisconfigurationAuditMacro.CHECK_FOR_GPP_HISTORY_FILES)) {
					session.sendIO(sessionId, "File Not Found");
				}else if(command.startsWith("cd ")) {//This will catch the return to original working directory, not the CD to windows
					session.sendIO(sessionId, "cd " + Paths.get("").toString());
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
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io, false, true, true, true, false, false);
		exec.submit(em);

		WindowsPrivescMisconfigurationAuditMacro macro = new WindowsPrivescMisconfigurationAuditMacro();
		macro.initialize(io, null);

		MacroOutcome outcome = macro.processCmd(WindowsPrivescMisconfigurationAuditMacro.CMD, sessionId, "ignored");
		assertEquals(1, outcome.getAuditFindings().size());
		assertEquals("Audit Finding: 'Warning, service path is unquoted and may be used for priviledge escalation: \"DDVCollectorSvcApi\",\"C:\\Program Files\\Dell\\DellDataVault\\DDVCollectorSvcApi.exe\"'", outcome.getAuditFindings().get(0));
		assertEquals(1, outcome.getOutput().size());
		assertEquals("Audit Finding: 'Warning, service path is unquoted and may be used for priviledge escalation: \"DDVCollectorSvcApi\",\"C:\\Program Files\\Dell\\DellDataVault\\DDVCollectorSvcApi.exe\"'", outcome.getOutput().get(0));
		em.kill();
	}

	@Test
	void testFindsSeImpersonate() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io, true, false, true, true, false, false);
		exec.submit(em);

		WindowsPrivescMisconfigurationAuditMacro macro = new WindowsPrivescMisconfigurationAuditMacro();
		macro.initialize(io, null);

		MacroOutcome outcome = macro.processCmd(WindowsPrivescMisconfigurationAuditMacro.CMD, sessionId, "ignored");
		assertEquals(1, outcome.getAuditFindings().size());
		assertEquals("Audit Finding: 'Warning: user has SeImpersonatePriviledge. This may be used to hijack the identity of another user that authenticates to a process controlled by this user.'", outcome.getAuditFindings().get(0));
		assertEquals(1, outcome.getOutput().size());
		assertEquals("Audit Finding: 'Warning: user has SeImpersonatePriviledge. This may be used to hijack the identity of another user that authenticates to a process controlled by this user.'", outcome.getOutput().get(0));
		em.kill();
	}
	
	@Test
	void testFindsCuAutoElevate() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io, true, true, false, true, false, false);
		exec.submit(em);

		WindowsPrivescMisconfigurationAuditMacro macro = new WindowsPrivescMisconfigurationAuditMacro();
		macro.initialize(io, null);

		MacroOutcome outcome = macro.processCmd(WindowsPrivescMisconfigurationAuditMacro.CMD, sessionId, "ignored");
		assertEquals(1, outcome.getAuditFindings().size());
		assertEquals("Audit Finding: 'Warning: current user is configured for installers to auto-elevate to administrator rights. This settings is rarely appropriate and should be evaluated.'", outcome.getAuditFindings().get(0));
		assertEquals(1, outcome.getOutput().size());
		assertEquals("Audit Finding: 'Warning: current user is configured for installers to auto-elevate to administrator rights. This settings is rarely appropriate and should be evaluated.'", outcome.getOutput().get(0));
		em.kill();
	}
	
	@Test
	void testFindsLmAutoElevate() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io, true, true, true, false, false, false);
		exec.submit(em);

		WindowsPrivescMisconfigurationAuditMacro macro = new WindowsPrivescMisconfigurationAuditMacro();
		macro.initialize(io, null);

		MacroOutcome outcome = macro.processCmd(WindowsPrivescMisconfigurationAuditMacro.CMD, sessionId, "ignored");
		assertEquals(1, outcome.getAuditFindings().size());
		assertEquals("Audit Finding: 'Warning: local machine is configured for installers to auto-elevate to administrator rights. This settings is rarely appropriate and should be evaluated.'", outcome.getAuditFindings().get(0));
		assertEquals(1, outcome.getOutput().size());
		assertEquals("Audit Finding: 'Warning: local machine is configured for installers to auto-elevate to administrator rights. This settings is rarely appropriate and should be evaluated.'", outcome.getOutput().get(0));
		em.kill();
	}

	@Test
	void findsBadIcacls() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io, true, true, true, true, true, false);
		exec.submit(em);

		WindowsPrivescMisconfigurationAuditMacro macro = new WindowsPrivescMisconfigurationAuditMacro();
		macro.initialize(io, null);
		
		MacroOutcome outcome = macro.processCmd(WindowsPrivescMisconfigurationAuditMacro.CMD, sessionId, "ignored");
		assertEquals(1, outcome.getAuditFindings().size());
		assertEquals(EXPECTED_BAD_ICACLS_MSG, outcome.getAuditFindings().get(0));
		assertEquals(1, outcome.getOutput().size());
		assertEquals(EXPECTED_BAD_ICACLS_MSG, outcome.getOutput().get(0));
		em.kill();
	}
	
	@Test
	void testHasNoFindingsOnCleanSystemWithTextShell() {
		testMain(TestConstants.WINDOWSNATIVE_TEST_EXE);
	}
	
	@Test
	void testCanFindServiceWithOpenConfigFile() {
		String expectedMsg = "Audit Finding: 'Warning: Service FakeSvc has potentially insecure permissions on argument, please review: C:\\Program Files\\Dell\\DellDataVault\\DDVCollectorSvcApi.exe NT AUTHORITY\\SYSTEM:(I)(F)\r\n"
				+ "                BUILTIN\\Administrators:(I)(F)\r\n"
				+ "                BUILTIN\\Everyone:(I)(F)\r\n"
				+ "\r\n"
				+ "Successfully processed 1 files; Failed processing 0 files\r\n"
				+ "'";
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io, true, true, true, true, false, true);
		exec.submit(em);

		WindowsPrivescMisconfigurationAuditMacro macro = new WindowsPrivescMisconfigurationAuditMacro();
		macro.initialize(io, null);

		MacroOutcome outcome = macro.processCmd(WindowsPrivescMisconfigurationAuditMacro.CMD, sessionId, "ignored");
		assertEquals(1, outcome.getAuditFindings().size());
		assertEquals(expectedMsg, outcome.getAuditFindings().get(0));
		assertEquals(1, outcome.getOutput().size());
		assertEquals(expectedMsg, outcome.getOutput().get(0));
		em.kill();
	}
	
	void testMain(String clientStr) {
		// Note: this test assumes the dev machine is fully patched and there are no
				// audit findings. If the test fails, PATCH YOUR SYSTEM!!!

				TestConfiguration.OS osConfig = TestConfiguration.getThisSystemOS();
				if (osConfig == TestConfiguration.OS.WINDOWS) {
					initiateServer();
					spawnClient(clientStr);
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
						//Client receive message
						Time.sleepWrapped(2000);
					} catch (Exception ex) {
						fail(ex.getMessage());
					}
				}
	}
	
	@Test
	void testHasNoFindingsOnCleanSystem() {
		testMain(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);
	}

}
