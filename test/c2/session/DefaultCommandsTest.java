package c2.session;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import c2.file.CommandLoadParser;

class DefaultCommandsTest {

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
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
			
		}catch(Exception ex) {
			ex.printStackTrace();
			fail();
		}
	}

}
