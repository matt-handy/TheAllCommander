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

class UserDirectoryHarvesterTest {

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
		private boolean invalidOS = false;
		
		public ClientResponseEmulator(IOManager io, int id, boolean hasUserProfile, boolean hasOnedrive) {
			this.io = io;
			this.id = id;
			this.hasOnedrive = hasOnedrive;
			this.hasUserProfile = hasUserProfile;
		}
		
		public void makeLinuxEmulator() {
			windows = false;
		}
		
		public void makeInvalidOs() {
			invalidOS = true;
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
				} else if (command.equals(Commands.CLIENT_CMD_OS_HERITAGE)) {
					if(invalidOS) {
						io.sendIO(id, Commands.OS_HERITAGE_RESPONSE_MAC);
					}else {
					if(windows) {
						io.sendIO(id, Commands.OS_HERITAGE_RESPONSE_WINDOWS);
					}else {
						io.sendIO(id, Commands.OS_HERITAGE_RESPONSE_LINUX);
					}
					}
				} else if (command.equals(Commands.CLIENT_CMD_PWD)) {
					if(windows) {
						io.sendIO(id, "C:\\test");
					}else {
						io.sendIO(id, "/home/kali/working");
					}
				}else if(command.equals(Commands.CLIENT_CMD_CD + " /home/kali/working")) { 
					io.sendIO(id, "/home/kali/working");
					alive = false;
				}else if(command.equals(Commands.CLIENT_CMD_CD + " ~")) {
					io.sendIO(id, "/home/kali");
				} else if (command.equals(Commands.CLIENT_CMD_CD + " " + expectedUserProfileDir + "\\Desktop")) {
					io.sendIO(id, expectedUserProfileDir + "\\Desktop");
				} else if (command.equals(Commands.CLIENT_CMD_CD + " " + expectedUserProfileDir + "\\Documents")) {
					io.sendIO(id, expectedUserProfileDir + "\\Documents");
				} else if (command.equals(Commands.CLIENT_CMD_CD + " " + expectedOnedriveDir + "\\Desktop")) {
					io.sendIO(id, expectedOnedriveDir + "\\Desktop");
				} else if (command.equals(Commands.CLIENT_CMD_CD + " " + expectedOnedriveDir + "\\Documents")) {
					io.sendIO(id, expectedOnedriveDir + "\\Documents");
				} else if (command.equals(Commands.CLIENT_CMD_CD + " " + "C:\\test")) {
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
				} else if (command.equals(Commands.CLIENT_CMD_HARVEST_CURRENT_DIRECTORY)) {
					if(!windows) {
						io.sendIO(id, "Started Harvest: /home/kali");
					}else {
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
		UserDirectoryHarvester macro = new UserDirectoryHarvester();
		macro.initialize(io, null);
		assertTrue(macro.isCommandMatch(UserDirectoryHarvester.HARVEST_USER_DIRS_CMD));
		assertFalse(macro.isCommandMatch("barf"));
	}
	
	@Test
	void testHarvestBothDirSets() {
		IOLogger logger = new IOLogger(Paths.get(properties.getProperty(Constants.HUBLOGGINGPATH)));
		IOManager io = new IOManager(logger, null);
		UserDirectoryHarvester macro = new UserDirectoryHarvester();
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
		
		MacroOutcome outcome = macro.processCmd(UserDirectoryHarvester.HARVEST_USER_DIRS_CMD, id, "Not used");
		
		assertEquals(outcome.getOutput().get(0), "Sent Command: 'os_heritage'");
		assertEquals(outcome.getOutput().get(1), "Received response: 'Windows" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(2), "Sent Command: 'pwd'");
		assertEquals(outcome.getOutput().get(3), "Received response: 'C:\\test" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(4), "Macro Executor: 'Saving original working directory, proceeding with macro'");
		assertEquals(outcome.getOutput().get(5), "Macro Executor: 'Found OneDrive folder: C:\\Users\\test\\OneDrive'");
		assertEquals(outcome.getOutput().get(6), "Sent Command: 'cd C:\\Users\\test\\OneDrive\\Desktop'");
		assertEquals(outcome.getOutput().get(7), "Received response: 'C:\\Users\\test\\OneDrive\\Desktop" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(8), "Sent Command: 'harvest_pwd'");
		assertEquals(outcome.getOutput().get(9), "Received response: 'Started Harvest: C:\\Users\\test\\OneDrive\\Desktop" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(10), "Sent Command: 'cd C:\\Users\\test\\OneDrive\\Documents'");
		assertEquals(outcome.getOutput().get(11), "Received response: 'C:\\Users\\test\\OneDrive\\Documents" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(12), "Sent Command: 'harvest_pwd'");
		assertEquals(outcome.getOutput().get(13), "Received response: 'Started Harvest: C:\\Users\\test\\OneDrive\\Documents" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(14), "Macro Executor: 'Found user profile folder: C:\\Users\\test'");
		assertEquals(outcome.getOutput().get(15), "Sent Command: 'cd C:\\Users\\test\\Desktop'");
		assertEquals(outcome.getOutput().get(16), "Received response: 'C:\\Users\\test\\Desktop" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(17), "Sent Command: 'harvest_pwd'");
		assertEquals(outcome.getOutput().get(18), "Received response: 'Started Harvest: C:\\Users\\test\\Desktop" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(19), "Sent Command: 'cd C:\\Users\\test\\Documents'");
		assertEquals(outcome.getOutput().get(20), "Received response: 'C:\\Users\\test\\Documents" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(21), "Sent Command: 'harvest_pwd'");
		assertEquals(outcome.getOutput().get(22), "Received response: 'Started Harvest: C:\\Users\\test\\Documents" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(23), "Sent Command: 'cd C:\\test'");
		assertEquals(outcome.getOutput().get(24), "Received response: 'C:\\test" + System.lineSeparator()
		+ "'");
		assertEquals(outcome.getOutput().get(25), "Macro Executor: 'Original working directory resumed, harvest underway in the background if directories found'");
		
		
		assertFalse(outcome.hasErrors());
	}
	
	@Test
	void testHarvestUserProfile() {
		IOLogger logger = new IOLogger(Paths.get(properties.getProperty(Constants.HUBLOGGINGPATH)));
		IOManager io = new IOManager(logger, null);
		UserDirectoryHarvester macro = new UserDirectoryHarvester();
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
		
		MacroOutcome outcome = macro.processCmd(UserDirectoryHarvester.HARVEST_USER_DIRS_CMD, id, "Not used");
		
		assertEquals(outcome.getOutput().get(0), "Sent Command: 'os_heritage'");
		assertEquals(outcome.getOutput().get(1), "Received response: 'Windows" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(2), "Sent Command: 'pwd'");
		assertEquals(outcome.getOutput().get(3), "Received response: 'C:\\test" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(4), "Macro Executor: 'Saving original working directory, proceeding with macro'");
		
		assertEquals(outcome.getOutput().get(5), "Macro Executor: 'Could not find OneDrive folder, proceeding.'");
		assertEquals(outcome.getOutput().get(6), "Macro Executor: 'Found user profile folder: C:\\Users\\test'");
		assertEquals(outcome.getOutput().get(7), "Sent Command: 'cd C:\\Users\\test\\Desktop'");
		assertEquals(outcome.getOutput().get(8), "Received response: 'C:\\Users\\test\\Desktop" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(9), "Sent Command: 'harvest_pwd'");
		assertEquals(outcome.getOutput().get(10), "Received response: 'Started Harvest: C:\\Users\\test\\Desktop" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(11), "Sent Command: 'cd C:\\Users\\test\\Documents'");
		assertEquals(outcome.getOutput().get(12), "Received response: 'C:\\Users\\test\\Documents" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(13), "Sent Command: 'harvest_pwd'");
		assertEquals(outcome.getOutput().get(14), "Received response: 'Started Harvest: C:\\Users\\test\\Documents" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(15), "Sent Command: 'cd C:\\test'");
		assertEquals(outcome.getOutput().get(16), "Received response: 'C:\\test" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(17), "Macro Executor: 'Original working directory resumed, harvest underway in the background if directories found'");
		
		assertFalse(outcome.hasErrors());
	}
	
	@Test 
	void testErrorsIfNotWindows() {
		IOLogger logger = new IOLogger(Paths.get(properties.getProperty(Constants.HUBLOGGINGPATH)));
		IOManager io = new IOManager(logger, null);
		UserDirectoryHarvester macro = new UserDirectoryHarvester();
		macro.initialize(io, null);
		
		int id = io.addSession("user", "host", "protocol");

		// Flush the command buffer
		String cmd = io.pollCommand(id);
		while (cmd != null) {
			cmd = io.pollCommand(id);
		}
		
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientResponseEmulator em = new ClientResponseEmulator(io, id, false, true);
		em.makeInvalidOs();
		exec.submit(em);
		
		MacroOutcome outcome = macro.processCmd(UserDirectoryHarvester.HARVEST_USER_DIRS_CMD, id, "Not used");
		assertEquals(outcome.getOutput().get(0), "Sent Command: 'os_heritage'");
		assertEquals(outcome.getOutput().get(1), "Received response: 'Mac" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(2), "Error: Unsupported operating system: Mac");
		assertTrue(outcome.hasErrors());
	}
	
	@Test
	void testLinuxHomeDir() {
		IOLogger logger = new IOLogger(Paths.get(properties.getProperty(Constants.HUBLOGGINGPATH)));
		IOManager io = new IOManager(logger, null);
		UserDirectoryHarvester macro = new UserDirectoryHarvester();
		macro.initialize(io, null);
		
		int id = io.addSession("user", "host", "protocol");

		// Flush the command buffer
		String cmd = io.pollCommand(id);
		while (cmd != null) {
			cmd = io.pollCommand(id);
		}
		
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientResponseEmulator em = new ClientResponseEmulator(io, id, false, false);
		em.makeLinuxEmulator();
		exec.submit(em);
		
		MacroOutcome outcome = macro.processCmd(UserDirectoryHarvester.HARVEST_USER_DIRS_CMD, id, "Not used");
		
		assertEquals(outcome.getOutput().get(0), "Sent Command: 'os_heritage'");
		assertEquals(outcome.getOutput().get(1), "Received response: 'Linux" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(2), "Sent Command: 'pwd'");
		assertEquals(outcome.getOutput().get(3), "Received response: '/home/kali/working" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(4), "Macro Executor: 'Saving original working directory, proceeding with macro'");
		assertEquals(outcome.getOutput().get(5), "Sent Command: 'cd ~'");
		assertEquals(outcome.getOutput().get(6), "Received response: '/home/kali" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(7), "Sent Command: 'harvest_pwd'");
		assertEquals(outcome.getOutput().get(8), "Received response: 'Started Harvest: /home/kali" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(9), "Sent Command: 'cd /home/kali/working'");
		assertEquals(outcome.getOutput().get(10), "Received response: '/home/kali/working" + System.lineSeparator()
			+ "'");
		assertEquals(outcome.getOutput().get(11), "Macro Executor: 'Original working directory resumed, harvest underway in the background if directories found'");
		
		assertFalse(outcome.hasErrors());
	}
	
	@Test
	void testHarvestOneDrive() {
		IOLogger logger = new IOLogger(Paths.get(properties.getProperty(Constants.HUBLOGGINGPATH)));
		IOManager io = new IOManager(logger, null);
		UserDirectoryHarvester macro = new UserDirectoryHarvester();
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
		
		MacroOutcome outcome = macro.processCmd(UserDirectoryHarvester.HARVEST_USER_DIRS_CMD, id, "Not used");
		
		assertEquals(outcome.getOutput().get(0), "Sent Command: 'os_heritage'");
		assertEquals(outcome.getOutput().get(1), "Received response: 'Windows" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(2), "Sent Command: 'pwd'");
		assertEquals(outcome.getOutput().get(3), "Received response: 'C:\\test" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(4), "Macro Executor: 'Saving original working directory, proceeding with macro'");
		assertEquals(outcome.getOutput().get(5), "Macro Executor: 'Found OneDrive folder: C:\\Users\\test\\OneDrive'");
		assertEquals(outcome.getOutput().get(6), "Sent Command: 'cd C:\\Users\\test\\OneDrive\\Desktop'");
		assertEquals(outcome.getOutput().get(7), "Received response: 'C:\\Users\\test\\OneDrive\\Desktop" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(8), "Sent Command: 'harvest_pwd'");
		assertEquals(outcome.getOutput().get(9), "Received response: 'Started Harvest: C:\\Users\\test\\OneDrive\\Desktop" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(10), "Sent Command: 'cd C:\\Users\\test\\OneDrive\\Documents'");
		assertEquals(outcome.getOutput().get(11), "Received response: 'C:\\Users\\test\\OneDrive\\Documents" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(12), "Sent Command: 'harvest_pwd'");
		assertEquals(outcome.getOutput().get(13), "Received response: 'Started Harvest: C:\\Users\\test\\OneDrive\\Documents" + System.lineSeparator()
				+ "'");
		assertEquals(outcome.getOutput().get(14), "Macro Executor: 'Could not find user profile folder, proceedind.'");
		assertEquals(outcome.getOutput().get(15), "Sent Command: 'cd C:\\test'");
		assertEquals(outcome.getOutput().get(16), "Received response: 'C:\\test" + System.lineSeparator()
		+ "'");
		assertEquals(outcome.getOutput().get(17), "Macro Executor: 'Original working directory resumed, harvest underway in the background if directories found'");
		
		assertFalse(outcome.hasErrors());
	
	}

}
