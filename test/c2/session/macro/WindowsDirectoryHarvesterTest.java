package c2.session.macro;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import c2.Commands;
import c2.Constants;
import c2.session.IOManager;
import c2.session.log.IOLogger;
import util.Time;
import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;

class WindowsDirectoryHarvesterTest {

	private Properties properties = ClientServerTest.getDefaultSystemTestProperties();
	
	public static final String USERPROFILE_EXAMPLE = " Volume in drive C is OS\r\n"
			+ " Volume Serial Number is AAA2-0F24\r\n" + "\r\n" + " Directory of C:\\Users\\test\r\n" + "\r\n"
			+ "08/19/2020  10:10 AM    <DIR>          .\r\n" + "08/19/2020  10:10 AM    <DIR>          ..\r\n"
			+ "08/19/2020  10:10 AM    <DIR>          SQL Server Management Studio\r\n"
			+ "               0 File(s)              0 bytes\r\n"
			+ "               3 Dir(s)  28,947,218,432 bytes free\r\n" + "";
	
	public static final String ONEDRIVE_EXAMPLE = " Volume in drive C is OS\r\n"
			+ " Volume Serial Number is AAA2-0F24\r\n" + "\r\n" + " Directory of C:\\Users\\test\\OneDrive\r\n" + "\r\n"
			+ "08/19/2020  10:10 AM    <DIR>          .\r\n" + "08/19/2020  10:10 AM    <DIR>          ..\r\n"
			+ "08/19/2020  10:10 AM    <DIR>          SQL Server Management Studio\r\n"
			+ "               0 File(s)              0 bytes\r\n"
			+ "               3 Dir(s)  28,947,218,432 bytes free\r\n" + "";
	
	public static final String BARF = " Volume in drive C is OS\r\n"
			+ " Volume Serial Number is AAA2-0F24\r\n"
			+ "\r\n"
			+ " Directory of C:\\Users\\matte\r\n"
			+ "\r\n"
			+ "File Not Found\r\n"
			+ "";
	
	private class ClientResponseEmulator implements Runnable {

		private IOManager io;
		private int id;

		private boolean alive = true;
		private boolean hasUserProfile;
		private boolean hasOnedrive;

		private boolean windows = true;
		
		public ClientResponseEmulator(IOManager io, int id, boolean hasUserProfile, boolean hasOnedrive) {
			this.io = io;
			this.id = id;
			this.hasOnedrive = hasOnedrive;
			this.hasUserProfile = hasUserProfile;
		}
		
		public void makeLinuxEmulator() {
			windows = false;
		}

		@Override
		public void run() {
			String expectedUserProfileDir = "C:\\Users\\test";
			String expectedOnedriveDir = "C:\\Users\\test\\OneDrive";
			
			int harvestCount = 0;
			while (alive) {
				String command = io.pollCommand(id);
				if (command == null) {
					// continue
					Time.sleepWrapped(10);
				} else if (command.equals(Commands.OS_HERITAGE)) {
					if(windows) {
						io.sendIO(id, Commands.OS_HERITAGE_RESPONSE_WINDOWS);
					}else {
						io.sendIO(id, Commands.OS_HERITAGE_RESPONSE_LINUX);
					}
				} else if (command.equals(Commands.PWD)) {
					io.sendIO(id, "C:\\test");
				} else if (command.equals(Commands.CD + " " + expectedUserProfileDir + "\\Desktop")) {
					io.sendIO(id, expectedUserProfileDir + "\\Desktop");
				} else if (command.equals(Commands.CD + " " + expectedUserProfileDir + "\\Documents")) {
					io.sendIO(id, expectedUserProfileDir + "\\Documents");
				} else if (command.equals(Commands.CD + " " + expectedOnedriveDir + "\\Desktop")) {
					io.sendIO(id, expectedOnedriveDir + "\\Desktop");
				} else if (command.equals(Commands.CD + " " + expectedOnedriveDir + "\\Documents")) {
					io.sendIO(id, expectedOnedriveDir + "\\Documents");
				} else if (command.equals(Commands.CD + " " + "C:\\test")) {
					io.sendIO(id, "C:\\test");
					alive = false;
				} else if (command.equals("dir %USERPROFILE%")) {
					if(hasUserProfile) {
						io.sendIO(id, USERPROFILE_EXAMPLE);
					}else {
						io.sendIO(id, BARF);
					}
				} else if (command.equals("dir %ONEDRIVE%")) {
					if(hasOnedrive) {
						io.sendIO(id, ONEDRIVE_EXAMPLE);
					}else {
						io.sendIO(id, BARF);
					}
				} else if (command.equals(Commands.HARVEST_CURRENT_DIRECTORY)) {
					if(hasUserProfile && hasOnedrive) {
						if (harvestCount == 1) {
						io.sendIO(id, "Started Harvest: " + expectedOnedriveDir + "\\Documents");
						} else if (harvestCount == 0) {
						io.sendIO(id, "Started Harvest: " + expectedOnedriveDir + "\\Desktop");
						} else if (harvestCount == 3) {
							io.sendIO(id, "Started Harvest: " + expectedUserProfileDir + "\\Documents");
						} else if (harvestCount == 2) {
							io.sendIO(id, "Started Harvest: " + expectedUserProfileDir + "\\Desktop");
						} else {
							io.sendIO(id, "You told me to harvest too many times!!!");
						}
					}else if(hasUserProfile) {
						if (harvestCount == 1) {
							io.sendIO(id, "Started Harvest: " + expectedUserProfileDir + "\\Documents");
						} else if (harvestCount == 0) {
							io.sendIO(id, "Started Harvest: " + expectedUserProfileDir + "\\Desktop");
						} else {
							io.sendIO(id, "You told me to harvest too many times!!!");
						}
					}else {//OneDrive only
						if (harvestCount == 1) {
							io.sendIO(id, "Started Harvest: " + expectedOnedriveDir + "\\Documents");
						} else if (harvestCount == 0) {
							io.sendIO(id, "Started Harvest: " + expectedOnedriveDir + "\\Desktop");
						} else {
							io.sendIO(id, "You told me to harvest too many times!!!");
						}
					}
					
					harvestCount++;
				} else {
					System.out.println("Unknown command: " + command);
				}
			}
		}

	}
	
	@AfterEach
	void cleanup() {
		RunnerTestGeneric.cleanLogs();
	}
	
	@Test
	void testRecognizesCommand() {
		IOLogger logger = new IOLogger(Paths.get(properties.getProperty(Constants.HUBLOGGINGPATH)));
		IOManager io = new IOManager(logger, null);
		WindowsDirectoryHarvester macro = new WindowsDirectoryHarvester();
		macro.initialize(io, null);
		assertTrue(macro.isCommandMatch(WindowsDirectoryHarvester.HARVEST_WINDOWS_USER_DIRS_CMD));
		assertFalse(macro.isCommandMatch("barf"));
	}
	
	@Test
	void testHarvestBothDirSets() {
		IOLogger logger = new IOLogger(Paths.get(properties.getProperty(Constants.HUBLOGGINGPATH)));
		IOManager io = new IOManager(logger, null);
		WindowsDirectoryHarvester macro = new WindowsDirectoryHarvester();
		macro.initialize(io, null);
		
		int id = io.addSession("user", "host", "protocol");

		// Flush the command buffer
		String cmd = io.pollCommand(id);
		while (cmd != null) {
			cmd = io.pollCommand(id);
		}
		
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientResponseEmulator em = new ClientResponseEmulator(io, id, true, true);
		exec.submit(em);
		
		MacroOutcome outcome = macro.processCmd(WindowsDirectoryHarvester.HARVEST_WINDOWS_USER_DIRS_CMD, id, "Not used");
		
		assertEquals(outcome.getOutput().get(0), "Sent Command: 'os_heritage'");
		assertEquals(outcome.getOutput().get(1), "Received response: 'Windows\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(2), "Sent Command: 'pwd'");
		assertEquals(outcome.getOutput().get(3), "Received response: 'C:\\test\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(4), "Macro Executor: 'Saving original working directory, proceeding with macro'");
		assertEquals(outcome.getOutput().get(5), "Macro Executor: 'Found OneDrive folder: C:\\Users\\test\\OneDrive'");
		assertEquals(outcome.getOutput().get(6), "Sent Command: 'cd C:\\Users\\test\\OneDrive\\Desktop'");
		assertEquals(outcome.getOutput().get(7), "Received response: 'C:\\Users\\test\\OneDrive\\Desktop\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(8), "Received response: 'Started Harvest: C:\\Users\\test\\OneDrive\\Desktop\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(9), "Sent Command: 'cd C:\\Users\\test\\OneDrive\\Documents'");
		assertEquals(outcome.getOutput().get(10), "Received response: 'C:\\Users\\test\\OneDrive\\Documents\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(11), "Received response: 'Started Harvest: C:\\Users\\test\\OneDrive\\Documents\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(12), "Macro Executor: 'Found user profile folder: C:\\Users\\test'");
		assertEquals(outcome.getOutput().get(13), "Sent Command: 'cd C:\\Users\\test\\Desktop'");
		assertEquals(outcome.getOutput().get(14), "Received response: 'C:\\Users\\test\\Desktop\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(15), "Received response: 'Started Harvest: C:\\Users\\test\\Desktop\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(16), "Sent Command: 'cd C:\\Users\\test\\Documents'");
		assertEquals(outcome.getOutput().get(17), "Received response: 'C:\\Users\\test\\Documents\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(18), "Received response: 'Started Harvest: C:\\Users\\test\\Documents\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(19), "Sent Command: 'cd C:\\test'");
		assertEquals(outcome.getOutput().get(20), "Received response: 'C:\\test'");
		assertEquals(outcome.getOutput().get(21), "Macro Executor: 'Original working directory resumed, harvest underway in the background if directories found'");
		
		
		assertFalse(outcome.hasErrors());
	}
	
	@Test
	void testHarvestUserProfile() {
		IOLogger logger = new IOLogger(Paths.get(properties.getProperty(Constants.HUBLOGGINGPATH)));
		IOManager io = new IOManager(logger, null);
		WindowsDirectoryHarvester macro = new WindowsDirectoryHarvester();
		macro.initialize(io, null);
	
		int id = io.addSession("user", "host", "protocol");

		// Flush the command buffer
		String cmd = io.pollCommand(id);
		while (cmd != null) {
			cmd = io.pollCommand(id);
		}
		
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientResponseEmulator em = new ClientResponseEmulator(io, id, true, false);
		exec.submit(em);
		
		MacroOutcome outcome = macro.processCmd(WindowsDirectoryHarvester.HARVEST_WINDOWS_USER_DIRS_CMD, id, "Not used");
		
		assertEquals(outcome.getOutput().get(0), "Sent Command: 'os_heritage'");
		assertEquals(outcome.getOutput().get(1), "Received response: 'Windows\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(2), "Sent Command: 'pwd'");
		assertEquals(outcome.getOutput().get(3), "Received response: 'C:\\test\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(4), "Macro Executor: 'Saving original working directory, proceeding with macro'");
		
		assertEquals(outcome.getOutput().get(5), "Macro Executor: 'Could not find OneDrive folder, proceeding.'");
		assertEquals(outcome.getOutput().get(6), "Macro Executor: 'Found user profile folder: C:\\Users\\test'");
		assertEquals(outcome.getOutput().get(7), "Sent Command: 'cd C:\\Users\\test\\Desktop'");
		assertEquals(outcome.getOutput().get(8), "Received response: 'C:\\Users\\test\\Desktop\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(9), "Received response: 'Started Harvest: C:\\Users\\test\\Desktop\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(10), "Sent Command: 'cd C:\\Users\\test\\Documents'");
		assertEquals(outcome.getOutput().get(11), "Received response: 'C:\\Users\\test\\Documents\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(12), "Received response: 'Started Harvest: C:\\Users\\test\\Documents\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(13), "Sent Command: 'cd C:\\test'");
		assertEquals(outcome.getOutput().get(14), "Received response: 'C:\\test'");
		assertEquals(outcome.getOutput().get(15), "Macro Executor: 'Original working directory resumed, harvest underway in the background if directories found'");
		
		assertFalse(outcome.hasErrors());
	}
	
	@Test 
	void testErrorsIfNotWindows() {
		IOLogger logger = new IOLogger(Paths.get(properties.getProperty(Constants.HUBLOGGINGPATH)));
		IOManager io = new IOManager(logger, null);
		WindowsDirectoryHarvester macro = new WindowsDirectoryHarvester();
		macro.initialize(io, null);
		
		int id = io.addSession("user", "host", "protocol");

		// Flush the command buffer
		String cmd = io.pollCommand(id);
		while (cmd != null) {
			cmd = io.pollCommand(id);
		}
		
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientResponseEmulator em = new ClientResponseEmulator(io, id, false, true);
		em.makeLinuxEmulator();
		exec.submit(em);
		
		MacroOutcome outcome = macro.processCmd(WindowsDirectoryHarvester.HARVEST_WINDOWS_USER_DIRS_CMD, id, "Not used");
		assertEquals(outcome.getOutput().get(0), "Sent Command: 'os_heritage'");
		assertEquals(outcome.getOutput().get(1), "Received response: 'Linux\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(2), "Error: Unsupported operating system: Linux");
		assertTrue(outcome.hasErrors());
	}
	
	@Test
	void testHarvestOneDrive() {
		IOLogger logger = new IOLogger(Paths.get(properties.getProperty(Constants.HUBLOGGINGPATH)));
		IOManager io = new IOManager(logger, null);
		WindowsDirectoryHarvester macro = new WindowsDirectoryHarvester();
		macro.initialize(io, null);
		
		int id = io.addSession("user", "host", "protocol");

		// Flush the command buffer
		String cmd = io.pollCommand(id);
		while (cmd != null) {
			cmd = io.pollCommand(id);
		}
		
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientResponseEmulator em = new ClientResponseEmulator(io, id, false, true);
		exec.submit(em);
		
		MacroOutcome outcome = macro.processCmd(WindowsDirectoryHarvester.HARVEST_WINDOWS_USER_DIRS_CMD, id, "Not used");
		
		assertEquals(outcome.getOutput().get(0), "Sent Command: 'os_heritage'");
		assertEquals(outcome.getOutput().get(1), "Received response: 'Windows\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(2), "Sent Command: 'pwd'");
		assertEquals(outcome.getOutput().get(3), "Received response: 'C:\\test\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(4), "Macro Executor: 'Saving original working directory, proceeding with macro'");
		assertEquals(outcome.getOutput().get(5), "Macro Executor: 'Found OneDrive folder: C:\\Users\\test\\OneDrive'");
		assertEquals(outcome.getOutput().get(6), "Sent Command: 'cd C:\\Users\\test\\OneDrive\\Desktop'");
		assertEquals(outcome.getOutput().get(7), "Received response: 'C:\\Users\\test\\OneDrive\\Desktop\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(8), "Received response: 'Started Harvest: C:\\Users\\test\\OneDrive\\Desktop\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(9), "Sent Command: 'cd C:\\Users\\test\\OneDrive\\Documents'");
		assertEquals(outcome.getOutput().get(10), "Received response: 'C:\\Users\\test\\OneDrive\\Documents\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(11), "Received response: 'Started Harvest: C:\\Users\\test\\OneDrive\\Documents\r\n"
				+ "'");
		assertEquals(outcome.getOutput().get(12), "Macro Executor: 'Could not find user profile folder, proceedind.'");
		assertEquals(outcome.getOutput().get(13), "Sent Command: 'cd C:\\test'");
		assertEquals(outcome.getOutput().get(14), "Received response: 'C:\\test'");
		assertEquals(outcome.getOutput().get(15), "Macro Executor: 'Original working directory resumed, harvest underway in the background if directories found'");
		
		assertFalse(outcome.hasErrors());
	
	}

}
