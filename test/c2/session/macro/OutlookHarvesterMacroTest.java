package c2.session.macro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
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
import c2.session.CommandLoader;
import c2.session.IOManager;
import c2.session.log.IOLogger;
import util.Time;
import util.test.ClientServerTest;
import util.test.OutputStreamWriterHelper;
import util.test.RunnerTestGeneric;
import util.test.TestCommons;
import util.test.TestConstants;

class OutlookHarvesterMacroTest extends ClientServerTest {

	private Properties properties = ClientServerTest.getDefaultSystemTestProperties();

	public static final String USERPROFILE_EXAMPLE = " Volume in drive C is OS\r\n"
			+ " Volume Serial Number is AAA2-0F24\r\n" + "\r\n" + " Directory of C:\\Users\\test\r\n" + "\r\n"
			+ "08/19/2020  10:10 AM    <DIR>          .\r\n" + "08/19/2020  10:10 AM    <DIR>          ..\r\n"
			+ "08/19/2020  10:10 AM    <DIR>          SQL Server Management Studio\r\n"
			+ "               0 File(s)              0 bytes\r\n"
			+ "               3 Dir(s)  28,947,218,432 bytes free\r\n" + "";

	public static final String APPDATA_EXAMPLE = " Volume in drive C is OS\r\n"
			+ " Volume Serial Number is AAA2-0F24\r\n" + "\r\n" + " Directory of C:\\testuser\\adir\r\n" + "\r\n"
			+ "03/07/2022  11:31 AM    <DIR>          .\r\n" + "03/07/2022  11:31 AM    <DIR>          ..\r\n"
			+ "03/11/2019  09:16 PM    <DIR>          Adobe\r\n"
			+ "08/18/2020  08:21 PM    <DIR>          Battle.net\r\n"
			+ "08/19/2019  04:34 PM    <DIR>          com.pyromancers.dps\r\n"
			+ "04/25/2022  10:04 AM    <DIR>          discord\r\n"
			+ "03/11/2019  09:19 PM    <DIR>          Intel Corporation\r\n"
			+ "03/17/2019  11:16 PM    <DIR>          launcher\r\n"
			+ "03/11/2019  09:20 PM    <DIR>          Macromedia\r\n"
			+ "02/25/2020  08:47 PM    <DIR>          Mael Horz\r\n"
			+ "01/02/2020  02:57 PM    <DIR>          Microsoft FxCop\r\n"
			+ "11/05/2019  09:07 PM    <DIR>          Mozilla\r\n" + "03/19/2022  09:18 PM    <DIR>          Mumble\r\n"
			+ "09/15/2019  07:48 PM    <DIR>          Notepad++\r\n" + "07/03/2021  08:08 PM    <DIR>          npm\r\n"
			+ "07/03/2021  08:08 PM    <DIR>          npm-cache\r\n"
			+ "09/15/2019  07:57 PM    <DIR>          NuGet\r\n" + "10/20/2020  07:49 PM    <DIR>          NVIDIA\r\n"
			+ "07/16/2020  08:10 AM    <DIR>          Skype\r\n" + "04/25/2022  11:21 AM    <DIR>          Slack\r\n"
			+ "12/01/2020  10:05 PM    <DIR>          STVEF\r\n" + "01/15/2021  12:30 PM    <DIR>          Teams\r\n"
			+ "11/24/2021  09:09 PM    <DIR>          TempleGates\r\n"
			+ "06/22/2019  02:23 PM    <DIR>          The Creative Assembly\r\n"
			+ "10/27/2021  08:08 PM    <DIR>          Valve Corporation\r\n"
			+ "09/15/2019  07:51 PM    <DIR>          Visual Studio Setup\r\n"
			+ "11/21/2021  12:10 AM    <DIR>          vlc\r\n"
			+ "09/15/2019  07:31 PM    <DIR>          vstelemetry\r\n"
			+ "09/15/2019  07:31 PM    <DIR>          vs_installershell\r\n"
			+ "01/03/2021  08:18 PM    <DIR>          Waves Audio\r\n"
			+ "12/06/2020  06:37 PM    <DIR>          Wireshark\r\n" + "12/09/2021  12:58 PM    <DIR>          Zoom\r\n"
			+ "               0 File(s)              0 bytes\r\n"
			+ "              32 Dir(s)  11,234,893,824 bytes free\r\n" + "";

	private class ClientResponseEmulator implements Runnable {

		private IOManager io;
		private int id;

		private boolean alive = true;
		private boolean basicHarvest;

		public ClientResponseEmulator(IOManager io, int id, boolean basicHarvest) {
			this.io = io;
			this.id = id;
			this.basicHarvest = basicHarvest;
		}

		public void kill() {
			alive = false;
		}

		@Override
		public void run() {
			int harvestCount = 0;
			while (alive) {
				String command = io.pollCommand(id);
				String expectedPSTDir = OutlookHarvesterMacro.OUTLOOK_PST_DIR.replace("%USERPROFILE%",
						"C:\\Users\\test");
				String expectedOSTDir = OutlookHarvesterMacro.OUTLOOK_OST_DIR.replace("%APPDATA%",
						"C:\\testuser\\adir");

				if (command == null) {
					// continue
					Time.sleepWrapped(10);
				} else if (command.equals(Commands.PWD)) {
					io.sendIO(id, "C:\\test");
				} else if (command.equals(Commands.CD + " " + expectedPSTDir)) {
					io.sendIO(id, expectedPSTDir);
				} else if (command.equals(Commands.CD + " " + expectedOSTDir)) {
					io.sendIO(id, expectedOSTDir);
				} else if (command.equals(Commands.CD + " " + "C:\\test")) {
					io.sendIO(id, "C:\\test");
					alive = false;
				} else if (command.equals("dir %APPDATA%")) {
					io.sendIO(id, APPDATA_EXAMPLE);
				} else if (command.equals("dir %USERPROFILE%")) {
					io.sendIO(id, USERPROFILE_EXAMPLE);
				} else if (command.equals(Commands.HARVEST_CURRENT_DIRECTORY)) {
					if (basicHarvest) {
						if (harvestCount == 0) {
							io.sendIO(id, "Started Harvest: " + expectedPSTDir);
						} else if (harvestCount == 1) {
							io.sendIO(id, "Started Harvest: " + expectedOSTDir);
						} else {
							io.sendIO(id, "You told me to harvest too many times!!!");
						}
					} else {
						if (harvestCount == 0) {
							io.sendIO(id, "Started Harvest: C:\\user\\fakedir");
						} else {
							io.sendIO(id, "You told me to harvest too many times!!!");
						}
					}
					harvestCount++;
				} else if (command.equals("where /r C:\\ *.pst")) {
					io.sendIO(id, "Attempting search with 10 minute timeout");
					io.sendIO(id, "C:\\user\\fakedir\\mystuff.pst");
					io.sendIO(id, "Search complete");
				} else if (command.equals(Commands.CD + " C:\\user\\fakedir")) {
					io.sendIO(id, "C:\\user\\fakedir" + System.lineSeparator());
				} else {
					System.out.println("Unknown command: " + command);
				}
			}
		}

	}

	@AfterEach
	void cleanup() {
		RunnerTestGeneric.cleanLogs();

		TestCommons.cleanFileHarvesterDir();
	}

	@Test
	void testRecognizesCommandAndRejectsNonCommand() {
		IOLogger logger = new IOLogger(Paths.get(properties.getProperty(Constants.HUBLOGGINGPATH)));
		IOManager io = new IOManager(logger, null);
		OutlookHarvesterMacro macro = new OutlookHarvesterMacro();
		macro.initialize(io, null);
		assertTrue(macro.isCommandMatch(OutlookHarvesterMacro.OUTLOOK_HARVEST_COMMAND + " "
				+ OutlookHarvesterMacro.OUTLOOK_HARVEST_DEEP_SEARCH));
		assertFalse(macro.isCommandMatch("NARF"));
	}

	@Test
	void testNoArgumentsErrorMessage() {
		IOLogger logger = new IOLogger(Paths.get(properties.getProperty(Constants.HUBLOGGINGPATH)));
		IOManager io = new IOManager(logger, null);
		OutlookHarvesterMacro macro = new OutlookHarvesterMacro();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(OutlookHarvesterMacro.OUTLOOK_HARVEST_COMMAND, 1, "doesntmatter");
		assertEquals(1, outcome.getErrors().size());
		assertEquals("Improper format of command: " + OutlookHarvesterMacro.OUTLOOK_HARVEST_COMMAND
				+ " - at least one argument required", outcome.getErrors().get(0));
	}

	@Test
	void testBadArgumentErrorMessage() {
		IOLogger logger = new IOLogger(Paths.get(properties.getProperty(Constants.HUBLOGGINGPATH)));
		IOManager io = new IOManager(logger, null);
		OutlookHarvesterMacro macro = new OutlookHarvesterMacro();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(OutlookHarvesterMacro.OUTLOOK_HARVEST_COMMAND + " fake_arg", 1,
				"doesntmatter");
		assertEquals(1, outcome.getErrors().size());
		assertEquals("Improper argument of command: " + OutlookHarvesterMacro.OUTLOOK_HARVEST_COMMAND
				+ " 'basic' for harvesting default directories or 'deep' for searching for non-traditional PST file locations",
				outcome.getErrors().get(0));
	}

	private IOManager io;

	@BeforeEach
	void setUp() {
		CommandLoader cl = new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>());
		io = new IOManager(new IOLogger(Paths.get(properties.getProperty(Constants.HUBLOGGINGPATH))), cl);
		TestCommons.cleanFileHarvesterDir();
	}

	@Test
	void testBasicHarvestCommandsIssuedAgainstHarness() {
		int id = io.addSession("user", "host", "protocol");

		// Flush the command buffer
		String cmd = io.pollCommand(id);
		while (cmd != null) {
			cmd = io.pollCommand(id);
		}

		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientResponseEmulator em = new ClientResponseEmulator(io, id, true);
		exec.submit(em);

		OutlookHarvesterMacro macro = new OutlookHarvesterMacro();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(
				OutlookHarvesterMacro.OUTLOOK_HARVEST_COMMAND + " " + OutlookHarvesterMacro.OUTLOOK_HARVEST_BASIC, id,
				"ignored");
		assertEquals("Macro Executor: 'Resolved APPDATA: C:\\testuser\\adir'", outcome.getOutput().get(0));
		assertEquals("Macro Executor: 'Resolved USERPROFILE: C:\\Users\\test'", outcome.getOutput().get(1));
		assertEquals("Sent Command: 'pwd'", outcome.getOutput().get(2));
		assertEquals("Received response: 'C:\\test" + System.lineSeparator() + "'", outcome.getOutput().get(3));
		assertEquals("Macro Executor: 'Saving original working directory, proceeding with Outlook harvest'",
				outcome.getOutput().get(4));
		assertEquals("Sent Command: 'cd C:\\Users\\test\\Documents\\Outlook Files'", outcome.getOutput().get(5));
		assertEquals("Received response: 'C:\\Users\\test\\Documents\\Outlook Files" + System.lineSeparator() + "'",
				outcome.getOutput().get(6));
		assertEquals("Sent Command: 'harvest_pwd'", outcome.getOutput().get(7));
		assertEquals("Received response: 'Started Harvest: C:\\Users\\test\\Documents\\Outlook Files"
				+ System.lineSeparator() + "'", outcome.getOutput().get(8));
		assertEquals("Sent Command: 'cd C:\\testuser\\adir\\Local\\Microsoft\\Outlook'", outcome.getOutput().get(9));
		assertEquals("Received response: 'C:\\testuser\\adir\\Local\\Microsoft\\Outlook" + System.lineSeparator() + "'",
				outcome.getOutput().get(10));
		assertEquals("Sent Command: 'harvest_pwd'", outcome.getOutput().get(11));
		assertEquals("Received response: 'Started Harvest: C:\\testuser\\adir\\Local\\Microsoft\\Outlook"
				+ System.lineSeparator() + "'", outcome.getOutput().get(12));
		assertEquals("Sent Command: 'cd C:\\test'", outcome.getOutput().get(13));
		assertEquals("Received response: 'C:\\test" + System.lineSeparator() + "'", outcome.getOutput().get(14));
		assertEquals(
				"Macro Executor: 'Original working directory resumed, harvest underway in the background if directories found'",
				outcome.getOutput().get(15));
	}

	@Test
	void testDeepHarvestSearchAndHarvestExecutedAgainstHarness() {
		int id = io.addSession("user", "host", "protocol");

		// Flush the command buffer
		String cmd = io.pollCommand(id);
		while (cmd != null) {
			cmd = io.pollCommand(id);
		}

		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientResponseEmulator em = new ClientResponseEmulator(io, id, false);
		exec.submit(em);

		OutlookHarvesterMacro macro = new OutlookHarvesterMacro();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(
				OutlookHarvesterMacro.OUTLOOK_HARVEST_COMMAND + " " + OutlookHarvesterMacro.OUTLOOK_HARVEST_DEEP_SEARCH,
				id, "ignored");
		assertEquals("Sent Command: 'pwd'", outcome.getOutput().get(0));
		assertEquals("Received response: 'C:\\test" + System.lineSeparator() + "'", outcome.getOutput().get(1));
		assertEquals("Macro Executor: 'Saving original working directory, proceeding with Outlook harvest'",
				outcome.getOutput().get(2));
		assertEquals("Sent Command: 'where /r C:\\ *.pst'", outcome.getOutput().get(3));
		assertEquals("Received response: 'Attempting search with 10 minute timeout" + System.lineSeparator()
				+ "C:\\user\\fakedir\\mystuff.pst" + System.lineSeparator() + "Search complete" + System.lineSeparator()
				+ "'", outcome.getOutput().get(4));
		assertEquals("Sent Command: 'cd C:\\user\\fakedir'", outcome.getOutput().get(5));
		assertEquals("Received response: 'C:\\user\\fakedir" + System.lineSeparator() + System.lineSeparator() + "'", outcome.getOutput().get(6));
		assertEquals("Sent Command: 'harvest_pwd'", outcome.getOutput().get(7));
		assertEquals("Received response: 'Started Harvest: C:\\user\\fakedir" + System.lineSeparator() + "'",
				outcome.getOutput().get(8));
		assertEquals("Sent Command: 'cd C:\\test'", outcome.getOutput().get(9));
		assertEquals("Received response: 'C:\\test"+ System.lineSeparator()+"'", outcome.getOutput().get(10));
		assertEquals(
				"Macro Executor: 'Original working directory resumed, harvest underway in the background if directories found'",
				outcome.getOutput().get(11));
	}

	Path determineReferencePSTFile() {
		Path rootPath;
		Path documentsRoot = Paths.get("C:", "Users", System.getProperty("user.name"), "Documents", "Outlook Files");
		Path oneDriveDocumentsRoot = Paths.get("C:", "Users", System.getProperty("user.name"), "OneDrive", "Documents",
				"Outlook Files");
		if (Files.exists(documentsRoot)) {
			rootPath = documentsRoot;
		} else {
			rootPath = oneDriveDocumentsRoot;
		}

		File[] matches = rootPath.toFile().listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".pst");
			}
		});
		if (matches != null && matches.length == 1) {
			return matches[0].toPath();
		} else {
			System.out.println("Warning: no PST file found");
			return null;
		}
	}

	Path determineReferenceOSTFile() {
		String appdata = System.getenv("APPDATA");
		Path ostDir;
		Path defaultOSTDir = Paths.get(OutlookHarvesterMacro.OUTLOOK_OST_DIR.replace("%APPDATA%", appdata));
		if (Files.exists(defaultOSTDir)) {
			ostDir = defaultOSTDir;
		} else {
			ostDir = Paths
					.get(OutlookHarvesterMacro.OUTLOOK_OST_WITH_APPDATA_ROAMING_DIR.replace("%APPDATA%", appdata));
		}
		File[] matches = ostDir.toFile().listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".ost");
			}
		});
		if (matches.length == 1) {
			return matches[0].toPath();
		} else {
			System.out.println("Warning: no OST file found");
			return null;
		}
	}

	@Test
	void testBasicHarvestLiveOS() {
		// This test is only enabled by user choice, as we don't want to test with
		// actual user data if someone
		// isn't reading the fine print and doesn't know what they're doing
		if (TestConstants.OUTLOOKHARVEST_LIVE_ENABLE && System.getProperty("os.name").contains("Windows")) {
			Path referencePST = determineReferencePSTFile();
			Path referenceOST = determineReferenceOSTFile();

			initiateServer();
			spawnClient(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);

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

				Time.sleepWrapped(500);
				System.out.println("Setting up test commander session...");
				try {
					RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, false, false);
				} catch (Exception ex) {
					fail(ex.getMessage());
				}

				OutputStreamWriterHelper.writeAndSend(bw, OutlookHarvesterMacro.OUTLOOK_HARVEST_COMMAND + " "
						+ OutlookHarvesterMacro.OUTLOOK_HARVEST_BASIC);

				Time.sleepWrapped(150000);

				// TODO: dynamically load from configuration this directory
				File[] directories = new File(
						Paths.get(TestCommons.HARVEST_LANDING_DIR.toString(), InetAddress.getLocalHost().getHostName())
								.toString()).listFiles(File::isDirectory);
				assertEquals(2, directories.length);

				Path pstUpload = Paths.get(directories[0].toPath().toString(), "c",
						referencePST.subpath(0, referencePST.getNameCount()).toString());
				System.out.println(pstUpload.toString());
				assertTrue(Files.exists(pstUpload));
				assertEquals(Files.size(referencePST), Files.size(pstUpload));

				Path ostUpload = Paths.get(directories[1].toPath().toString(), "c",
						referenceOST.subpath(0, referenceOST.getNameCount()).toString());
				System.out.println(ostUpload.toString());
				assertTrue(Files.exists(ostUpload));
				assertEquals(Files.size(referenceOST), Files.size(ostUpload));

				OutputStreamWriterHelper.writeAndSend(bw, "die");

				awaitClient();
			} catch (IOException ex) {

			}
			teardown();
		} else {
			System.out.println(
					"WARNING: Skipping Outlook harvester test with local Outlook instance. If you want this to be configured, enable: "
							+ TestConstants.I_OUTLOOKHARVEST_LIVE_ENABLE);
		}
	}

	@Test
	void testDeepSearchHarvestLiveOS() {
		// This test is only enabled by user choice, as we don't want to test with
		// actual user data if someone
		// isn't reading the fine print and doesn't know what they're doing
		if (TestConstants.OUTLOOKHARVEST_LIVE_ENABLE && System.getProperty("os.name").contains("Windows")) {
			Path referencePST = determineReferencePSTFile();

			initiateServer();
			spawnClient(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);

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

				Time.sleepWrapped(500);
				System.out.println("Setting up test commander session...");
				try {
					RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, false, false);
				} catch (Exception ex) {
					fail(ex.getMessage());
				}

				String command = OutlookHarvesterMacro.OUTLOOK_HARVEST_COMMAND + " "
						+ OutlookHarvesterMacro.OUTLOOK_HARVEST_DEEP_SEARCH + " C:\\Users\\"
						+ System.getProperty("user.name") + "\\OneDrive\\Documents";
				System.out.println(command);
				OutputStreamWriterHelper.writeAndSend(bw, command);

				Time.sleepWrapped(60000);

				// TODO: dynamically load from configuration this directory
				File[] directories = new File(
						Paths.get(TestCommons.HARVEST_LANDING_DIR.toString(), InetAddress.getLocalHost().getHostName())
								.toString()).listFiles(File::isDirectory);
				assertTrue(directories != null);
				assertEquals(1, directories.length);

				Path pstUpload = Paths.get(directories[0].toPath().toString(), "c",
						referencePST.subpath(0, referencePST.getNameCount()).toString());
				System.out.println(pstUpload.toString());
				assertTrue(Files.exists(pstUpload));
				assertEquals(Files.size(referencePST), Files.size(pstUpload));

				OutputStreamWriterHelper.writeAndSend(bw, "die");

				awaitClient();
			} catch (IOException ex) {

			}
			teardown();
		} else {
			System.out.println(
					"WARNING: Skipping Outlook harvester test with local Outlook instance. If you want this to be configured, enable: "
							+ TestConstants.I_OUTLOOKHARVEST_LIVE_ENABLE);
		}
	}

}
