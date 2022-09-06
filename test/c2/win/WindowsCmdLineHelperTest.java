package c2.win;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import c2.Constants;
import c2.session.CommandLoader;
import c2.session.IOManager;
import c2.session.log.IOLogger;

public class WindowsCmdLineHelperTest {

	public static final String SYSTEMDRIVE_EXAMPLE = " Volume in drive C is OS\r\n"
			+ " Volume Serial Number is AAA2-0F24\r\n"
			+ "\r\n"
			+ " Directory of C:\\Users\\matte\r\n"
			+ "\r\n"
			+ "03/06/2022  11:35 PM    <DIR>          .\r\n"
			+ "03/06/2022  11:35 PM    <DIR>          ..\r\n"
			+ "07/03/2021  08:08 PM                57 .angular-config.json\r\n"
			+ "04/18/2022  10:34 AM             7,027 .bash_history\r\n"
			+ "06/27/2021  03:50 PM    <DIR>          .codemix\r\n"
			+ "06/27/2021  04:13 PM    <DIR>          .codemix-store\r\n"
			+ "06/27/2021  03:49 PM               104 .codemix.properties\r\n"
			+ "06/27/2021  04:08 PM    <DIR>          .config\r\n"
			+ "04/01/2021  10:58 AM    <DIR>          .dotnet\r\n"
			+ "04/16/2019  10:29 PM    <DIR>          .eclipse\r\n"
			+ "01/20/2020  10:25 PM                58 .gitconfig\r\n"
			+ "07/24/2020  02:46 PM    <DIR>          .idlerc\r\n"
			+ "06/14/2021  06:55 PM    <DIR>          .lemminx\r\n"
			+ "12/20/2019  11:49 AM    <DIR>          .m2\r\n"
			+ "02/20/2020  08:37 PM    <DIR>          .nuget\r\n"
			+ "04/25/2022  10:05 AM    <DIR>          .p2\r\n"
			+ "08/16/2019  02:18 PM    <DIR>          .ssh\r\n"
			+ "02/03/2021  04:51 PM    <DIR>          .templateengine\r\n"
			+ "04/16/2019  10:29 PM    <DIR>          .tooling\r\n"
			+ "02/10/2021  01:13 PM             1,187 .viminfo\r\n"
			+ "04/25/2022  10:01 AM    <DIR>          .VirtualBox\r\n"
			+ "04/25/2022  10:05 AM    <DIR>          .webclipse\r\n"
			+ "03/28/2021  02:31 PM    <DIR>          3D Objects\r\n"
			+ "03/28/2021  02:31 PM    <DIR>          Contacts\r\n"
			+ "12/21/2021  11:26 AM    <DIR>          Desktop\r\n"
			+ "08/19/2020  10:10 AM    <DIR>          Documents\r\n"
			+ "04/21/2022  07:29 PM    <DIR>          Downloads\r\n"
			+ "02/08/2021  08:30 PM    <DIR>          eclipse\r\n"
			+ "06/14/2021  06:51 PM    <DIR>          eclipse-workspace\r\n"
			+ "03/28/2021  02:31 PM    <DIR>          Favorites\r\n"
			+ "09/26/2021  08:26 PM    <DIR>          GCS\r\n"
			+ "06/14/2021  03:23 PM    <DIR>          git\r\n"
			+ "03/28/2021  02:31 PM    <DIR>          Links\r\n"
			+ "03/28/2021  02:31 PM    <DIR>          Music\r\n"
			+ "04/18/2022  09:02 PM    <DIR>          OneDrive\r\n"
			+ "03/28/2021  02:31 PM    <DIR>          Saved Games\r\n"
			+ "03/28/2021  02:31 PM    <DIR>          Searches\r\n"
			+ "09/15/2019  07:46 PM    <DIR>          source\r\n"
			+ "03/28/2021  02:31 PM    <DIR>          Videos\r\n"
			+ "05/25/2021  09:28 AM    <DIR>          VirtualBox VMs\r\n"
			+ "               5 File(s)          8,433 bytes\r\n"
			+ "              35 Dir(s)  11,233,517,568 bytes free\r\n"
			+ "";
	
	public static final String APPDATA_EXAMPLE = " Volume in drive C is OS\r\n"
			+ " Volume Serial Number is AAA2-0F24\r\n"
			+ "\r\n"
			+ " Directory of C:\\Users\\matte\\AppData\\Roaming\r\n"
			+ "\r\n"
			+ "03/07/2022  11:31 AM    <DIR>          .\r\n"
			+ "03/07/2022  11:31 AM    <DIR>          ..\r\n"
			+ "03/11/2019  09:16 PM    <DIR>          Adobe\r\n"
			+ "08/18/2020  08:21 PM    <DIR>          Battle.net\r\n"
			+ "08/19/2019  04:34 PM    <DIR>          com.pyromancers.dps\r\n"
			+ "04/25/2022  10:04 AM    <DIR>          discord\r\n"
			+ "03/11/2019  09:19 PM    <DIR>          Intel Corporation\r\n"
			+ "03/17/2019  11:16 PM    <DIR>          launcher\r\n"
			+ "03/11/2019  09:20 PM    <DIR>          Macromedia\r\n"
			+ "02/25/2020  08:47 PM    <DIR>          Mael Horz\r\n"
			+ "01/02/2020  02:57 PM    <DIR>          Microsoft FxCop\r\n"
			+ "11/05/2019  09:07 PM    <DIR>          Mozilla\r\n"
			+ "03/19/2022  09:18 PM    <DIR>          Mumble\r\n"
			+ "09/15/2019  07:48 PM    <DIR>          Notepad++\r\n"
			+ "07/03/2021  08:08 PM    <DIR>          npm\r\n"
			+ "07/03/2021  08:08 PM    <DIR>          npm-cache\r\n"
			+ "09/15/2019  07:57 PM    <DIR>          NuGet\r\n"
			+ "10/20/2020  07:49 PM    <DIR>          NVIDIA\r\n"
			+ "07/16/2020  08:10 AM    <DIR>          Skype\r\n"
			+ "04/25/2022  11:21 AM    <DIR>          Slack\r\n"
			+ "12/01/2020  10:05 PM    <DIR>          STVEF\r\n"
			+ "01/15/2021  12:30 PM    <DIR>          Teams\r\n"
			+ "11/24/2021  09:09 PM    <DIR>          TempleGates\r\n"
			+ "06/22/2019  02:23 PM    <DIR>          The Creative Assembly\r\n"
			+ "10/27/2021  08:08 PM    <DIR>          Valve Corporation\r\n"
			+ "09/15/2019  07:51 PM    <DIR>          Visual Studio Setup\r\n"
			+ "11/21/2021  12:10 AM    <DIR>          vlc\r\n"
			+ "09/15/2019  07:31 PM    <DIR>          vstelemetry\r\n"
			+ "09/15/2019  07:31 PM    <DIR>          vs_installershell\r\n"
			+ "01/03/2021  08:18 PM    <DIR>          Waves Audio\r\n"
			+ "12/06/2020  06:37 PM    <DIR>          Wireshark\r\n"
			+ "12/09/2021  12:58 PM    <DIR>          Zoom\r\n"
			+ "               0 File(s)              0 bytes\r\n"
			+ "              32 Dir(s)  11,234,893,824 bytes free\r\n"
			+ "";
	
	IOManager io;
	int sessionId;

	@BeforeEach
	void setUp() throws Exception {
		Path testPath = null;
		if (System.getProperty("os.name").contains("Windows")) {
			testPath = Paths.get("config", "test.properties");
		}else {
			testPath = Paths.get("config", "test_linux.properties");
		}
		try (InputStream input = new FileInputStream(testPath.toFile())) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			CommandLoader cl = new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>());
			io = new IOManager(new IOLogger(Paths.get(prop.getProperty(Constants.HUBLOGGINGPATH))), cl);

			sessionId = io.addSession("noone", "testHost", "protocol");
		} catch (IOException ex) {
			System.out.println("Unable to load config file");
		}
	}
	
	@Test
	void testIsClientElevated() {
		io.sendIO(sessionId, "Access is denied.");
		assertFalse(WindowsCmdLineHelper.isClientElevated(sessionId, io));
		io.sendIO(sessionId, "There are no entries in the list.");
		assertTrue(WindowsCmdLineHelper.isClientElevated(sessionId, io));

		// Flush commands
		String out = io.pollCommand(sessionId);
		assertEquals(out, "net session 2>&1");
		out = io.pollCommand(sessionId);
		assertEquals(out, "net session 2>&1");
	}

	@Test
	void testResolveAppData() {
		io.sendIO(sessionId, APPDATA_EXAMPLE);
		try {
			String location = WindowsCmdLineHelper.resolveAppData(io, sessionId);
			assertEquals("C:\\Users\\matte\\AppData\\Roaming", location);
			String command = io.pollCommand(sessionId);
			assertEquals("dir %APPDATA%", command);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	void testResolveVariableData() {
		io.sendIO(sessionId, SYSTEMDRIVE_EXAMPLE);
		try {
			String testVar = "%SYSTEMDRIVE%";
			String location = WindowsCmdLineHelper.resolveVariableDirectory(io, sessionId, testVar);
			assertEquals("C:\\Users\\matte", location);
			String command = io.pollCommand(sessionId);
			assertEquals("dir " + testVar, command);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
}
