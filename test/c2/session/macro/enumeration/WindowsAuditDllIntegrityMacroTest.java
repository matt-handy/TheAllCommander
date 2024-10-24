package c2.session.macro.enumeration;

import static org.junit.jupiter.api.Assertions.*;

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
import c2.win.process.WindowsProcessInfoGatherer;
import c2.win.remotefiles.WindowsRemoteFileGatherer;
import c2.win.services.WindowsServiceParser;
import util.Time;
import util.test.ClientServerTest;
import util.test.OutputStreamWriterHelper;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConstants;

class WindowsAuditDllIntegrityMacroTest extends ClientServerTest {

	public static final String KNOWN_DLLS_RSP = "\r\n"
			+ "\r\n"
			+ "    Hive: HKEY_LOCAL_MACHINE\\System\\CurrentControlSet\\Control\\Session Manager\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "Name                           Property\r\n"
			+ "----                           --------\r\n"
			+ "KnownDLLs                      *kernel32 : kernel32.dll\r\n"
			+ "                               _wow64cpu : wow64cpu.dll\r\n"
			+ "                               _wowarmhw : wowarmhw.dll\r\n"
			+ "                               _xtajit   : xtajit.dll\r\n"
			+ "                               advapi32  : advapi32.dll\r\n"
			+ "                               clbcatq   : clbcatq.dll\r\n"
			+ "                               combase   : combase.dll\r\n"
			+ "                               COMDLG32  : COMDLG32.dll\r\n"
			+ "                               coml2     : coml2.dll\r\n"
			+ "                               DifxApi   : difxapi.dll\r\n"
			+ "                               gdi32     : gdi32.dll\r\n"
			+ "                               gdiplus   : gdiplus.dll\r\n"
			+ "                               IMAGEHLP  : IMAGEHLP.dll\r\n"
			+ "                               IMM32     : IMM32.dll\r\n"
			+ "                               MSCTF     : MSCTF.dll\r\n"
			+ "                               MSVCRT    : MSVCRT.dll\r\n"
			+ "                               NORMALIZ  : NORMALIZ.dll\r\n"
			+ "                               NSI       : NSI.dll\r\n"
			+ "                               ole32     : ole32.dll\r\n"
			+ "                               OLEAUT32  : OLEAUT32.dll\r\n"
			+ "                               PSAPI     : PSAPI.DLL\r\n"
			+ "                               rpcrt4    : rpcrt4.dll\r\n"
			+ "                               sechost   : sechost.dll\r\n"
			+ "                               Setupapi  : Setupapi.dll\r\n"
			+ "                               SHCORE    : SHCORE.dll\r\n"
			+ "                               SHELL32   : SHELL32.dll\r\n"
			+ "                               SHLWAPI   : SHLWAPI.dll\r\n"
			+ "                               user32    : user32.dll\r\n"
			+ "                               WLDAP32   : WLDAP32.dll\r\n"
			+ "                               wow64     : wow64.dll\r\n"
			+ "                               wow64base : wow64base.dll\r\n"
			+ "                               wow64con  : wow64con.dll\r\n"
			+ "                               wow64win  : wow64win.dll\r\n"
			+ "                               WS2_32    : WS2_32.dll\r\n"
			+ "                               xtajit64  : xtajit64.dll\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "";
	
	public static final String SAMPLE_TASKLIST_RSP = "\"Image Name\",\"PID\",\"Session Name\",\"Session#\",\"Mem Usage\",\"Status\",\"User Name\",\"CPU Time\",\"Window Title\"\r\n"
			+ "\"System Idle Process\",\"0\",\"Services\",\"0\",\"8 K\",\"Unknown\",\"NT AUTHORITY\\SYSTEM\",\"3277:13:43\",\"N/A\"\r\n"
			+ "\"System\",\"4\",\"Services\",\"0\",\"144 K\",\"Unknown\",\"N/A\",\"1:35:50\",\"N/A\"\r\n"
			+ "\"Secure System\",\"300\",\"Services\",\"0\",\"83,960 K\",\"Unknown\",\"N/A\",\"0:00:00\",\"N/A\"\r\n"
			+ "\"Registry\",\"336\",\"Services\",\"0\",\"40,596 K\",\"Unknown\",\"N/A\",\"0:00:02\",\"N/A\"\r\n"
			+ "\"smss.exe\",\"924\",\"Services\",\"0\",\"232 K\",\"Unknown\",\"N/A\",\"0:00:00\",\"N/A\"\r\n"
			+ "\"csrss.exe\",\"1348\",\"Services\",\"0\",\"2,708 K\",\"Unknown\",\"N/A\",\"0:00:02\",\"N/A\"\r\n"
			+ "\"wininit.exe\",\"1632\",\"Services\",\"0\",\"784 K\",\"Unknown\",\"N/A\",\"0:00:00\",\"N/A\"\r\n"
			+ "\"csrss.exe\",\"1640\",\"Console\",\"1\",\"5,164 K\",\"Running\",\"N/A\",\"0:00:49\",\"N/A\"\r\n"
			+ "\"services.exe\",\"1704\",\"Services\",\"0\",\"8,048 K\",\"Unknown\",\"N/A\",\"0:00:45\",\"N/A\"\r\n"
			+ "\"LsaIso.exe\",\"1724\",\"Services\",\"0\",\"1,716 K\",\"Unknown\",\"N/A\",\"0:00:00\",\"N/A\"\r\n"
			+ "\"lsass.exe\",\"1740\",\"Services\",\"0\",\"19,792 K\",\"Unknown\",\"N/A\",\"0:01:16\",\"N/A\"\r\n"
			+ "\"svchost.exe\",\"1864\",\"Services\",\"0\",\"37,092 K\",\"Unknown\",\"N/A\",\"0:01:06\",\"N/A\"\r\n"
			+ "\"fontdrvhost.exe\",\"1896\",\"Services\",\"0\",\"1,104 K\",\"Unknown\",\"N/A\",\"0:00:00\",\"N/A\"\r\n"
			+ "\"WUDFHost.exe\",\"1944\",\"Services\",\"0\",\"1,888 K\",\"Unknown\",\"N/A\",\"0:00:00\",\"N/A\"\r\n"
			+ "\"svchost.exe\",\"2008\",\"Services\",\"0\",\"23,524 K\",\"Unknown\",\"N/A\",\"0:00:49\",\"N/A\"\r\n"
			+ "\"svchost.exe\",\"1016\",\"Services\",\"0\",\"4,864 K\",\"Unknown\",\"N/A\",\"0:00:01\",\"N/A\"\r\n"
			+ "\"winlogon.exe\",\"1800\",\"Console\",\"1\",\"5,780 K\",\"Unknown\",\"N/A\",\"0:00:00\",\"N/A\"\r\n"
			+ "\"fontdrvhost.exe\",\"1336\",\"Console\",\"1\",\"6,464 K\",\"Unknown\",\"N/A\",\"0:00:00\",\"N/A\"\r\n"
			+ "\"hijackable.exe\",\"666\",\"Services\",\"0\",\"1,236 K\",\"Unknown\",\"ANDURIL\\GARY\",\"0:00:00\",\"N/A\"\r\n"
			+ "\"svchost.exe\",\"2128\",\"Services\",\"0\",\"1,064 K\",\"Unknown\",\"N/A\",\"0:00:00\",\"N/A\"\r\n"
			+ "\"svchost.exe\",\"2136\",\"Services\",\"0\",\"4,444 K\",\"Unknown\",\"N/A\",\"0:00:00\",\"N/A\"\r\n"
			+ "\"svchost.exe\",\"2212\",\"Services\",\"0\",\"6,032 K\",\"Unknown\",\"N/A\",\"0:00:00\",\"N/A\"\r\n"
			+ "\"svchost.exe\",\"2296\",\"Services\",\"0\",\"4,832 K\",\"Unknown\",\"N/A\",\"0:00:04\",\"N/A\"\r\n"
			+ "\"svchost.exe\",\"2332\",\"Services\",\"0\",\"5,096 K\",\"Unknown\",\"N/A\",\"0:00:00\",\"N/A\"\r\n"
			+ "\"svchost.exe\",\"2340\",\"Services\",\"0\",\"4,996 K\",\"Unknown\",\"N/A\",\"0:00:00\",\"N/A\"\\r\\n\\r\\n";
	
	public static final String SAMPLE_LIST_OF_DLLS = "\r\n"
			+ "   Size(K) ModuleName                                         FileName\r\n"
			+ "   ------- ----------                                         --------\r\n"
			+ "      2140 vuln.dll                                           C:\\OpenDIR\\vuln.dll\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "";
	
	public static final String SAMPLE_ACL = "\r\n"
			+ "\r\n"
			+ "FileSystemRights  : ReadAndExecute, Synchronize\r\n"
			+ "AccessControlType : Allow\r\n"
			+ "IdentityReference : S-1-15-3-1024-1238444810-1356253261-2257478630-1143196962-1563090664-2414759320-1282101916-42182878\r\n"
			+ "                    53\r\n"
			+ "IsInherited       : True\r\n"
			+ "InheritanceFlags  : None\r\n"
			+ "PropagationFlags  : None\r\n"
			+ "\r\n"
			+ "FileSystemRights  : FullControl\r\n"
			+ "AccessControlType : Allow\r\n"
			+ "IdentityReference : NT AUTHORITY\\SYSTEM\r\n"
			+ "IsInherited       : True\r\n"
			+ "InheritanceFlags  : None\r\n"
			+ "PropagationFlags  : None\r\n"
			+ "\r\n"
			+ "FileSystemRights  : FullControl\r\n"
			+ "AccessControlType : Allow\r\n"
			+ "IdentityReference : BUILTIN\\Administrators\r\n"
			+ "IsInherited       : True\r\n"
			+ "InheritanceFlags  : None\r\n"
			+ "PropagationFlags  : None\r\n"
			+ "\r\n"
			+ "FileSystemRights  : FullControl\r\n"
			+ "AccessControlType : Allow\r\n"
			+ "IdentityReference : BUILTIN\\Users\r\n"
			+ "IsInherited       : True\r\n"
			+ "InheritanceFlags  : None\r\n"
			+ "PropagationFlags  : None\r\n"
			+ "\r\n"
			+ "FileSystemRights  : ReadAndExecute, Synchronize\r\n"
			+ "AccessControlType : Allow\r\n"
			+ "IdentityReference : APPLICATION PACKAGE AUTHORITY\\ALL APPLICATION PACKAGES\r\n"
			+ "IsInherited       : True\r\n"
			+ "InheritanceFlags  : None\r\n"
			+ "PropagationFlags  : None\r\n"
			+ "\r\n"
			+ "FileSystemRights  : ReadAndExecute, Synchronize\r\n"
			+ "AccessControlType : Allow\r\n"
			+ "IdentityReference : APPLICATION PACKAGE AUTHORITY\\ALL RESTRICTED APPLICATION PACKAGES\r\n"
			+ "IsInherited       : True\r\n"
			+ "InheritanceFlags  : None\r\n"
			+ "PropagationFlags  : None\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "";
	
	public static final String SAMPLE_PATH="C:\\path_dir\r\n\r\n";
	
	public static final String PATH_ACL="\r\n"
			+ "\r\n"
			+ "FileSystemRights  : FullControl\r\n"
			+ "AccessControlType : Allow\r\n"
			+ "IdentityReference : NT AUTHORITY\\SYSTEM\r\n"
			+ "IsInherited       : True\r\n"
			+ "InheritanceFlags  : ContainerInherit, ObjectInherit\r\n"
			+ "PropagationFlags  : None\r\n"
			+ "\r\n"
			+ "FileSystemRights  : FullControl\r\n"
			+ "AccessControlType : Allow\r\n"
			+ "IdentityReference : BUILTIN\\Administrators\r\n"
			+ "IsInherited       : True\r\n"
			+ "InheritanceFlags  : ContainerInherit, ObjectInherit\r\n"
			+ "PropagationFlags  : None\r\n"
			+ "\r\n"
			+ "FileSystemRights  : FullControl\r\n"
			+ "AccessControlType : Allow\r\n"
			+ "IdentityReference : testHost\\noone\r\n"
			+ "IsInherited       : True\r\n"
			+ "InheritanceFlags  : ContainerInherit, ObjectInherit\r\n"
			+ "PropagationFlags  : None\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "";
	
	@AfterEach
	void shutdown() {
		awaitClient();
		teardown();
	}
	
	IOManager io;
	int sessionId;

	@BeforeEach
	void setUp() throws Exception {
		Properties prop = ClientServerTest.getDefaultSystemTestProperties();
		CommandLoader cl = new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>());
		io = new IOManager(new IOLogger(Paths.get(prop.getProperty(Constants.HUBLOGGINGPATH))), cl);

		sessionId = io.determineAndGetCorrectSessionId("testHost", "noone", "protocol", false, null);
	}
	
	
	@Test
	void testLiveSystemPython() {
		testLiveSystem(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);
	}
	
	@Test
	void testLiveSystemNative() {
		testLiveSystem(TestConstants.WINDOWSNATIVE_TEST_EXE);
	}
	
	void testLiveSystem(String clientStr) {
		//Test will run live, and is expected to return at least one PATH folder with access rights. No processes are expected.
		TestConfiguration.OS osConfig = TestConfiguration.getThisSystemOS();
		if (osConfig == TestConfiguration.OS.WINDOWS) {
			initiateServer();
			spawnClient(clientStr);
			try {
				Socket remote = LocalConnection.getSocket("127.0.0.1", 8012,
						ClientServerTest.getDefaultSystemTestProperties());
				OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
				BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

				// Ensure that client has connected
				Time.sleepWrapped(500);

				RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, osConfig == TestConfiguration.OS.LINUX, false);

				OutputStreamWriterHelper.writeAndSend(bw, WindowsAuditDllIntegrityMacro.CMD);
				String output = br.readLine();
				
				while(output != null && !output.equals("Macro Executor: 'Windows DLL Permissions Integrity Auditor Complete'")) {
					//Assumes that the test PC will have no vulnerable processes, but may have vulnerable PATH settings
					assertTrue(output.startsWith("Audit Finding: 'Warning: user can modify path directory:"), "The following is not a PATH warning: "+ output);
					output=br.readLine();
				}
				

				OutputStreamWriterHelper.writeAndSend(bw, Commands.CLIENT_CMD_SHUTDOWN_DAEMON);
				//Client receive message
				Time.sleepWrapped(2000);
				
			} catch (Exception ex) {
				ex.printStackTrace();
				fail(ex.getMessage());
			}
		}
	}
	
	private class ClientStartCmdEmulator implements Runnable {

		private IOManager session;
		private int sessionId;
		private boolean alive = true;
		
		public ClientStartCmdEmulator(int sessionid, IOManager session) {
			this.session = session;
			this.sessionId = sessionid;
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
				} else if (command.equalsIgnoreCase(WindowsAuditDllIntegrityMacro.GET_SYSTEMDRIVE_CMD)) {
					session.sendIO(sessionId, "C:" + WindowsConstants.WINDOWS_LINE_SEP);
				}else if (command.equalsIgnoreCase(WindowsRemoteFileGatherer.GET_KNOWN_DLLS)) {
					session.sendIO(sessionId, KNOWN_DLLS_RSP + WindowsConstants.WINDOWS_LINE_SEP);
				} else if (command.equalsIgnoreCase(WindowsProcessInfoGatherer.TASKLIST_CMD)){
					session.sendIO(sessionId, SAMPLE_TASKLIST_RSP);
				}else if(command.equalsIgnoreCase(WindowsProcessInfoGatherer.TAC_COMPLETE_ECHO)) {
					session.sendIO(sessionId, "TAC_COMPLETE_SCAN\r\n");
				}else if(command.equalsIgnoreCase("powershell -c \"(Get-Process -Id 666).Modules\"")) {
					//Process 666 is flagged as being from another user in SAMPLE_TASKLIST
					session.sendIO(sessionId, SAMPLE_LIST_OF_DLLS + WindowsConstants.WINDOWS_LINE_SEP);
				}else if(command.equalsIgnoreCase("powershell -c \"Get-Acl -Path 'C:\\OpenDIR\\vuln.dll' | Select-Object -ExpandProperty Access\"")) {
					session.sendIO(sessionId, SAMPLE_ACL + WindowsConstants.WINDOWS_LINE_SEP);
				}else if(command.equalsIgnoreCase(WindowsAuditDllIntegrityMacro.GET_PATH_CMD)) {
					session.sendIO(sessionId, SAMPLE_PATH + WindowsConstants.WINDOWS_LINE_SEP);
				}else if(command.startsWith("powershell -c \"Get-Acl -Path 'C:\\path_dir' | Select-Object -ExpandProperty Access\"")) {
					session.sendIO(sessionId, PATH_ACL);
				}
			}
		}

	}
	
	@Test
	void testFindsOtherUserProcess() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io);
		exec.submit(em);

		WindowsAuditDllIntegrityMacro macro = new WindowsAuditDllIntegrityMacro();
		macro.initialize(io, null);
		
		MacroOutcome outcome = macro.processCmd(WindowsAuditDllIntegrityMacro.CMD, sessionId, "ignored");
		assertEquals(2, outcome.getAuditFindings().size());
		assertEquals("Audit Finding: 'Warning: current user can modify other processes DLL. Process: hijackable.exe and module vuln.dll'", outcome.getAuditFindings().get(0));
		assertEquals("Audit Finding: 'Warning: user can modify path directory: C:\\path_dir which may lead to insecure user action.'", outcome.getAuditFindings().get(1));
		assertEquals(3, outcome.getOutput().size());
		assertEquals("Audit Finding: 'Warning: current user can modify other processes DLL. Process: hijackable.exe and module vuln.dll'", outcome.getOutput().get(0));
		assertEquals("Audit Finding: 'Warning: user can modify path directory: C:\\path_dir which may lead to insecure user action.'", outcome.getOutput().get(1));
		assertEquals("Macro Executor: 'Windows DLL Permissions Integrity Auditor Complete'", outcome.getOutput().get(2));
		em.kill();
	}
	
	@Test
	void testFindsLiveOtherUser() {
		//TODO: Mark this for a follow-up action item
		System.out.println("TODO: Implement testFindsLiveOtherUser");
	}

}
