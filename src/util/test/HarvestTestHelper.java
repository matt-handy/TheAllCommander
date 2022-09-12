package util.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

import c2.Constants;
import util.Time;
import util.test.TestConfiguration.OS;

public class HarvestTestHelper {
	
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
	
public static void testDataExfilBody(TestConfiguration config) {
		
		try {
			Socket remote = new Socket("localhost", 8111);
			OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// Ensure that python client has connected
			}
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
			
			bw.write("cd target" + File.separator + "lib" + System.lineSeparator());
			bw.flush();
			
			bw.write("harvest_pwd" + System.lineSeparator());
			bw.flush();
			
			System.out.println("Testing CD");
			String result = br.readLine();//Read CD response
			Path targetDirPath = Paths.get("target", "lib").toAbsolutePath();
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
			String lz = ClientServerTest.getDefaultSystemTestProperties().getProperty(Constants.DAEMON_EXFILTEST_DIRECTORY);
			File dir = new File(lz + File.separator + InetAddress.getLocalHost().getHostName());

			File[] matches = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return true;
				}
			});
			assertTrue(matches.length == 1);
			String expectedFileName = lz + File.separator + InetAddress.getLocalHost().getHostName() + File.separator + matches[0].getName() + File.separator;
			if(config.os == OS.WINDOWS) {
				expectedFileName = expectedFileName + targetDirPath.getRoot().toString().charAt(0) + File.separator;
			}
			expectedFileName = expectedFileName + targetDirPath.getRoot().relativize(targetDirPath).toString();
			File target = new File(
					expectedFileName);
			assertTrue(target.exists());
			assertTrue(target.isDirectory());
			for(File file : target.listFiles()) {
				byte[] dlCopy = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
				byte[] original = Files.readAllBytes(Paths.get("target", "lib", file.getName()));
				assertEquals(original.length, dlCopy.length);
				for(int idx = 0; idx < original.length; idx++) {
					assertEquals(dlCopy[idx], original[idx]);
				}
			}
			
			if(config.protocol.equals("DNS")) {
				//Why are we only testing harvest control flow with DNS? It's slow, so we know that we can transmit control instructions
				//before the transmission is completed
				
				bw.write("harvest_pwd" + System.lineSeparator());
				bw.write("cd .." + System.lineSeparator());
				bw.write("harvest_pwd" + System.lineSeparator());
				bw.write("listActiveHarvests" + System.lineSeparator());
				bw.write("kill_all_harvests" + System.lineSeparator());
				bw.flush();
				
				Path targetAbsPath = Paths.get("target").toAbsolutePath();
				
				assertEquals("Started Harvest: " + targetDirPath.toString(), br.readLine());//discard first harvest ack
				br.readLine();//Discard first cd
				assertEquals("Started Harvest: " + targetAbsPath.toString(), br.readLine());//discard second harvest ack
				System.out.println("Testing list all harvests");
				result = br.readLine();
				assertEquals(result, "0 : " + targetDirPath.toString());
				result = br.readLine();
				assertEquals(result, "1 : " + targetAbsPath.toString());
				
				//test kill all harvests
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
				
				
				assertEquals("Started Harvest: " + targetAbsPath.toString(), br.readLine());//discard first harvest ack
				System.out.println("Reading list line");
				result = br.readLine();
				assertEquals(result, "0 : " + targetAbsPath.toString());
				
				bw.write("kill_harvest 0" + System.lineSeparator());
				bw.flush();
				assertEquals("Harvester terminated: " + targetAbsPath.toString(), br.readLine());
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
			}
			
			bw.write("die" + System.lineSeparator());
			bw.flush();
			
			Time.sleepWrapped(2000);
		} catch (IOException ex) {
			ex.printStackTrace();
			fail();
		}
	}
}
