package c2.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import c2.Constants;
import c2.file.CommandLoadParser;
import c2.session.log.IOLogger;
import c2.session.macro.CleanFodhelperMacro;
import util.test.ClientServerTest;

class DefaultCommandsTest {

	private final Path defaultsFile = Paths.get("test", "default_commands");
	
	@AfterEach
	void tearDown() throws Exception {
		Files.delete(defaultsFile);
	}

	@Test
	void testMacroCommandInterpretation() {
		try (BufferedWriter writer = Files.newBufferedWriter(defaultsFile, Charset.forName("UTF-8"))) {
			writer.write(":all" + System.lineSeparator());
			writer.write(CleanFodhelperMacro.COMMAND + System.lineSeparator());
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
		try {
			CommandLoader cl = new CommandLoadParser().buildLoader(defaultsFile.toString());
			assertEquals(1, cl.getDefaultCommands().size());
			assertEquals(CleanFodhelperMacro.COMMAND, cl.getDefaultCommands().get(0));

			assertEquals(null, cl.getUserCommands(System.getProperty("user.name")));
			String hostname = InetAddress.getLocalHost().getHostName().toUpperCase();
			assertEquals(null, cl.getHostCommands(hostname));
			
			Properties prop = ClientServerTest.getDefaultSystemTestProperties();
			IOManager io = new IOManager(new IOLogger(Paths.get(prop.getProperty(Constants.HUBLOGGINGPATH))), cl);
			CommandMacroManager cmm = new CommandMacroManager(null, io, "nostring");
			cmm.initializeMacros(prop);
			io.setCommandMacroManager(cmm);
			int id = io.determineAndGetCorrectSessionId(System.getProperty("user.name"), hostname, "HTTPS", false, null);
			assertEquals(2, id);
			assertEquals(CleanFodhelperMacro.CLIENT_COMMAND, io.pollCommand(id));
			assertEquals(null, io.pollCommand(id));
			
			assertEquals("Sent Command: '" + CleanFodhelperMacro.CLIENT_COMMAND + "'" + System.lineSeparator(), io.pollIO(id));
			assertEquals("Macro Executor: 'Fodhelper registry cleaned up'" + System.lineSeparator(), io.pollIO(id));

		} catch (Exception ex) {
			ex.printStackTrace();
			fail();
		}
	}
	
	@Test
	void testVanillaCommands() {
		try (BufferedWriter writer = Files.newBufferedWriter(defaultsFile, Charset.forName("UTF-8"))) {
			writer.write(":all" + System.lineSeparator());
			writer.write("pwd" + System.lineSeparator());
			writer.write(":user-" + System.getProperty("user.name") + System.lineSeparator());
			writer.write("cd .." + System.lineSeparator());
			writer.write("pwd" + System.lineSeparator());
			writer.write(":host-" + InetAddress.getLocalHost().getHostName().toUpperCase() + System.lineSeparator());
			writer.write("pwd" + System.lineSeparator());
			writer.write("cd ." + System.lineSeparator());
			writer.write("pwd");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
		try {
			CommandLoader cl = new CommandLoadParser().buildLoader(defaultsFile.toString());
			assertTrue(cl.getDefaultCommands().size() == 1);
			assertTrue(cl.getDefaultCommands().get(0).equals("pwd"));

			assertTrue(cl.getUserCommands(System.getProperty("user.name")).size() == 2);
			assertTrue(cl.getUserCommands(System.getProperty("user.name")).get(0).equals("cd .."));
			assertTrue(cl.getUserCommands(System.getProperty("user.name")).get(1).equals("pwd"));
			assertTrue(cl.getUserCommands("someguy") == null);

			String hostname = InetAddress.getLocalHost().getHostName().toUpperCase();
			assertTrue(cl.getHostCommands(hostname).size() == 3);
			assertTrue(cl.getHostCommands(hostname).get(0).equals("pwd"));
			assertTrue(cl.getHostCommands(hostname).get(1).equals("cd ."));
			assertTrue(cl.getHostCommands(hostname).get(2).equals("pwd"));
			assertTrue(cl.getHostCommands("otherhost") == null);

		} catch (Exception ex) {
			ex.printStackTrace();
			fail();
		}
	}

}
