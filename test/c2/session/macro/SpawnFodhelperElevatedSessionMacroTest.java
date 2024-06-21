package c2.session.macro;

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
import util.Time;
import util.test.ClientServerTest;
import util.test.IOManagerUserTest;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;

class SpawnFodhelperElevatedSessionMacroTest extends IOManagerUserTest{

	@BeforeEach
	void setUp() throws Exception {
		setUpManagerAndSession();
	}

	private class ClientStartCmdEmulator implements Runnable {

		private IOManager session;
		private int sessionId;
		private boolean alive = true;

		public ClientStartCmdEmulator(int sessionid, IOManager session) {
			this.session = session;
			this.sessionId = sessionid;
		}

		public void run() {
			while (alive) {
				String command = session.pollCommand(sessionId);
				if (command == null) {
					// continue
					Time.sleepWrapped(10);
				} else {
					if (command.equalsIgnoreCase(Commands.CLIENT_CMD_GET_EXE)) {
						session.sendIO(sessionId, "fakestart.exe");
						alive = false;
					}
				}
			}
		}

	}

	@Test
	void testDetectCommand() {
		SpawnFodhelperElevatedSessionMacro macro = new SpawnFodhelperElevatedSessionMacro();
		macro.initialize(io, null);
		assertTrue(macro.isCommandMatch(SpawnFodhelperElevatedSessionMacro.COMMAND));
		assertFalse(macro.isCommandMatch("bogus_cmd all"));
	}

	@Test
	void testNominalSendCommand() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io);
		exec.submit(em);

		SpawnFodhelperElevatedSessionMacro macro = new SpawnFodhelperElevatedSessionMacro();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(SpawnFodhelperElevatedSessionMacro.COMMAND, sessionId, null);
		assertEquals(0, outcome.getErrors().size());
		assertEquals(7, outcome.getOutput().size());
		assertEquals("Sent Command: '" + Commands.CLIENT_CMD_GET_EXE + "'", outcome.getOutput().get(0));
		assertEquals("Received response: 'fakestart.exe" + System.lineSeparator() + "'", outcome.getOutput().get(1));
		assertEquals("Sent Command: '" + SpawnFodhelperElevatedSessionMacro.NEW_ITEM_CMD + "'",
				outcome.getOutput().get(2));
		assertEquals("Sent Command: '" + SpawnFodhelperElevatedSessionMacro.NEW_ITEM_PROP_CMD + "'",
				outcome.getOutput().get(3));
		assertEquals("Sent Command: '" + SpawnFodhelperElevatedSessionMacro.SET_ITEM_PROP_CMD_A + "fakestart.exe"
				+ SpawnFodhelperElevatedSessionMacro.SET_ITEM_PROP_CMD_B + "'", outcome.getOutput().get(4));
		assertEquals("Sent Command: 'fodhelper.exe'", outcome.getOutput().get(5));
		assertEquals(
				"Macro Executor: 'Fodhelper engaged, new elevated session should be available if current user has elevated privs'",
				outcome.getOutput().get(6));
	}

}
