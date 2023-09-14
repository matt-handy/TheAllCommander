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

import c2.Constants;
import c2.session.CommandLoader;
import c2.session.IOManager;
import c2.session.log.IOLogger;
import util.Time;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;

class WindowsLogDeleterMacroTest {

	private class ClientElevatedResponseEmulator implements Runnable {

		private IOManager session;
		private int sessionId;
		private boolean alive = true;
		private boolean respondElevated;

		public ClientElevatedResponseEmulator(boolean respondElevated, int sessionid, IOManager session) {
			this.respondElevated = respondElevated;
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
					if (command.equalsIgnoreCase("net session 2>&1")) {
						if (respondElevated) {
							session.sendIO(sessionId, "There are no entries in the list.");
						} else {
							session.sendIO(sessionId, "Access is denied.");
						}
						alive = false;
					}
				}
			}
		}

	}

	IOManager io;
	int sessionId;

	@BeforeEach
	void setUp() throws Exception {
		Path testPath = null;
		if (TestConfiguration.getThisSystemOS() == OS.WINDOWS) {
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
	void testMacroCmdStringMatch() {
		WindowsLogDeleterMacro logDeleter = new WindowsLogDeleterMacro();
		logDeleter.initialize(io, null);
		assertTrue(logDeleter.isCommandMatch("delete_windows_logs all"));
		assertFalse(logDeleter.isCommandMatch("bogus_cmd all"));
	}
	
	@Test
	void testFailsIfNotElevated() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientElevatedResponseEmulator em = new ClientElevatedResponseEmulator(false, sessionId, io);
		exec.submit(em);
		
		WindowsLogDeleterMacro logDeleter = new WindowsLogDeleterMacro();
		logDeleter.initialize(io, null);
		MacroOutcome outcome = logDeleter.processCmd("delete_windows_logs", sessionId, null);
		assertTrue(outcome.hasErrors());
		assertEquals(1, outcome.getErrors().size());
		assertEquals("Daemon session is not elevated, cannot delete windows logs", outcome.getErrors().get(0));
	}

	@Test
	void testDeleteAll() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientElevatedResponseEmulator em = new ClientElevatedResponseEmulator(true, sessionId, io);
		exec.submit(em);
		
		WindowsLogDeleterMacro logDeleter = new WindowsLogDeleterMacro();
		logDeleter.initialize(io, null);
		MacroOutcome outcome = logDeleter.processCmd("delete_windows_logs all", sessionId, null);
		assertFalse(outcome.hasErrors());
		
		assertEquals(5, outcome.getOutput().size());
		assertEquals("Sent Command: 'wevtutil clear-log Application'", outcome.getOutput().get(0));
		assertEquals("Sent Command: 'wevtutil clear-log Security'", outcome.getOutput().get(1));
		assertEquals("Sent Command: 'wevtutil clear-log System'", outcome.getOutput().get(2));
		assertEquals("Sent Command: 'wevtutil clear-log Setup'", outcome.getOutput().get(3));
		
		assertEquals("Macro Executor: 'System Log Deletion Complete'", outcome.getOutput().get(4));
	}

	@Test
	void testDeletesLogsIndividually() {
		String logTypes[] = {"Application", "Security", "System", "Setup"};
		for(String logType : logTypes) {
			ExecutorService exec = Executors.newFixedThreadPool(2);
			ClientElevatedResponseEmulator em = new ClientElevatedResponseEmulator(true, sessionId, io);
			exec.submit(em);
		
			WindowsLogDeleterMacro logDeleter = new WindowsLogDeleterMacro();
			logDeleter.initialize(io, null);
			MacroOutcome outcome = logDeleter.processCmd("delete_windows_logs " + logType, sessionId, null);
			assertFalse(outcome.hasErrors());
		
			assertEquals(2, outcome.getOutput().size());
			assertEquals("Sent Command: 'wevtutil clear-log " + logType + "'", outcome.getOutput().get(0));
			assertEquals("Macro Executor: 'System " + logType + " Log Deletion Complete'", outcome.getOutput().get(1));
		}
	}
	
	
	

	@Test
	void testTooManyArgs() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientElevatedResponseEmulator em = new ClientElevatedResponseEmulator(true, sessionId, io);
		exec.submit(em);
		
		WindowsLogDeleterMacro logDeleter = new WindowsLogDeleterMacro();
		logDeleter.initialize(io, null);
		String cmd = "delete_windows_logs application fake_arg";
		MacroOutcome outcome = logDeleter.processCmd(cmd, sessionId, null);
		assertTrue(outcome.hasErrors());
		assertEquals(1, outcome.getErrors().size());
		assertEquals("Too many arguments: " + cmd, outcome.getErrors().get(0));
	}

	@Test
	void testInvalidLogType() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientElevatedResponseEmulator em = new ClientElevatedResponseEmulator(true, sessionId, io);
		exec.submit(em);
		
		WindowsLogDeleterMacro logDeleter = new WindowsLogDeleterMacro();
		logDeleter.initialize(io, null);
		String cmd = "delete_windows_logs bogus_log";
		MacroOutcome outcome = logDeleter.processCmd(cmd, sessionId, null);
		assertTrue(outcome.hasErrors());
		assertEquals(1, outcome.getErrors().size());
		assertEquals("Unknown log type: " + cmd, outcome.getErrors().get(0));
	}

}
