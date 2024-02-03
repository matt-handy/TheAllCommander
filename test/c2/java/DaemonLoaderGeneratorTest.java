package c2.java;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import c2.admin.LocalConnection;
import c2.session.CommandMacroManager;
import c2.session.CommandWizard;
import c2.session.IOManager;
import c2.session.SessionInitiator;
import c2.session.SessionManager;
import c2.session.log.IOLogger;
import util.Time;
import util.test.JavaLoaderTestFramework;

class DaemonLoaderGeneratorTest {

	private JavaLoaderTestFramework framework;

	@AfterEach
	void cleanup() {
		if (framework != null) {
			try {
				framework.teardownTest();
				framework = null;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	@Test
	void testHelloWorldStringGeneration() {
		List<String> jars = new ArrayList<>();
		jars.add("HelloWorld.jar");

		try {
			DaemonLoaderGenerator.generateDaemonLoaderB64Exe("localhost:8010", jars, "c2.daemon.HelloWorld");
			// Why aren't we testing the jar here? If we got anything back at all and didn't
			// an exception, then we compiled, and that's all we're proving here.
		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Failed to compile test code");
		}
	}

	@Test
	void testServerGivesJar() {
		Path sampleJarPath = Paths.get("agents", "HelloWorld", "java");
		List<String> jars = new ArrayList<>();
		jars.add("HelloWorld.jar");

		framework = new JavaLoaderTestFramework();
		try {
			framework.init(sampleJarPath, jars, "HelloWorld");
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("Warning, system not configured to test Java staging: " + ex.getMessage());
			return;
		}
		try {
		ByteArrayOutputStream cmdBuffer = new ByteArrayOutputStream();
		BufferedOutputStream bos = new BufferedOutputStream(cmdBuffer);
		PrintWriter builder = new PrintWriter(bos);
		builder.println("WIZARD");
		builder.println(CommandWizard.CMD_GENERATE_JAVA + " localhost:8011 HelloWorld HelloWorld.jar");
		builder.println(CommandWizard.CMD_QUIT);
		builder.println("sleep");
		builder.println(LocalConnection.CMD_QUIT_LOCAL);
		builder.flush();
		builder.close();
		BufferedReader br = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(cmdBuffer.toByteArray())));

		// Prepare stream to receive output
		ByteArrayOutputStream receiveCmdBuffer = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(receiveCmdBuffer);

		// Set up server elements
		IOManager io = new IOManager(new IOLogger(Paths.get("test")), null);
		Random random = new Random();
		int port = 40000 + random.nextInt(1000);
		CommandMacroManager cmm = new CommandMacroManager(null, io, null);
		SessionManager manager = new SessionManager(io, port, cmm);
		SessionInitiator testSession = new SessionInitiator(manager, io, port, cmm);
		ExecutorService service = Executors.newFixedThreadPool(4);
		service.submit(testSession);
		
		LocalConnection lc = new LocalConnection();
		String args[] = { "127.0.0.1", port + "" };
		try {
			lc.engage(args, br, ps);
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}

		// Read back the stream through the client
		br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(receiveCmdBuffer.toByteArray())));
		// Iterate over lines and validate

		br.readLine();// Available sessions
		br.readLine();// Default session
		String line = br.readLine();
		assertEquals("Enter 'WIZARD' to begin other server commands", line);
		line = br.readLine();
		assertEquals("Available commands: ", line);
		br.readLine();// Option line
		br.readLine();// Command line
		br.readLine();// Example line
		br.readLine();// Command line
		br.readLine();// Example line
		line = br.readLine();
		assertEquals("Note: Only available with TheAllCommander on Windows", line);
		line = br.readLine();
		assertEquals(CommandWizard.CMD_QUIT, line);
		line = br.readLine();
		assertEquals("Jar downloaded", line);
		
		framework.launchJarAndReturn();
		
		while (framework.isLoaderJarRunning()) {
			Time.sleepWrapped(500);
		}

		assertEquals("stdout: Hello World" + System.lineSeparator() + System.lineSeparator(),
					framework.getLoaderJarOutput());
		
		
		}catch(Exception ex) {
			fail(ex.getMessage());
		}
		
		
		
		
	}
	
	@Test
	void testHelloWorldJar() {
		Path sampleJarPath = Paths.get("agents", "HelloWorld", "java");
		List<String> jars = new ArrayList<>();
		jars.add("HelloWorld.jar");

		framework = new JavaLoaderTestFramework();
		try {
			framework.init(sampleJarPath, jars, "HelloWorld");
			framework.generateAndPlaceLoaderJar();
			framework.launchJarAndReturn();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("Warning, system not configured to test Java staging: " + ex.getMessage());
			return;
		}

		while (framework.isLoaderJarRunning()) {
			Time.sleepWrapped(500);
		}

		try {
			assertEquals("stdout: Hello World" + System.lineSeparator() + System.lineSeparator(),
					framework.getLoaderJarOutput());
		} catch (Exception ex) {

		}

	}

}
