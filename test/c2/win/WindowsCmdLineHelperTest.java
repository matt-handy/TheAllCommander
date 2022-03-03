package c2.win;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class WindowsCmdLineHelperTest {

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
	void testIsClientElevated() {
		io.sendIO(sessionId, "Access is denied.");
		assertFalse(WindowsCmdLineHelper.isClientElevated(sessionId, io));
		io.sendIO(sessionId, "There are no entries in the list.");
		assertTrue(WindowsCmdLineHelper.isClientElevated(sessionId, io));

		// Flush commands
		String out = io.pollCommand(sessionId);
		assertEquals(out, "net session 2>&1");
		out = io.pollCommand(sessionId);
		assertEquals(out, "net session 2>&1");
	}

}
