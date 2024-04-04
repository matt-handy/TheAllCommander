package c2.python.tlm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import util.Time;
import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConstants;

class BasicTlmTest extends ClientServerTest {

	@BeforeEach
	void setup() {
		initiateServer();
	}

	@AfterEach
	void clean() {
		awaitClient();
		teardown();
		if (Files.exists(getTestFilename())) {
			try {
				Files.delete(getTestFilename());
				Files.delete(getTestFilename().getParent());
				Files.delete(getTestFilename().getParent().getParent());
			} catch (IOException ex) {
				System.out.println("Warning: unable to clean up after test: " + this.toString());
			}
		}
	}

	private Path getTestFilename() {
		try {
			return Paths.get("test", "tlm", InetAddress.getLocalHost().getHostName(), "MEASURE_ME");
		} catch (UnknownHostException e) {
			// This can't happen
			return null;
		}
	}

	@Test
	void test() {
		spawnClient(TestConstants.PYTHON_TLMSIM_TEST_EXE);

		// Give time for the simulator to connect and upload at least one record
		Time.sleepWrapped(3000);

		assertTrue(Files.exists(getTestFilename()));

		try {
			List<String> lines = Files.readAllLines(getTestFilename());
			assertTrue(lines.size() >= 1);
			String firstLine = lines.get(0);
			System.out.println(firstLine);
			String elements[] = firstLine.split(" ");
			assertEquals("'42'", elements[2]);
			assertEquals("DOUBLE", elements[3]);
			try {
				String time = elements[0] + "T" + elements[1];
				System.out.println("Parse this: " + time);
				ZonedDateTime.parse(time);
			} catch (Exception ex) {
				fail("Could not parse expected zoned datetime: " + elements[0]);
			}

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
				RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, false, false);
			} catch (Exception ex) {
				fail(ex.getMessage());
			}

			bw.write("die" + System.lineSeparator());
			bw.flush();
		} catch (Exception ex) {
			fail(ex.getMessage());
		}
		Time.sleepWrapped(2500);
	}

}
