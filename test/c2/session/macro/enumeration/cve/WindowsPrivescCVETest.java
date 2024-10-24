package c2.session.macro.enumeration.cve;

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

import com.ctc.wstx.sw.RepairingNsStreamWriter;

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

	private static final String NO_CVE_SYSTEMINFO= "Host Name:                 ANDURIL\r\n"
			+ "OS Name:                   Microsoft Windows 11 Home\r\n"
			+ "OS Version:                10.0.22631 N/A Build 22631\r\n"
			+ "OS Manufacturer:           Microsoft Corporation\r\n"
			+ "OS Configuration:          Standalone Workstation\r\n"
			+ "OS Build Type:             Multiprocessor Free\r\n"
			+ "Registered Owner:          noone@gmail.com\r\n"
			+ "Registered Organization:   N/A\r\n"
			+ "Product ID:                00342-21134-16801-AAOEM\r\n"
			+ "Original Install Date:     3/10/2024, 4:27:10 PM\r\n"
			+ "System Boot Time:          8/23/2024, 11:24:10 PM\r\n"
			+ "System Manufacturer:       Alienware\r\n"
			+ "System Model:              Alienware m18 R1 AMD\r\n"
			+ "System Type:               x64-based PC\r\n"
			+ "Processor(s):              1 Processor(s) Installed.\r\n"
			+ "                           [01]: AMD64 Family 25 Model 97 Stepping 2 AuthenticAMD ~2501 Mhz\r\n"
			+ "BIOS Version:              Alienware 1.13.1, 4/23/2024\r\n"
			+ "Windows Directory:         C:\\Windows\r\n"
			+ "System Directory:          C:\\Windows\\system32\r\n"
			+ "Boot Device:               \\Device\\HarddiskVolume1\r\n"
			+ "System Locale:             en-us;English (United States)\r\n"
			+ "Input Locale:              en-us;English (United States)\r\n"
			+ "Time Zone:                 (UTC-05:00) Eastern Time (US & Canada)\r\n"
			+ "Total Physical Memory:     31,937 MB\r\n"
			+ "Available Physical Memory: 15,622 MB\r\n"
			+ "Virtual Memory: Max Size:  37,057 MB\r\n"
			+ "Virtual Memory: Available: 10,473 MB\r\n"
			+ "Virtual Memory: In Use:    26,584 MB\r\n"
			+ "Page File Location(s):     C:\\pagefile.sys\r\n"
			+ "Domain:                    WORKGROUP\r\n"
			+ "Logon Server:              \\\\HOSTNAME\r\n"
			+ "Hotfix(s):                 6 Hotfix(s) Installed.\r\n"
			+ "                           [01]: KB5042099\r\n"
			+ "                           [02]: KB5027397\r\n"
			+ "                           [03]: KB5031274\r\n"
			+ "                           [04]: KB5036212\r\n"
			+ "                           [05]: KB5041585\r\n"
			+ "                           [06]: KB5041584\r\n"
			+ "Network Card(s):           2 NIC(s) Installed.\r\n"
			+ "                           [01]: Realtek Gaming 2.5GbE Family Controller\r\n"
			+ "                                 Connection Name: Ethernet\r\n"
			+ "                                 Status:          Media disconnected\r\n"
			+ "                           [02]: VirtualBox Host-Only Ethernet Adapter\r\n"
			+ "                                 Connection Name: Ethernet 2\r\n"
			+ "                                 DHCP Enabled:    No\r\n"
			+ "                                 IP address(es)\r\n"
			+ "                                 [01]: 192.168.56.1\r\n"
			+ "                                 [02]: fe80::d5b9:7bc6:87ef:1271\r\n"
			+ "Hyper-V Requirements:      A hypervisor has been detected. Features required for Hyper-V will not be displayed.\r\n"
			+ "";
	
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
		private boolean giveMSValues;

		public ClientStartCmdEmulator(int sessionid, IOManager session, boolean giveMSValues) {
			this.session = session;
			this.sessionId = sessionid;
			this.giveMSValues = giveMSValues;
		}

		public void run() {
			while (alive) {
				String command = session.pollCommand(sessionId);
				if (command == null) {
					// continue
					Time.sleepWrapped(10);
				}else if(command.equalsIgnoreCase(WindowsConstants.SYSTEMINFO_CMD)) {
					if(giveMSValues) {
						session.sendIO(sessionId, NO_CVE_SYSTEMINFO);
					}else {
						session.sendIO(sessionId, WindowsPatchLevelCVECheckerTest.SAMPLE_SYSTEMINFO_CVE_2019_0836);
					}
				} else {
					//We got the command for the first FileInfoCall. We can close now after sending results.
					if(giveMSValues) {
						//We want to trigger the warning for MS10-092 and MS14-058
						session.sendIO(sessionId, "7600\r\n");
						session.sendIO(sessionId, "18000\r\n");
						session.sendIO(sessionId, "7600\r\n");
						session.sendIO(sessionId, "18000\r\n");
						session.sendIO(sessionId, "Get-Item : Cannot find path 'C:\\System32\\win32k.sys' because it does not exist.At line:1 char:2+ (Get-Item C:\\System32\\win32k.sys).VersionInfo.FileBuildPart+  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~    + CategoryInfo          : ObjectNotFound: (C:\\System32\\win32k.sys:String) [Get-Item], ItemNotFoundException    + FullyQualifiedErrorId : PathNotFound,Microsoft.PowerShell.Commands.GetItemCommand\r\n");
					}else {
						//These numbers don't really matter when we aren't trying to trigger the vuln detection
						session.sendIO(sessionId, "22631\r\n");
						session.sendIO(sessionId, "22631\r\n");
						session.sendIO(sessionId, "22631\r\n");
						session.sendIO(sessionId, "22631\r\n");
						session.sendIO(sessionId, "Get-Item : Cannot find path 'C:\\System32\\win32k.sys' because it does not exist.At line:1 char:2+ (Get-Item C:\\System32\\win32k.sys).VersionInfo.FileBuildPart+  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~    + CategoryInfo          : ObjectNotFound: (C:\\System32\\win32k.sys:String) [Get-Item], ItemNotFoundException    + FullyQualifiedErrorId : PathNotFound,Microsoft.PowerShell.Commands.GetItemCommand\r\n");
					}
					
					alive = false;
				}
			}
		}

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
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io, false);
		exec.submit(em);
		
		WindowsPrivescCVE macro = new WindowsPrivescCVE();
		macro.initialize(io, null);

		MacroOutcome outcome = macro.processCmd(WindowsPrivescCVE.CMD, sessionId, "ignored");
		assertEquals(1, outcome.getAuditFindings().size());
		assertEquals("Audit Finding: 'Applicable CVE: CVE-2019-0836'", outcome.getAuditFindings().get(0));
		assertEquals(1, outcome.getOutput().size());
		assertEquals("Audit Finding: 'Applicable CVE: CVE-2019-0836'", outcome.getOutput().get(0));
	}
	
	@Test
	void testGivesCorrectMSAudit() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io, true);
		exec.submit(em);
		
		WindowsPrivescCVE macro = new WindowsPrivescCVE();
		macro.initialize(io, null);

		MacroOutcome outcome = macro.processCmd(WindowsPrivescCVE.CMD, sessionId, "ignored");
		assertEquals(3, outcome.getAuditFindings().size());
		assertEquals("Audit Finding: 'Applicable Vulnerability: MS10-092'", outcome.getAuditFindings().get(0));
		assertEquals("Audit Finding: 'Applicable Vulnerability: MS14-058'", outcome.getAuditFindings().get(1));
		assertEquals("Audit Finding: 'Applicable Vulnerability: MS15-051'", outcome.getAuditFindings().get(2));
		assertEquals(3, outcome.getOutput().size());
		assertEquals("Audit Finding: 'Applicable Vulnerability: MS10-092'", outcome.getOutput().get(0));
		assertEquals("Audit Finding: 'Applicable Vulnerability: MS14-058'", outcome.getOutput().get(1));
		assertEquals("Audit Finding: 'Applicable Vulnerability: MS15-051'", outcome.getOutput().get(2));
	}

	@Test
	void testPython() {
		testReturnsNoCVEs(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);
	}
	
	@Test
	void testNative() {
		testReturnsNoCVEs(TestConstants.WINDOWSNATIVE_TEST_EXE);
	}
	
	void testReturnsNoCVEs(String client) {
		// Note: this test assumes the dev machine is fully patched and there are no
		// audit findings. If the test fails, PATCH YOUR SYSTEM!!!

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
				
				OutputStreamWriterHelper.writeAndSend(bw, WindowsPrivescCVE.CMD);
				assertEquals("Macro Executor: 'Windows Privesc CVE Enumerator: No findings'", br.readLine());

				OutputStreamWriterHelper.writeAndSend(bw, Commands.CLIENT_CMD_SHUTDOWN_DAEMON);
				
				Time.sleepWrapped(2000);
			} catch (Exception ex) {
				fail(ex.getMessage());
			}
		}

	}

}
