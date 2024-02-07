package c2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import c2.admin.LocalConnection;
import c2.session.SessionInitiator;
import util.Time;
import util.test.ClientServerTest;
import util.test.OutputStreamWriterHelper;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;
import util.test.TestConstants;

class CommanderInterfaceTest extends ClientServerTest {

	Socket remote;
	OutputStreamWriter bw;
	BufferedReader br;
	
	@AfterEach()
	void clean(){
		teardown();
	}
	
	private void secureBoot() {
		initiateServer("test_secure.properties");
		try {
		remote = LocalConnection.getSocket("127.0.0.1", Integer.parseInt(ClientServerTest.getDefaultSystemTestProperties().getProperty(Constants.SECURECOMMANDERPORT)), getDefaultSystemTestProperties());
		bw = new OutputStreamWriter(remote.getOutputStream());
		br = new BufferedReader(new InputStreamReader(remote.getInputStream()));
		}catch(Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}
	
	private void secureTeardown() {
		try {
			bw.close();
			br.close();
			remote.close();
		}catch(IOException ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}
	
	@Test
	void testDoesSecureLogin() {
		secureBoot();
		try {
			String output = br.readLine();
			assertEquals("Username:", output);
			OutputStreamWriterHelper.writeAndSend(bw, "admin");
			output = br.readLine();
			assertEquals("Password:", output);
			OutputStreamWriterHelper.writeAndSend(bw, "changeme");
			output = br.readLine();
			assertEquals("Access Granted", output);
		}catch(Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
		secureTeardown();
	}
	
	@Test
	void testDoesSecureLoginCheckUsername() {
		secureBoot();
		try {
			String output = br.readLine();
			assertEquals("Username:", output);
			OutputStreamWriterHelper.writeAndSend(bw, "notadmin");
			output = br.readLine();
			assertEquals("Invalid Username",output);
			
			
		}catch(Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
		secureTeardown();
	}
	
	@Test
	void testDoesSecureLoginCheckPassword() {
		secureBoot();
		try {
			String output = br.readLine();
			assertEquals("Username:", output);
			OutputStreamWriterHelper.writeAndSend(bw, "admin");
			output = br.readLine();
			assertEquals("Password:", output);
			OutputStreamWriterHelper.writeAndSend(bw, "barf");
			output = br.readLine();
			assertEquals("Invalid Password", output);
		}catch(Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
		secureTeardown();
	}
	
	@Test
	void testListAllMacros() {
		initiateServer();
		
		try {
			System.out.println("Connecting test commander...");
			Socket remote = LocalConnection.getSocket("127.0.0.1", Integer.parseInt(ClientServerTest.getDefaultSystemTestProperties().getProperty(Constants.SECURECOMMANDERPORT)), getDefaultSystemTestProperties());
			System.out.println("Locking test commander streams...");
			OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

			String output = br.readLine();
			assertEquals(output, SessionInitiator.AVAILABLE_SESSION_BANNER);
			output = br.readLine();
			assertEquals("1:default:default:default", output);
			output = br.readLine();
			assertEquals(SessionInitiator.WIZARD_BANNER, output);
			
			OutputStreamWriterHelper.writeAndSend(bw, "1");
			assertEquals("New session: default:default:default", br.readLine());
			
			OutputStreamWriterHelper.writeAndSend(bw, Commands.SERVER_CMD_LIST_ALL_MACROS);
			
			assertEquals("Delete Browser Cookies", br.readLine());
			assertEquals("delete_cookies", br.readLine());
			assertEquals("This macro deletes cookies for Edge, Firefox, and Chrome", br.readLine());
			
			assertEquals("Browser Cookie Harvester", br.readLine());
			assertEquals("harvest_cookies", br.readLine());
			assertEquals("This macro harvests available browser cookies for Edge, Chrome, and Firefox", br.readLine());
			
			assertEquals("Windows log deletion macro", br.readLine());
			assertEquals("delete_windows_logs (optional - application, security, system, or setup)", br.readLine());
			assertEquals("When invoked with no arguments, this macro will delete all Windows Event logs. When the argument application, security, system, or setup is provided, the corresponding logset is deleted", br.readLine());
			
			assertEquals("Clean Fodhelper", br.readLine());
			assertEquals("clean_fodhelper", br.readLine());
			assertEquals("This macro cleares the registry key used by the FOD Helper UAC bypass macro", br.readLine());
			
			assertEquals("Spawn FOD Helper elevated session macro", br.readLine());
			assertEquals("spawn_fodhelper_elevated_session", br.readLine());
			assertEquals("This macro uses the FOD Helper UAC bypass to spawn an elevated session.", br.readLine());
			
			assertEquals("Recycle Bin Cleaner Macro", br.readLine());
			assertEquals("empty_recycle_bin", br.readLine());
			assertEquals("This macro deletes the contents of the recycle bin", br.readLine());
			
			assertEquals("Outlook Harvester Macro", br.readLine());
			assertEquals("harvest_outlook basic", br.readLine());
			assertEquals("harvest_outlook deep <optional directory to search>", br.readLine());
			assertEquals("This macro searches either the default Outlook folders (basic option) or deep searches the file system (deep option) to find Outlook .pst and .ost files", br.readLine());
			
			assertEquals("User Directory Harvester Macro", br.readLine());
			assertEquals("harvest_user_dir", br.readLine());
			assertEquals("This macro harvests the contents of the user documents and desktop directory", br.readLine());
			
			assertEquals("Windows Startup Key Persistence Macro", br.readLine());
			assertEquals("regkey_persist (cu or lm) <optional - calc>", br.readLine());
			assertEquals("third argument, calc, is used when the test should insert calc.exe as the startup argument", br.readLine());
			assertEquals("The second argument is used to specify if the current user or local machine startup key should be used.", br.readLine());
			assertEquals("This macro will use either the current user or local machine startup key to enable persistence for the daemon.", br.readLine());
			
			assertEquals("Registry Debugger Persistence Macro", br.readLine());
			assertEquals("reg_debugger <name of EXE to attach>", br.readLine());
			assertEquals("This macro utilizes the Windows feature to allow a process to be launched instead of another to serve as a debugger. This is not a particularly stealth persistence method, but some attackers will use it. Requires an elevated session.", br.readLine());
			
			assertEquals("Registry Silent Exit Persistence Macro", br.readLine());
			assertEquals("reg_silent_exit <name of EXE to attach>", br.readLine());
			assertEquals("This macro will use the Windows feature to launch a process after another process exits to enable persistence. Requires an elevated session.", br.readLine());
			
			assertEquals("Windows Antivirus Enumerator", br.readLine());
			assertEquals("enum_av", br.readLine());
			assertEquals("This macro will use WMIC to identify installed antivirus products", br.readLine());
			
			assertEquals("Windows Network Share Enumerator", br.readLine());
			assertEquals("enum_network_share", br.readLine());
			assertEquals("This macro will use WMIC to enumerate all network shares", br.readLine());
			
			assertEquals("Windows Patch Enumerator", br.readLine());
			assertEquals("enum_patches (wmic or ps)", br.readLine());
			assertEquals("This macro uses either WMIC (wmic) or PowerShell (ps) to list installed Windows patches", br.readLine());
			
			br.close();
			remote.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}
	
	@Test
	void test() {
		initiateServer();
		spawnClient(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);
		System.out.println("Transmitting commands");

		Time.sleepWrapped(5000);

		try {
			System.out.println("Connecting test commander...");
			Socket remote = LocalConnection.getSocket("127.0.0.1", Integer.parseInt(ClientServerTest.getDefaultSystemTestProperties().getProperty(Constants.SECURECOMMANDERPORT)), getDefaultSystemTestProperties());
			System.out.println("Locking test commander streams...");
			OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

			RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, TestConfiguration.getThisSystemOS() == OS.LINUX, false);

			bw.write("quit_session" + System.lineSeparator());
			bw.flush();

			RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, TestConfiguration.getThisSystemOS() == OS.LINUX, false);

			bw.write("list_sessions" + System.lineSeparator());
			bw.flush();
			RunnerTestGeneric.validateTwoSessionBanner(remote, bw, br, TestConfiguration.getThisSystemOS() == OS.LINUX, 2, false);

			// Test killing a session. First the session will disconnect, see that it isn't
			// there, then
			// reconnect and tell it to "die"
			bw.write("kill_session 2" + System.lineSeparator());
			bw.flush();

			String output = br.readLine();
			assertEquals(output, SessionInitiator.AVAILABLE_SESSION_BANNER);
			output = br.readLine();
			assertEquals(output, "1:default:default:default");
			output = br.readLine();
			assertEquals(output, "Enter 'WIZARD' to begin other server commands");
			bw.write("2" + System.lineSeparator());
			bw.flush();
			output = br.readLine();
			assertEquals(output, "Invalid Session id, will continue with prior session id.");
			bw.close();
			br.close();
			remote.close();

			// Wait long enough for a reconnect
			Time.sleepWrapped(2000);
			System.out.println("Connecting test commander...");
			remote = LocalConnection.getSocket("127.0.0.1", Integer.parseInt(ClientServerTest.getDefaultSystemTestProperties().getProperty(Constants.SECURECOMMANDERPORT)), getDefaultSystemTestProperties());
			System.out.println("Locking test commander streams...");
			bw = new OutputStreamWriter(remote.getOutputStream());
			br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

			RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, TestConfiguration.getThisSystemOS() == OS.LINUX, 3, false);
			bw.write("list_sessions" + System.lineSeparator());
			bw.flush();
			RunnerTestGeneric.validateTwoSessionBanner(remote, bw, br, TestConfiguration.getThisSystemOS() == OS.LINUX, 3, false);

			bw.write("die" + System.lineSeparator());
			bw.flush();
			bw.close();
			br.close();
			remote.close();

		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}

		awaitClient();
	}

}
