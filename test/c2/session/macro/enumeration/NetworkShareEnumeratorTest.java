package c2.session.macro.enumeration;

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
import c2.session.macro.persistence.RegistryDebugger;
import util.Time;
import util.test.ClientServerTest;

class NetworkShareEnumeratorTest {

	
	IOManager io;
	int sessionId;

	@BeforeEach
	void setUp() throws Exception {
		Properties prop = ClientServerTest.getDefaultSystemTestProperties();
					CommandLoader cl = new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>());
		io = new IOManager(new IOLogger(Paths.get(prop.getProperty(Constants.HUBLOGGINGPATH))), cl);

		sessionId = io.addSession("noone", "testHost", "protocol");
	}
	
	@Test
	void testDetectsCommand() {
		WindowsNetworkShareEnumerator macro = new WindowsNetworkShareEnumerator();
		assertTrue(macro.isCommandMatch(WindowsNetworkShareEnumerator.COMMAND));
		assertFalse(macro.isCommandMatch("barf calc.exe"));
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
				}else if(command.equalsIgnoreCase(WindowsNetworkShareEnumerator.WMIC_COMMAND)) {
					session.sendIO(sessionId, "Sample AV Response");
				} else {
					System.out.println("Unknown command");
				}
			}
		}

	}
	
	@Test
	void sendsCorrectCommands() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientStartCmdEmulator em = new ClientStartCmdEmulator(sessionId, io);
		exec.submit(em);
		
		WindowsNetworkShareEnumerator macro = new WindowsNetworkShareEnumerator();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(WindowsNetworkShareEnumerator.COMMAND, sessionId, null);
		assertEquals(2, outcome.getOutput().size());
		assertEquals("Sent Command: 'wmic share get caption,name,path'", outcome.getOutput().get(0));
		assertEquals("Received response: 'Sample AV Response" + System.lineSeparator() + "'", outcome.getOutput().get(1));
		
	}

}
