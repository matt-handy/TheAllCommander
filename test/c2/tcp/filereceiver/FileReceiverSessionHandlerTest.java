package c2.tcp.filereceiver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import util.Time;

class FileReceiverSessionHandlerTest {

	public static final int TEST_PORT = 8010;
	public static final int TEST_PORT2 = 8011;

	private byte[] intToByteArray(final int i) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		dos.writeInt(i);
		dos.flush();
		return bos.toByteArray();
	}

	private byte[] longToByteArray(final long i) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		dos.writeLong(i);
		dos.flush();
		return bos.toByteArray();
	}

	@AfterEach
	void cleanup() {
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

	@Test
	void test() {
		FileReceiverSessionReceiver sessionReceiver = new FileReceiverSessionReceiver(TEST_PORT,
				Paths.get("test", "fileReceiverTest"));
		ExecutorService service = Executors.newCachedThreadPool();
		service.submit(sessionReceiver);
		assertTrue(sessionReceiver.awaitOnline());

		try {
			Socket socket = new Socket("127.0.0.1", TEST_PORT);
			OutputStream os = socket.getOutputStream();
			String hostname = "test-host";
			byte[] hostnameLen = intToByteArray(hostname.length());
			os.write(hostnameLen);
			os.write(hostname.getBytes());
			os.flush();

			String testFilename = "C:\\Users\\testperson\\fakefile.txt";
			byte[] filenameLen = intToByteArray(testFilename.length());
			os.write(filenameLen);
			os.write(testFilename.getBytes());
			Random fileGenerator = new Random();
			byte[] fakeFile = new byte[81280];
			fileGenerator.nextBytes(fakeFile);
			byte[] fileLen = longToByteArray(fakeFile.length);
			os.write(fileLen);
			os.write(fakeFile);
			os.flush();

			String endOfT = "End of transmission";
			byte[] endOfTLen = intToByteArray(endOfT.length());
			os.write(endOfTLen);
			os.write(endOfT.getBytes());
			os.flush();
			socket.close();

			Time.sleepWrapped(1000);

			File dir = Paths.get("test", "fileReceiverTest", "test-host").toFile();

			File[] matches = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return true;
				}
			});
			assertTrue(matches.length == 1);
			
			File target = Paths.get("test", "fileReceiverTest", "test-host", matches[0].getName(), "Users", "testperson", "fakefile.txt").toFile();
			assertTrue(target.exists());
			byte[] data = Files.readAllBytes(target.toPath());
			assertEquals(fakeFile.length, data.length);
			for (int idx = 0; idx < fakeFile.length; idx++) {
				assertEquals(fakeFile[idx], data[idx]);
			}

			sessionReceiver.kill();
		} catch (IOException ex) {
			fail(ex.getMessage());
		}
	}

	@Test
	void testTwoFiles() {
		FileReceiverSessionReceiver sessionReceiver = new FileReceiverSessionReceiver(TEST_PORT2,
				Paths.get("test", "fileReceiverTest"));
		ExecutorService service = Executors.newCachedThreadPool();
		service.submit(sessionReceiver);
		assertTrue(sessionReceiver.awaitOnline());

		try {
			Socket socket = new Socket("127.0.0.1", TEST_PORT2);
			OutputStream os = socket.getOutputStream();
			String hostname = "test-host";
			byte[] hostnameLen = intToByteArray(hostname.length());
			os.write(hostnameLen);
			os.write(hostname.getBytes());
			os.flush();

			String testFilename = "C:\\Users\\testperson\\fakefile.txt";
			byte[] filenameLen = intToByteArray(testFilename.length());
			os.write(filenameLen);
			os.write(testFilename.getBytes());
			Random fileGenerator = new Random();
			byte[] fakeFile = new byte[81280];
			fileGenerator.nextBytes(fakeFile);
			byte[] fileLen = longToByteArray(fakeFile.length);
			os.write(fileLen);
			os.write(fakeFile);
			os.flush();

			String testFilename2 = "C:\\Users\\testperson\\fakefile2.txt";
			byte[] filenameLen2 = intToByteArray(testFilename2.length());
			os.write(filenameLen2);
			os.write(testFilename2.getBytes());
			byte[] fakeFile2 = new byte[60000];
			fileGenerator.nextBytes(fakeFile2);
			byte[] fileLen2 = longToByteArray(fakeFile2.length);
			os.write(fileLen2);
			os.write(fakeFile2);
			os.flush();

			socket.close();

			// Do not send end of transmission signal, make sure the listener can handle the
			// broken
			// connection and self-terminate

			Time.sleepWrapped(1000);

			File dir = Paths.get("test", "fileReceiverTest", "test-host").toFile();

			File[] matches = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return true;
				}
			});
			assertTrue(matches.length == 1);
			File target = Paths.get("test", "fileReceiverTest", "test-host", matches[0].getName(), "Users", "testperson", "fakefile.txt").toFile();
			assertTrue(target.exists());
			byte[] data = Files.readAllBytes(target.toPath());
			assertEquals(fakeFile.length, data.length);
			for (int idx = 0; idx < fakeFile.length; idx++) {
				assertEquals(fakeFile[idx], data[idx]);
			}

			File target2 = Paths.get("test", "fileReceiverTest", "test-host", matches[0].getName(), "Users", "testperson", "fakefile2.txt").toFile();
			assertTrue(target2.exists());
			byte[] data2 = Files.readAllBytes(target2.toPath());
			assertEquals(fakeFile2.length, data2.length);
			for (int idx = 0; idx < fakeFile2.length; idx++) {
				assertEquals(fakeFile2[idx], data2[idx]);
			}

			sessionReceiver.kill();
		} catch (IOException ex) {
			fail(ex.getMessage());
		}
	}

}
