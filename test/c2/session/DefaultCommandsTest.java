package c2.session;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import c2.file.CommandLoadParser;

class DefaultCommandsTest {

	private final Path defaultsFile = Paths.get("test", "default_commands");
	
	@BeforeEach
	void setUp() throws Exception {
		
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
	}

	@AfterEach
	void tearDown() throws Exception {
		Files.delete(defaultsFile);
	}

	@Test
	void test() {
		try {
			CommandLoader cl = new CommandLoadParser().buildLoader("test\\default_commands");
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
