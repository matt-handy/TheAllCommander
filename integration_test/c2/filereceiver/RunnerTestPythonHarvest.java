package c2.filereceiver;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import c2.RunnerTestGeneric;
import util.Time;
import util.test.ClientServerTest;
import util.test.TestConfiguration;
import util.test.TestConstants;

public class RunnerTestPythonHarvest  extends ClientServerTest {

	@Test
	void test() {
		testPythonDataExfil();
	}

	@AfterEach
	@BeforeEach
	void clean() {
		cleanup();
	}
	
	public static void testPythonDataExfil() {
		initiateServer();
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "httpsAgent.py\"";
		spawnClient(clientCmd);

		TestConfiguration testConfig = new TestConfiguration(TestConfiguration.OS.WINDOWS, "python", "HTTPS");
		testDataExfilBody(testConfig);
		
		teardown();
	}

	public static void testDataExfilBody(TestConfiguration config) {
		try {
			System.out.println("Connecting test commander...");
			Socket remote = new Socket("localhost", 8111);
			System.out.println("Locking test commander streams...");
			OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// Ensure that python client has connected
			}
			System.out.println("Setting up test commander session...");
			try {
				RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, config.os == TestConfiguration.OS.LINUX,
						config.isRemote());
			} catch (Exception ex) {
				fail(ex.getMessage());
			}

			if (config.protocol.equals("SMTP")) {
				String output = br.readLine();
				assertEquals(output, "Daemon alive");
			}
			
			bw.write("cd tmp" + System.lineSeparator());
			bw.flush();
			
			bw.write("harvest_pwd" + System.lineSeparator());
			bw.flush();
			
			System.out.println("Testing CD");
			String result = br.readLine();//Read CD response
			Path targetDirPath = Paths.get("tmp").toAbsolutePath();
			assertEquals(targetDirPath.toString(), result);
			//Start message can arrive after the complete message, if the harvest is super fast
			System.out.println("Testing Start Harvest");
			result = br.readLine();
			assertTrue(result.equals("Started Harvest: " + targetDirPath.toString()) ||
					result.equals("Harvest complete: " + targetDirPath.toString()));
			System.out.println("Testing Stop Harvest");
			result = br.readLine();
			assertTrue(result.equals("Started Harvest: " + targetDirPath.toString()) ||
					result.equals("Harvest complete: " + targetDirPath.toString()));
		
			Time.sleepWrapped(2000);
			// Test Harvest contents
			File dir = new File("test\\fileReceiverTest\\" + InetAddress.getLocalHost().getHostName());

			File[] matches = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return true;
				}
			});
			assertTrue(matches.length == 1);
			File target = new File(
					"test\\fileReceiverTest\\" + InetAddress.getLocalHost().getHostName() + "\\" + matches[0].getName() + "\\" + targetDirPath.getRoot().relativize(targetDirPath).toString());
			assertTrue(target.exists());
			assertTrue(target.isDirectory());
			for(File file : target.listFiles()) {
				byte[] dlCopy = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
				byte[] original = Files.readAllBytes(Paths.get("tmp", file.getName()));
				assertEquals(original.length, dlCopy.length);
				for(int idx = 0; idx < original.length; idx++) {
					assertEquals(dlCopy[idx], original[idx]);
				}
			}
			
			//Test list all harvests 
			bw.write("cd ../.." + System.lineSeparator());
			bw.write("harvest_pwd" + System.lineSeparator());
			bw.write("cd .." + System.lineSeparator());
			bw.write("harvest_pwd" + System.lineSeparator());
			bw.write("listActiveHarvests" + System.lineSeparator());
			bw.flush();
			
			String path1 = Paths.get("").toAbsolutePath().getParent().toAbsolutePath().toString();
			String path2 = Paths.get("").toAbsolutePath().getParent().getParent().toAbsolutePath().toString();
			assertEquals(path1, br.readLine());//discard first cd
			assertEquals("Started Harvest: " + path1, br.readLine());//discard first harvest ack
			assertEquals(path2, br.readLine());//discard second cd
			assertEquals("Started Harvest: " + path2, br.readLine());//discard second harvest ack
			System.out.println("Testing list all harvests");
			result = br.readLine();
			assertTrue(result.equals("0 : " + path1) || result.equals("0 : " +path2));
			result = br.readLine();
			assertTrue(result.equals("1 : " + path1) || result.equals("1 : " +path2));
			//assertEquals("0 : " + Paths.get("").toAbsolutePath().getParent().toAbsolutePath().toString(), br.readLine());
			//assertEquals("1 : " + Paths.get("").toAbsolutePath().getParent().getParent().toAbsolutePath().toString(), br.readLine());
			
			//test kill all harvests
			bw.write("kill_all_harvests" + System.lineSeparator());
			bw.flush();
			System.out.println("Testing all harvest kill message");
			assertEquals("All harvests terminated", br.readLine());
			System.out.println("Testing all harvest kill confirmation");
			bw.write("listActiveHarvests" + System.lineSeparator());
			bw.flush();
			assertEquals("No active harvests", br.readLine());
			
			
			// Test kill individual harvest
			System.out.println("Initiate new harvest, get status");
			bw.write("harvest_pwd" + System.lineSeparator());
			
			bw.write("listActiveHarvests" + System.lineSeparator());
			bw.flush();
			
			
			assertEquals("Started Harvest: " + Paths.get("").toAbsolutePath().getParent().getParent().toAbsolutePath().toString(), br.readLine());
			System.out.println("Reading list line");
			assertEquals("0 : " + Paths.get("").toAbsolutePath().getParent().getParent().toAbsolutePath().toString(), br.readLine());
			
			bw.write("kill_harvest 0" + System.lineSeparator());
			bw.flush();
			assertEquals("Harvester terminated: " + Paths.get("").toAbsolutePath().getParent().getParent().toAbsolutePath().toString(), br.readLine());
			bw.write("listActiveHarvests" + System.lineSeparator());
			bw.flush();
			System.out.println("Reading kill conf");
			assertEquals("No active harvests", br.readLine());
			
			// test kill harvest w/ too many args
			bw.write("kill_harvest 0 barf" + System.lineSeparator());
			bw.flush();
			assertEquals("Invalid kill_harvest command", br.readLine());
			// test kill harvest w/ too many args
			bw.write("kill_harvest barf" + System.lineSeparator());
			bw.flush();
			assertEquals("Invalid kill_harvest command", br.readLine());
			
			bw.write("die" + System.lineSeparator());
			bw.flush();
			
			Time.sleepWrapped(2000);
		} catch (IOException ex) {
			ex.printStackTrace();
			fail();
		}
	}

	public static void cleanup() {
		try {
			if (Files.exists(Paths.get("test", "fileReceiverTest"))) {
				Files.walk(Paths.get("test", "fileReceiverTest")).sorted(Comparator.reverseOrder()).map(Path::toFile)
						.forEach(File::delete);
			}
		} catch (IOException e2) {
			e2.printStackTrace();
			fail("Cannot clean up harvester");
		}
	}
}
