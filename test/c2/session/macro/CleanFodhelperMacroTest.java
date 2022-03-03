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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import c2.Constants;
import c2.session.CommandLoader;
import c2.session.IOManager;

class CleanFodhelperMacroTest {

	IOManager io;
	int sessionId;

	@BeforeEach
	void setUp() throws Exception {
		Path testPath = null;
		if (System.getProperty("os.name").contains("Windows")) {
			testPath = Paths.get("config", "test.properties");
		}else {
			testPath = Paths.get("config", "test_linux.properties");
		}
		try (InputStream input = new FileInputStream(testPath.toFile())) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			CommandLoader cl = new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>());
			io = new IOManager(Paths.get(prop.getProperty(Constants.HUBLOGGINGPATH)), cl);

			sessionId = io.addSession("noone", "testHost", "protocol");
		} catch (IOException ex) {
			System.out.println("Unable to load config file");
		}
	}
	
	@Test
	void testRecognizeTest() {
		CleanFodhelperMacro macro = new CleanFodhelperMacro();
		macro.initialize(io, null);
		assertTrue(macro.isCommandMatch(CleanFodhelperMacro.COMMAND));
		assertFalse(macro.isCommandMatch("bogus_cmd all"));
	}
	
	@Test
	void testCommandIssuedTest() {
		CleanFodhelperMacro macro = new CleanFodhelperMacro();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(CleanFodhelperMacro.COMMAND, sessionId, null);
		assertEquals(0, outcome.getErrors().size());
		assertEquals(2, outcome.getOutput().size());
		assertEquals("Sent Command: '" + CleanFodhelperMacro.CLIENT_COMMAND + "'", outcome.getOutput().get(0));
		assertEquals("Macro Executor: 'Fodhelper registry cleaned up'", outcome.getOutput().get(1));
	}

}
