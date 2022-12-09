package c2.session.macro.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import c2.Commands;
import c2.Constants;
import c2.session.CommandLoader;
import c2.session.IOManager;
import c2.session.log.IOLogger;
import c2.session.macro.MacroOutcome;
import util.Time;
import util.test.ClientServerTest;

class WindowsStartupKeyTest {

	IOManager io;
	int sessionId;

	@BeforeEach
	void setUp() throws Exception {
		Properties prop = ClientServerTest.getDefaultSystemTestProperties();
					CommandLoader cl = new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>());
		io = new IOManager(new IOLogger(Paths.get(prop.getProperty(Constants.HUBLOGGINGPATH))), cl);

		sessionId = io.addSession("noone", "testHost", "protocol");
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
				}else if(command.startsWith("powershell.exe") && command.contains("calc.exe")) {
					session.sendIO(sessionId, "\r\n"
							+ "\r\n"
							+ "ASLKJASFAS   : calc.exe\r\n"
							+ "PSPath       : Microsoft.PowerShell.Core\\Registry::HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\r\n"
							+ "PSParentPath : Microsoft.PowerShell.Core\\Registry::HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\r\n"
							+ "PSChildName  : Run\r\n"
							+ "PSDrive      : HKCU\r\n"
							+ "PSProvider   : Microsoft.PowerShell.Core\\Registry\r\n"
							+ "\r\n"
							+ "\r\n"
							+ "\r\n"
							+ "");
				}else if(command.startsWith("powershell.exe") && !command.contains("calc.exe")) {
					session.sendIO(sessionId, "\r\n"
							+ "\r\n"
							+ "ASLKJASFAS   : fakestart.exe\r\n"
							+ "PSPath       : Microsoft.PowerShell.Core\\Registry::HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\r\n"
							+ "PSParentPath : Microsoft.PowerShell.Core\\Registry::HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\r\n"
							+ "PSChildName  : Run\r\n"
							+ "PSDrive      : HKCU\r\n"
							+ "PSProvider   : Microsoft.PowerShell.Core\\Registry\r\n"
							+ "\r\n"
							+ "\r\n"
							+ "\r\n"
							+ "");
				} else {
					if (command.equalsIgnoreCase(Commands.CLIENT_GET_EXE_CMD)) {
						session.sendIO(sessionId, "fakestart.exe");
					}
				}
			}
		}

	}
	
	@Test
	void testCommandRecognized() {
		WindowStartupKey macro = new WindowStartupKey();
		assertTrue(macro.isCommandMatch(WindowStartupKey.TEST_WIN_RUNKEY_PERSISTENCE + " " + WindowStartupKey.TEST_WIN_RUNKEY_PERSISTENCE_OPTION_CU));
		assertFalse(macro.isCommandMatch(WindowStartupKey.TEST_WIN_RUNKEY_PERSISTENCE_OPTION_CU + " " + WindowStartupKey.TEST_WIN_RUNKEY_PERSISTENCE));
	}
	
	@Test
	void testFailsBadThirdArgument() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io);
		exec.submit(em);
		
		WindowStartupKey macro = new WindowStartupKey();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(WindowStartupKey.TEST_WIN_RUNKEY_PERSISTENCE + " " + WindowStartupKey.TEST_WIN_RUNKEY_PERSISTENCE_OPTION_CU + " narf", sessionId, null);
		assertTrue(outcome.hasErrors());
		assertEquals("Error: Cannot execute command: third argument if specified must be 'calc'", outcome.getOutput().get(0));
		
		em.kill();
	}
	
	@Test 
	void testFailsBadSecondArgument() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io);
		exec.submit(em);
		
		WindowStartupKey macro = new WindowStartupKey();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(WindowStartupKey.TEST_WIN_RUNKEY_PERSISTENCE + " barfington", sessionId, null);
		assertTrue(outcome.hasErrors());
		assertEquals("Sent Command: 'get_daemon_start_cmd'", outcome.getOutput().get(0));
		assertEquals("Received response: 'fakestart.exe" + System.lineSeparator()
				+ "'", outcome.getOutput().get(1));
		assertEquals("Error: Cannot execute command, unknown second argument: barfington", outcome.getOutput().get(2));
		
		em.kill();
	}
	
	@Test
	void testSuccessfullyWritesCalc() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io);
		exec.submit(em);
		
		WindowStartupKey macro = new WindowStartupKey();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(WindowStartupKey.TEST_WIN_RUNKEY_PERSISTENCE + " " + WindowStartupKey.TEST_WIN_RUNKEY_PERSISTENCE_OPTION_CU + " " + WindowStartupKey.TEST_WIN_RUNKEY_PERSISTENCE_OPTION_CALC, sessionId, null);
		assertFalse(outcome.hasErrors());
		assertEquals(2, outcome.getOutput().size());
		String psCommand = outcome.getOutput().get(0);
		assertTrue(psCommand.startsWith("Sent Command: 'powershell.exe -c \"New-ItemProperty -Path 'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Run' -Name '"));
		assertTrue(psCommand.endsWith("' -Value 'calc.exe' -Force'"));
		assertFalse(psCommand.contains("$EXE_NAME$"));
		assertTrue(outcome.getOutput().get(1).contains("ASLKJASFAS   : calc.exe"));
		
		em.kill();
	}
	
	@Test
	void testSuccessfullyWritesCustomExe() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io);
		exec.submit(em);
		
		WindowStartupKey macro = new WindowStartupKey();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(WindowStartupKey.TEST_WIN_RUNKEY_PERSISTENCE + " " + WindowStartupKey.TEST_WIN_RUNKEY_PERSISTENCE_OPTION_CU, sessionId, null);
		assertFalse(outcome.hasErrors());
		assertEquals(4, outcome.getOutput().size());
		assertEquals("Sent Command: 'get_daemon_start_cmd'", outcome.getOutput().get(0));
		assertEquals("Received response: 'fakestart.exe" + System.lineSeparator()
				+ "'", outcome.getOutput().get(1));
		String psCommand = outcome.getOutput().get(2);
		assertTrue(psCommand.startsWith("Sent Command: 'powershell.exe -c \"New-ItemProperty -Path 'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Run' -Name '"));
		assertTrue(psCommand.endsWith("' -Value 'fakestart.exe' -Force'"));
		assertFalse(psCommand.contains("$EXE_NAME$"));
		assertTrue(outcome.getOutput().get(3).contains("ASLKJASFAS   : fakestart.exe"));
		
		em.kill();
	}

}
