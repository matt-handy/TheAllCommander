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
import util.test.IOManagerUserTest;

class RegistryDebuggerTest extends IOManagerUserTest{

	
	@BeforeEach
	void setUp() throws Exception {
		setUpManagerAndSession();
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
				}else if(command.startsWith("REG ADD") && simElevatedPrivs) {
					session.sendIO(sessionId, WindowsConstants.WINDOWS_SYSTEM_OPERATION_COMPLETE_MSG);
				}else if(command.contains("net session 2>&1")) {
					if(simElevatedPrivs) {
						session.sendIO(sessionId, "There are no entries in the list.");
					}else {
						session.sendIO(sessionId, "Access is denied.");
					}
				} else {
					if (command.equalsIgnoreCase(Commands.CLIENT_CMD_GET_EXE)) {
						session.sendIO(sessionId, "fakestart.exe");
					}
				}
			}
		}

	}
	
	@Test
	void testDetectsCommand() {
		RegistryDebugger macro = new RegistryDebugger();
		assertTrue(macro.isCommandMatch(RegistryDebugger.COMMAND + " calc.exe"));
		assertFalse(macro.isCommandMatch("barf calc.exe"));
	}
	
	@Test
	void testNominalWorkflow() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io);
		exec.submit(em);
		
		RegistryDebugger macro = new RegistryDebugger();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(RegistryDebugger.COMMAND + " calc.exe", sessionId, null);
		assertFalse(outcome.hasErrors());
		assertEquals("Sent Command: 'get_daemon_start_cmd'", outcome.getOutput().get(0));
		assertEquals("Received response: 'fakestart.exe"
				+ System.lineSeparator() + "'", outcome.getOutput().get(1));
		assertEquals("Sent Command: 'REG ADD \"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Image File Execution Options\\calc.exe\" /v Debugger /d \"fakestart.exe\"'", outcome.getOutput().get(2));
		assertEquals("Received response: 'The operation completed successfully."
				+ System.lineSeparator() + "'", outcome.getOutput().get(3));
		assertEquals("Macro Executor: 'Success!'", outcome.getOutput().get(4));
		
		em.kill();
	}
	
	@Test
	void testNotElevated() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io);
		em.deactivateElevatedPrivs();
		exec.submit(em);
		
		RegistryDebugger macro = new RegistryDebugger();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(RegistryDebugger.COMMAND + " calc.exe", sessionId, null);
		assertTrue(outcome.hasErrors());
		assertEquals("Error: Failure: Must be running from an elevated session to write to HKLM", outcome.getOutput().get(0));
	}
	
	@Test
	void testGarbageArguments() {
		RegistryDebugger macro = new RegistryDebugger();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(RegistryDebugger.COMMAND + " calc.exe BARF", sessionId, null);
		assertTrue(outcome.hasErrors());
		assertEquals("Error: " + RegistryDebugger.HELP, outcome.getOutput().get(0));
		
		outcome = macro.processCmd(RegistryDebugger.COMMAND, sessionId, null);
		assertTrue(outcome.hasErrors());
		assertEquals("Error: " + RegistryDebugger.HELP, outcome.getOutput().get(0));
	}

}
