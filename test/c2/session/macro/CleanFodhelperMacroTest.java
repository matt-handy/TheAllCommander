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
import c2.session.log.IOLogger;
import util.test.ClientServerTest;
import util.test.IOManagerUserTest;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;

class CleanFodhelperMacroTest extends IOManagerUserTest{

	@BeforeEach
	void setUp() throws Exception {
		setUpManagerAndSession();
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
