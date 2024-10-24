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
import util.Time;
import util.test.ClientServerTest;
import util.test.OutputStreamWriterHelper;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConstants;

class WindowsRegistryConfigurationAuditMacroTest extends ClientServerTest {

	private static final String NO_REG_VALUE ="\r\n"
			+ "\r\n"
			+ "ERROR: The system was unable to find the specified registry key or value.";
	
	private static final String UAC_ENABLED= "\r\n"
			+ "HKEY_LOCAL_MACHINE\\Software\\Microsoft\\Windows\\CurrentVersion\\Policies\\System\r\n"
			+ "    EnableLUA    REG_DWORD    0x1\r\n"
			+ "\r\n"
			+ "";
	private static final String LSA_ENABLED= "\r\n"
			+ "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\LSA\r\n"
			+ "    RunAsPPL    REG_DWORD    0x1\r\n"
			+ "\r\n"
			+ "";
	
	private static final String LSA_CFG= "\r\n"
			+ "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\LSA\r\n"
			+ "    LsaCfgFlags    REG_DWORD    0x1\r\n"
			+ "\r\n"
			+ "";
	
	private static final String LEGACY_LAPS= "\r\n"
			+ "HKEY_LOCAL_MACHINE\\Software\\Policies\\Microsoft Services\\AdmPwd\r\n"
			+ "    AdmPwdEnabled    REG_DWORD    0x1\r\n"
			+ "\r\n"
			+ "";
	
	private static final String WDIGEST_PLAINTEXT= "\r\n"
			+ "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSetControlSecurityProviders\\WDigest\r\n"
			+ "    UseLogonCredential    REG_DWORD    0x1\r\n"
			+ "\r\n"
			+ "";
	
	private static final String AUTOLOGON_DISABLED = "\r\n"
			+ "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Winlogon\r\n"
			+ "    AutoRestartShell    REG_DWORD    0x1\r\n"
			+ "    Background    REG_SZ    0 0 0\r\n"
			+ "    CachedLogonsCount    REG_SZ    10\r\n"
			+ "    DebugServerCommand    REG_SZ    no\r\n"
			+ "    DisableBackButton    REG_DWORD    0x1\r\n"
			+ "    EnableSIHostIntegration    REG_DWORD    0x1\r\n"
			+ "    ForceUnlockLogon    REG_DWORD    0x0\r\n"
			+ "    LegalNoticeCaption    REG_SZ\r\n"
			+ "    LegalNoticeText    REG_SZ\r\n"
			+ "    PasswordExpiryWarning    REG_DWORD    0x5\r\n"
			+ "    PowerdownAfterShutdown    REG_SZ    0\r\n"
			+ "    PreCreateKnownFolders    REG_SZ    {A520A1A4-1780-4FF6-BD18-167343C5AF16}\r\n"
			+ "    ReportBootOk    REG_SZ    1\r\n"
			+ "    Shell    REG_SZ    explorer.exe\r\n"
			+ "    ShellAppRuntime    REG_SZ    ShellAppRuntime.exe\r\n"
			+ "    ShellCritical    REG_DWORD    0x0\r\n"
			+ "    ShellInfrastructure    REG_SZ    sihost.exe\r\n"
			+ "    SiHostCritical    REG_DWORD    0x0\r\n"
			+ "    SiHostReadyTimeOut    REG_DWORD    0x0\r\n"
			+ "    SiHostRestartCountLimit    REG_DWORD    0x0\r\n"
			+ "    SiHostRestartTimeGap    REG_DWORD    0x0\r\n"
			+ "    Userinit    REG_SZ    C:\\Windows\\system32\\userinit.exe,\r\n"
			+ "    VMApplet    REG_SZ    SystemPropertiesPerformance.exe /pagefile\r\n"
			+ "    WinStationsDisabled    REG_SZ    0\r\n"
			+ "    scremoveoption    REG_SZ    0\r\n"
			+ "    DisableCAD    REG_DWORD    0x1\r\n"
			+ "    LastLogOffEndTimePerfCounter    REG_QWORD    0x15e4a595\r\n"
			+ "    ShutdownFlags    REG_DWORD    0x7\r\n"
			+ "    DisableLockWorkstation    REG_DWORD    0x0\r\n"
			+ "    EnableFirstLogonAnimation    REG_DWORD    0x1\r\n"
			+ "    AutoLogonSID    REG_SZ    S-1-5-21-1300932248-441011472-3549276696-1001\r\n"
			+ "    LastUsedUsername    REG_SZ    matte\r\n"
			+ "    DefaultUserName    REG_SZ    matte\r\n"
			+ "    DefaultDomainName    REG_SZ    .\r\n"
			+ "    AutoAdminLogon    REG_SZ    0\r\n"
			+ "\r\n"
			+ "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Winlogon\\AlternateShells\r\n"
			+ "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Winlogon\\GPExtensions\r\n"
			+ "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Winlogon\\ShellPrograms\r\n"
			+ "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Winlogon\\UserDefaults\r\n"
			+ "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Winlogon\\AutoLogonChecked\r\n"
			+ "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Winlogon\\VolatileUserMgrKey\r\n";
	
	
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
		RunnerTestGeneric.cleanup();
	}

	private class ClientStartCmdEmulator implements Runnable {

		private IOManager session;
		private int sessionId;
		private boolean alive = true;
		private boolean giveLSAEnabled;
		private boolean giveUACEnabled;
		private boolean giveLegacyLAPSEnabled;
		private boolean usingPlaintext;

		public ClientStartCmdEmulator(int sessionid, IOManager session, boolean giveLSAEnabled, boolean giveUACEnabled, boolean giveLegacyLapsInstalled, boolean usingPlaintext) {
			this.session = session;
			this.sessionId = sessionid;
			this.giveLSAEnabled = giveLSAEnabled;
			this.giveUACEnabled = giveUACEnabled;
			this.giveLegacyLAPSEnabled = giveLegacyLapsInstalled;
			this.usingPlaintext = usingPlaintext;
		}
		
		public void kill() {
			alive=false;
		}

		public void run() {
			while (alive) {
				String command = session.pollCommand(sessionId);
				if (command == null) {
					// continue
					Time.sleepWrapped(10);
				}else if(command.equalsIgnoreCase(WindowsRegistryConfigurationAuditMacro.CHECK_REG_FOR_LSA_CMD)) {
					if(giveLSAEnabled) {
						session.sendIO(sessionId, LSA_ENABLED);
					}else {
						session.sendIO(sessionId, NO_REG_VALUE);
					}
				}else if(command.equalsIgnoreCase(WindowsRegistryConfigurationAuditMacro.CHECK_REG_FOR_LSA_CFG_CMD)) {
					if(giveLSAEnabled) {
						session.sendIO(sessionId, LSA_CFG);
					}else {
						session.sendIO(sessionId, NO_REG_VALUE);
					}
				} else if(command.equalsIgnoreCase(WindowsRegistryConfigurationAuditMacro.CHECK_REG_FOR_UAC_CMD)){ //UAC Command
					if(giveUACEnabled) {
						session.sendIO(sessionId, UAC_ENABLED);
					}else {
						session.sendIO(sessionId, NO_REG_VALUE);
					}
				} else if(command.equalsIgnoreCase(WindowsRegistryConfigurationAuditMacro.CHECK_LEGACY_LAPS_INSTALLED)) {
					if(giveLegacyLAPSEnabled) {
						session.sendIO(sessionId, LEGACY_LAPS);
					}else {
						session.sendIO(sessionId, NO_REG_VALUE);
					}
				} else if(command.equalsIgnoreCase(WindowsRegistryConfigurationAuditMacro.CHECK_PLAINTEXT_DIGEST_CMD)) {
					if(usingPlaintext) {
						session.sendIO(sessionId, WDIGEST_PLAINTEXT);
					}else {
						session.sendIO(sessionId, NO_REG_VALUE);
					}
				} else if(command.equalsIgnoreCase(WindowsRegistryConfigurationAuditMacro.CHECK_AUTOLOGON)) {
					session.sendIO(sessionId, AUTOLOGON_DISABLED);
				}
			}
		}

	}
	
	@Test
	void testDetectsCommand() {
		WindowsRegistryConfigurationAuditMacro macro = new WindowsRegistryConfigurationAuditMacro();
		assertTrue(macro.isCommandMatch(WindowsRegistryConfigurationAuditMacro.CMD));
		assertFalse(macro.isCommandMatch(WindowsRegistryConfigurationAuditMacro.CMD + " arg"));
		assertFalse(macro.isCommandMatch("something_else"));
	}

	@Test
	void testGivesUACFinding() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io, true, false, true, false);
		exec.submit(em);
		
		WindowsRegistryConfigurationAuditMacro macro = new WindowsRegistryConfigurationAuditMacro();
		macro.initialize(io, null);

		MacroOutcome outcome = macro.processCmd(WindowsRegistryConfigurationAuditMacro.CMD, sessionId, "ignored");
		
		assertEquals(2, outcome.getAuditFindings().size());
		assertEquals("Audit Finding: 'Warning: UAC protection may not be enabled. Check the following registry settings to validate REG QUERY HKEY_LOCAL_MACHINE\\Software\\Microsoft\\Windows\\CurrentVersion\\Policies\\System\\ /v EnableLUA'", outcome.getAuditFindings().get(0));
		assertEquals("Audit Finding: 'Warning: This machine has been configured to use Legacy LAPS. Consider upgrading to the modern implementation of LAPS.'", outcome.getAuditFindings().get(1));
		assertEquals(2, outcome.getOutput().size());
		assertEquals("Audit Finding: 'Warning: UAC protection may not be enabled. Check the following registry settings to validate REG QUERY HKEY_LOCAL_MACHINE\\Software\\Microsoft\\Windows\\CurrentVersion\\Policies\\System\\ /v EnableLUA'", outcome.getOutput().get(0));
		assertEquals("Audit Finding: 'Warning: This machine has been configured to use Legacy LAPS. Consider upgrading to the modern implementation of LAPS.'", outcome.getOutput().get(1));
		
		em.kill();
	}
	
	@Test
	void testGivesWdigestPlaintext() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io, true, true, false, true);
		exec.submit(em);
		
		WindowsRegistryConfigurationAuditMacro macro = new WindowsRegistryConfigurationAuditMacro();
		macro.initialize(io, null);

		MacroOutcome outcome = macro.processCmd(WindowsRegistryConfigurationAuditMacro.CMD, sessionId, "ignored");
		
		assertEquals(1, outcome.getAuditFindings().size());
		assertEquals("Audit Finding: 'Warning: plaintext password storage appears to be enabled. Please confirm with reg query HKLM\\SYSTEM\\CurrentControlSetControlSecurityProviders\\WDigest /v UseLogonCredential'", outcome.getAuditFindings().get(0));
		assertEquals(1, outcome.getOutput().size());
		assertEquals("Audit Finding: 'Warning: plaintext password storage appears to be enabled. Please confirm with reg query HKLM\\SYSTEM\\CurrentControlSetControlSecurityProviders\\WDigest /v UseLogonCredential'", outcome.getOutput().get(0));
		
		em.kill();
	}
	
	@Test
	void testReturnsLiveLSAWithTextShell() {
		testMain(TestConstants.WINDOWSNATIVE_TEST_EXE);
	}
	
	void testMain(String client) {
		// Note: this test assumes that like most enpoint systems LSA protection is not enabled. If you have it
				// enabled on your dev system, good for you! 

				TestConfiguration.OS osConfig = TestConfiguration.getThisSystemOS();
				if (osConfig == TestConfiguration.OS.WINDOWS) {
					initiateServer();
					spawnClient(client);
					try {
						Socket remote = LocalConnection.getSocket("127.0.0.1", 8012,
								ClientServerTest.getDefaultSystemTestProperties());
						OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
						BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

						// Ensure that python client has connected
						Time.sleepWrapped(500);

						RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, osConfig == TestConfiguration.OS.LINUX, false);
						
						OutputStreamWriterHelper.writeAndSend(bw, WindowsRegistryConfigurationAuditMacro.CMD);
						assertEquals("Audit Finding: 'Warning: LSA protection may not be enabled. LSA protection provides additional protection against credential dumping and should be enabled if possible. Check the following registry settings to validate REG QUERY HKLM\\SYSTEM\\CurrentControlSet\\Control\\LSA /v RunAsPPL'", br.readLine());
						assertEquals("Audit Finding: 'Warning: LSA Credential Guard protection may not be enabled. LSA protection provides additional protection against credential dumping and should be enabled if possible. Check the following registry settings to validate REG QUERY HKLM\\SYSTEM\\CurrentControlSet\\Control\\LSA /v LsaCfgFlags'", br.readLine());
						OutputStreamWriterHelper.writeAndSend(bw, Commands.CLIENT_CMD_SHUTDOWN_DAEMON);
						//Client receive message
						Time.sleepWrapped(2000);
					} catch (Exception ex) {
						fail(ex.getMessage());
					}
				}
	}
	
	@Test
	void testReturnsLiveLSA() {
		testMain(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);
	}

}
