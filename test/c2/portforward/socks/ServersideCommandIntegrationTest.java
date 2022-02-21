package c2.portforward.socks;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import c2.session.CommandLoader;
import c2.session.CommandPreprocessorOutcome;
import c2.session.IOManager;
import c2.session.ServersideCommandPreprocessor;
import util.test.socks5.SocksClientEmulator;
import util.test.socks5.TargetDaemonEmulator;

class ServersideCommandIntegrationTest {

	@Test
	void testInvalidCommandOptions() {
		IOManager io = new IOManager(Paths.get("test", "log"),
				new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>()));
		io.addSession("fake", "fake", "fake");
		ServersideCommandPreprocessor preprocessor = new ServersideCommandPreprocessor(io);
		
		//Invalid start command - bad port
		CommandPreprocessorOutcome outcome = preprocessor.processCommand("startSocks5 barf", 2);
		assertFalse(outcome.outcome);
		assertEquals(ServersideCommandPreprocessor.SOCKS5_FORMAT_ERROR, outcome.message);
		
		//Invalid start command - incorrect args
		outcome = preprocessor.processCommand("startSocks5", 2);
		assertFalse(outcome.outcome);
		assertEquals(ServersideCommandPreprocessor.SOCKS5_FORMAT_ERROR, outcome.message);
		
		//Invalid stop command - no existing socks
		outcome = preprocessor.processCommand("killSocks5", 2);
		assertFalse(outcome.outcome);
		assertEquals(ServersideCommandPreprocessor.NO_EXISTING_SOCKS5_ERROR, outcome.message);
	}
	
	@Test
	void testProxyStartAndStopCommandsNominal() {
		IOManager io = new IOManager(Paths.get("test", "log"),
				new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>()));
		io.addSession("fake", "fake", "fake");
		ExecutorService service = Executors.newCachedThreadPool();

		TargetDaemonEmulator targetDaemon = new TargetDaemonEmulator(io, 2, 1, false, false);
		service.submit(targetDaemon);
		
		ServersideCommandPreprocessor preprocessor = new ServersideCommandPreprocessor(io);
		preprocessor.processCommand("startSocks5 9000", 2);
		
		SocksClientEmulator clientEmulator = new SocksClientEmulator(9000, false, null, false);
		service.submit(clientEmulator);
		assertTrue(clientEmulator.isComplete());
		
		preprocessor.processCommand("killSocks5", 2);
		
		boolean caughtException = false;
		try {
			//This should fail
			new Socket("127.0.0.1", 9000);
		}catch(IOException ex) {
			caughtException = true;
		}
		
		assertTrue(caughtException);
	}

}
