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

class WindowsHiddenUserTest {

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
		private boolean simFailure = false;

		public ClientStartCmdEmulator(int sessionid, IOManager session) {
			this.session = session;
			this.sessionId = sessionid;
		}

		public void kill() {
			alive = false;
		}
		
		public void modelFailure() {
			simFailure = true;
		}
		
		public void run() {
			while (alive) {
				String command = session.pollCommand(sessionId);
				if (command == null) {
					// continue
					Time.sleepWrapped(10);
				} else {
					if (command.startsWith(WindowsHiddenUserMacro.COMMAND)) {
						if(simFailure) {
							session.sendIO(sessionId, "Unable to add user: BARF");
						}else {
							session.sendIO(sessionId, "SUCCESS");
						}
					}
				}
			}
		}

	}
	
	@Test
	void testDetectsCommand() {
		WindowsHiddenUserMacro macro = new WindowsHiddenUserMacro();
		assertTrue(macro.isCommandMatch(WindowsHiddenUserMacro.COMMAND));
		assertTrue(macro.isCommandMatch(WindowsHiddenUserMacro.COMMAND + " test"));
		assertFalse(macro.isCommandMatch("barf calc.exe"));
	}

	@Test
	void testBarfsOnInvalidInput() {
		WindowsHiddenUserMacro macro = new WindowsHiddenUserMacro();
		MacroOutcome outcome = macro.processCmd(WindowsHiddenUserMacro.COMMAND + " user password garbage", 0, null);
		assertEquals(1, outcome.getErrors().size());
		assertEquals(1, outcome.getOutput().size());
		assertEquals(WindowsHiddenUserMacro.INVALID_INPUT_MSG, outcome.getErrors().iterator().next());
	}
	
	@Test
	void testGeneratesDefaultCreds() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io);
		exec.submit(em);
		
		WindowsHiddenUserMacro macro = new WindowsHiddenUserMacro();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(WindowsHiddenUserMacro.COMMAND, sessionId, null);
		
		assertEquals(0, outcome.getErrors().size());
		assertEquals(3, outcome.getOutput().size());
		assertTrue(outcome.getOutput().get(0).startsWith("Sent Command: 'add_hidden_user "));
		assertTrue(outcome.getOutput().get(1).equals("Received response: 'SUCCESS" + System.lineSeparator() + "'"));
		assertTrue(outcome.getOutput().get(2).startsWith("Macro Executor: 'Successfully added user: '"));
	}
	
	@Test
	void testReportsClientError() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io);
		em.modelFailure();
		exec.submit(em);
		
		WindowsHiddenUserMacro macro = new WindowsHiddenUserMacro();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(WindowsHiddenUserMacro.COMMAND, sessionId, null);
		
		assertEquals(1, outcome.getErrors().size());
		assertEquals(3, outcome.getOutput().size());
		assertTrue(outcome.getOutput().get(0).startsWith("Sent Command: 'add_hidden_user "));
		assertTrue(outcome.getOutput().get(1).equals("Received response: 'Unable to add user: BARF" + System.lineSeparator() + "'"));
		assertTrue(outcome.getOutput().get(2).startsWith("Error: Unable to add user: Unable to add user: BARF"));
		
	}
}
