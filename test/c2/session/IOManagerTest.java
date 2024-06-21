package c2.session;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import c2.session.log.IOLogger;
import util.test.TestCommons;

class IOManagerTest {

	final String REMOTE_FORWARD_NAME = "localhost:8000";
	
	@BeforeEach
	void setUp() throws Exception {
		TestCommons.pretestCleanup();
	}

	@AfterEach
	void tearDown() throws Exception {
		TestCommons.pretestCleanup();
	}

	@Test
	void testIOFlow() {
		try {
			IOManager ioManager = new IOManager(new IOLogger(Paths.get("test", "log")), new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>()));
			
			int id = ioManager.determineAndGetCorrectSessionId("fake", "fake", "fake", false, null);

			assertEquals(id, 2);
			assertEquals(ioManager.getSessionId("fake:fake:fake"), 2);
			
			ioManager.sendCommand(id, "dir");

			assertEquals(ioManager.pollCommand(id), "dir");
			
			assertEquals(ioManager.pollCommand(id), null);
			
			ioManager.sendIO(id, "bunch of files");
			
			assertEquals(ioManager.pollIO(id), "bunch of files");
			
			assertEquals(ioManager.pollIO(id), null);
			
			id = ioManager.determineAndGetCorrectSessionId("afake", "afake", "afake", false, null);
			assertEquals(id, 3);
			assertEquals(ioManager.getSessionId("afake:afake:afake"), 3);
			
			Path logFilePath = Paths.get("test", "log", "fake", "fake2");
			assertTrue(Files.exists(logFilePath));
			BufferedReader br = new BufferedReader(new FileReader(logFilePath.toFile()));
			assertTrue(br.readLine().contains("Send command: 'dir'"));
			assertTrue(br.readLine().contains("Received IO: 'bunch of files'"));
			assertTrue(br.readLine()== null);
			br.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getLocalizedMessage());
		}
	}

	@Test
	void testAwaitMultilineCommands() {
		IOManager ioManager = new IOManager(new IOLogger(Paths.get("test", "log")), new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>()));
		int id = ioManager.determineAndGetCorrectSessionId("fake", "otherfake", "moarfake", false, null);
		assertEquals(2, id);
		String val = ioManager.awaitMultilineCommands(2);
		assertEquals("", val);
		
		val = ioManager.awaitMultilineCommands(2, 20000);
		assertEquals("", val);
	}
	
	@Test
	void nonExistantSession() {
		IOManager ioManager = new IOManager(new IOLogger(Paths.get("test", "log")), new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>()));
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ioManager.pollCommand(2);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ioManager.pollIO(2);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ioManager.sendCommand(3, "dir");
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ioManager.sendIO(3, "Stuff");
		});

		int id = ioManager.determineAndGetCorrectSessionId("fake", "otherfake", "moarfake", false, null);
		assertEquals(id, 2);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ioManager.pollCommand(3);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ioManager.pollIO(3);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ioManager.sendCommand(3, "dir");
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ioManager.sendIO(3, "Stuff");
		});
	}
	
	@Test
	void testPortForwardQueues() {
		IOManager ioManager = new IOManager(new IOLogger(Paths.get("test", "log")), new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>()));
		int id = ioManager.determineAndGetCorrectSessionId("fake", "fake", "fake", false, null);
		ioManager.forwardTCPTraffic(id, REMOTE_FORWARD_NAME, "This is a test string");
		String forwardedData = ioManager.grabForwardedTCPTraffic(id, REMOTE_FORWARD_NAME);
		assertEquals("This is a test string", forwardedData);
		
		ioManager.queueForwardedTCPTraffic(id, REMOTE_FORWARD_NAME, "This is a return test string");
		String returnedData = ioManager.receiveForwardedTCPTraffic(id, REMOTE_FORWARD_NAME);
		assertEquals("This is a return test string", returnedData);
	}

}
