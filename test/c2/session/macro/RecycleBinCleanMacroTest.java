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
import c2.win.WindowsCmdLineHelperTest;
import util.test.ClientServerTest;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;

class RecycleBinCleanMacroTest {

	IOManager io;
	int sessionId;

	@BeforeEach
	void setUp() throws Exception {
		io = ClientServerTest.setupDefaultIOManager();
		sessionId = io.addSession("noone", "testHost", "protocol");
	}

	@Test
	void testRecognizesExpectedMacro() {
		RecycleBinCleanMacro macro = new RecycleBinCleanMacro();
		assertTrue(macro.isCommandMatch(RecycleBinCleanMacro.COMMAND));
		assertFalse(macro.isCommandMatch("narf"));
	}

	@Test
	void testSendsProperCommand() {
		RecycleBinCleanMacro macro = new RecycleBinCleanMacro();
		macro.initialize(io, null);
		io.sendIO(sessionId, WindowsCmdLineHelperTest.SYSTEMDRIVE_EXAMPLE);
		MacroOutcome outcome = macro.processCmd(SpawnFodhelperElevatedSessionMacro.COMMAND, sessionId, null);
		assertEquals(0, outcome.getErrors().size());
		assertEquals(3, outcome.getOutput().size());
		assertEquals("Sent Command: 'del /s /q C:\\Users\\matte\\$Recycle.Bin'", outcome.getOutput().get(0));
		assertEquals("Received response: ''", outcome.getOutput().get(1));
		assertEquals("Macro Executor: 'Recycle bin emptied'", outcome.getOutput().get(2));
	}

}
