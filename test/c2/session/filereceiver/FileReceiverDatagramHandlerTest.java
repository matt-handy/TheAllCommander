package c2.session.filereceiver;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Properties;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import c2.Constants;
import c2.smtp.EmailHandlerTester;
import util.Time;
import util.test.ClientServerTest;
import util.test.OutputStreamWriterHelper;
import util.test.RunnerTestGeneric;
import util.test.TestConstants;

class FileReceiverDatagramHandlerTest extends ClientServerTest {

	static final Path CONTENT_DIR = Paths.get("test", "fileReceiverTest");
	static final String TEST_BASE_FOLDER = "test_subfolder";
	static final String TEST_HOST_NAME = "test_hostname";
	static final String HARVEST_TEST_DIR = "harvest_test_source";

	static final Path SINGLE_XMISSION_FILE_FOLDER = Paths.get("test", HARVEST_TEST_DIR, "single_file");
	static final Path DOUBLE_XMISSION_FILE_FOLDER = Paths.get("test", HARVEST_TEST_DIR, "double_file");
	static final Path TRIPLE_XMISSION_FILE_FOLDER = Paths.get("test", HARVEST_TEST_DIR, "triple_file");
	static final Path SINGLE_FILE_PATH = Paths.get(SINGLE_XMISSION_FILE_FOLDER.toString(), "s_file");
	static final Path DOUBLE_FILE_PATH = Paths.get(DOUBLE_XMISSION_FILE_FOLDER.toString(), "d_file");
	static final Path TRIPLE_FILE_PATH = Paths.get(TRIPLE_XMISSION_FILE_FOLDER.toString(), "t_file");

	@AfterEach
	void cleanupTestArtifacts() {
		try (Stream<Path> walk = Files.walk(CONTENT_DIR)) {
			walk.sorted(Comparator.reverseOrder()).forEach(FileReceiverDatagramHandlerTest::deleteDirectory);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}

		try (Stream<Path> walk = Files.walk(Paths.get("test", HARVEST_TEST_DIR))) {
			walk.sorted(Comparator.reverseOrder()).forEach(FileReceiverDatagramHandlerTest::deleteDirectory);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
	}
	
	@BeforeEach
	void checkAndFlushEmail() {
		if (System.getProperty("os.name").contains("Windows")) {
			Path testPath = Paths.get("config", "test.properties");
			try (InputStream input = new FileInputStream(testPath.toFile())) {

				Properties prop = new Properties();

				// load a properties file
				prop.load(input);

				if(prop.getProperty(Constants.DAEMON_EMAIL_PORT) != null) {
					EmailHandlerTester.flushC2Emails();
				}
			} catch (IOException ex) {
				System.out.println("Unable to load config file");
			}
			
		}
	}

	static void deleteDirectory(Path path) {
		try {
			Files.delete(path);
		} catch (IOException e) {
			System.err.printf("Unable to delete this path : %s%n%s", path, e);
		}
	}

	private static byte[] intToByteArray(final int i) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(bos);
			dos.writeInt(i);
			dos.flush();
			return bos.toByteArray();
		} catch (IOException ex) {
			// This can't actually happen
			return null;
		}
	}

	private static byte[] longToByteArray(final long i) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(bos);
			dos.writeLong(i);
			dos.flush();
			return bos.toByteArray();
		} catch (IOException ex) {
			// This can't actually happen
			return null;
		}
	}

	void verifyDaemonTransferredFilesExistAndCorrect() {
		try {
			File[] directories = new File(
					Paths.get(CONTENT_DIR.toString(), InetAddress.getLocalHost().getHostName()).toString())
							.listFiles(File::isDirectory);
			// There should only be one session in the directory
			assertEquals(1, directories.length);
			Path files[] = {SINGLE_FILE_PATH, DOUBLE_FILE_PATH, TRIPLE_FILE_PATH};
			for(Path filePath : files) {
				String cleanedPath = filePath.toAbsolutePath().toString().replaceAll(":", "");
				Path target = Paths.get(directories[0].toPath().toString(), cleanedPath);
				assertTrue(Files.exists(target));
				assertFalse(Files.isDirectory(target));
				byte[] content = Files.readAllBytes(target);
				byte[] referenceContent = Files.readAllBytes(filePath);
				assertEquals(referenceContent.length, content.length);
				for(int idx = 0; idx < referenceContent.length; idx++) {
					assertEquals(referenceContent[idx], content[idx]);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}

	void verifyFileExistsAndCorrect(String name, int expectedLen) {
		File[] directories = new File(Paths.get(CONTENT_DIR.toString(), TEST_HOST_NAME).toString())
				.listFiles(File::isDirectory);
		// There should only be one session in the directory
		assertEquals(1, directories.length);
		Path firstChildFolder = Paths.get(directories[0].toPath().toString(), TEST_BASE_FOLDER);
		assertTrue(Files.exists(firstChildFolder));
		assertTrue(Files.isDirectory(firstChildFolder));
		Path targetFile = Paths.get(firstChildFolder.toString(), name);
		assertTrue(Files.exists(targetFile));
		try {
			byte[] content = Files.readAllBytes(targetFile);
			assertEquals(expectedLen, content.length);
			byte currentChar = 65;
			for (int idx = 0; idx < expectedLen; idx++) {
				assertEquals(currentChar, content[idx]);
				currentChar++;
				if (currentChar > 90) {
					currentChar = 65;
				}
			}
		} catch (IOException e) {
			fail("Can't read test file");
		}
	}

	private static byte[] buildSessionCloseout() {
		String closeoutMessage = "End of transmission";
		byte[] payload = new byte[4 + closeoutMessage.length()];
		byte[] filenameLenBytes = intToByteArray(closeoutMessage.length());
		for (int idx = 0; idx < 4; idx++) {
			payload[idx] = filenameLenBytes[idx];
		}
		for (int idx = 0; idx < closeoutMessage.length(); idx++) {
			payload[idx + 4] = closeoutMessage.getBytes()[idx];
		}
		return payload;
	}

	private static byte[] buildTestPayload(int length, String filename, boolean createNativePath) {
		String fullFilename = null;
		if (createNativePath) {
			fullFilename = Paths.get(TEST_BASE_FOLDER, filename).toString();
		} else {
			fullFilename = filename;
		}
		byte[] payload = new byte[4 + fullFilename.length() + 8 + length];

		byte[] filenameLenBytes = intToByteArray(fullFilename.length());
		int mIdx = 0;
		for (int idx = 0; idx < 4; idx++) {
			payload[idx] = filenameLenBytes[idx];
		}

		byte[] filenameBytes = fullFilename.getBytes();
		mIdx = 4;
		for (int idx = 0; idx < filenameBytes.length; idx++) {
			payload[mIdx + idx] = filenameBytes[idx];
		}

		mIdx += filenameBytes.length;

		byte[] payloadLenBytes = longToByteArray(length);
		for (int idx = 0; idx < payloadLenBytes.length; idx++) {
			payload[mIdx + idx] = payloadLenBytes[idx];
		}
		mIdx += payloadLenBytes.length;

		byte nextFillChar = 65;
		for (int idx = 0; idx < length; idx++) {
			payload[mIdx + idx] = nextFillChar;
			nextFillChar++;
			if (nextFillChar > 90) {
				nextFillChar = 65;
			}
		}

		return payload;
	}

	@Test
	void testEndOfTransmissionClosesSessionObject() {
		//Until Linux support is added, bypass this test
				if (!System.getProperty("os.name").contains("Windows")) {
					return;
				}
		FileReceiverDatagramHandler handler = new FileReceiverDatagramHandler(CONTENT_DIR);
		handler.registerNewSession(2, 1, TEST_HOST_NAME);
		assertTrue(handler.hasSessionCurrently(2, 1));
		handler.processIncoming(2, 1, buildSessionCloseout());
		assertFalse(handler.hasSessionCurrently(2, 1));
	}

	@Test
	void testCanRegisterNewSession() {
		FileReceiverDatagramHandler handler = new FileReceiverDatagramHandler(CONTENT_DIR);
		handler.registerNewSession(2, 1, TEST_HOST_NAME);
		assertTrue(handler.hasSessionCurrently(2, 1));
		assertFalse(handler.hasSessionCurrently(2, 2));
	}

	@Test
	void testSingleFileTransmission() {
		//Until Linux support is added, bypass this test
				if (!System.getProperty("os.name").contains("Windows")) {
					return;
				}
		byte[] testPayload = buildTestPayload(1024, "test1", true);
		FileReceiverDatagramHandler handler = new FileReceiverDatagramHandler(CONTENT_DIR);
		handler.registerNewSession(2, 1, TEST_HOST_NAME);
		handler.processIncoming(2, 1, testPayload);
		verifyFileExistsAndCorrect("test1", 1024);
	}

	@Test
	void testFileOverTwoTransmissions() {
		//Until Linux support is added, bypass this test
				if (!System.getProperty("os.name").contains("Windows")) {
					return;
				}
		byte[] testPayload = buildTestPayload(1024, "test1", true);
		FileReceiverDatagramHandler handler = new FileReceiverDatagramHandler(CONTENT_DIR);
		handler.registerNewSession(2, 1, TEST_HOST_NAME);
		byte[] xMission = Arrays.copyOfRange(testPayload, 0, 512);
		handler.processIncoming(2, 1, xMission);
		xMission = Arrays.copyOfRange(testPayload, 512, testPayload.length);
		handler.processIncoming(2, 1, xMission);
		verifyFileExistsAndCorrect("test1", 1024);
	}

	@Test
	void testFileOverThreeTransmissions() {
		//Until Linux support is added, bypass this test
				if (!System.getProperty("os.name").contains("Windows")) {
					return;
				}
		byte[] testPayload = buildTestPayload(2048, "test1", true);
		FileReceiverDatagramHandler handler = new FileReceiverDatagramHandler(CONTENT_DIR);
		handler.registerNewSession(2, 1, TEST_HOST_NAME);
		byte[] xMission = Arrays.copyOfRange(testPayload, 0, 512);
		handler.processIncoming(2, 1, xMission);
		xMission = Arrays.copyOfRange(testPayload, 512, 1024);
		handler.processIncoming(2, 1, xMission);
		xMission = Arrays.copyOfRange(testPayload, 1024, testPayload.length);
		handler.processIncoming(2, 1, xMission);
		verifyFileExistsAndCorrect("test1", 2048);
	}

	@Test
	void testTwoFileTwoTransmission() {
		//Until Linux support is added, bypass this test
				if (!System.getProperty("os.name").contains("Windows")) {
					return;
				}
		byte[] testPayload = buildTestPayload(1024, "test1", true);
		byte[] testPayload2 = buildTestPayload(2055, "test2", true);
		FileReceiverDatagramHandler handler = new FileReceiverDatagramHandler(CONTENT_DIR);
		handler.registerNewSession(2, 1, TEST_HOST_NAME);
		handler.processIncoming(2, 1, testPayload);
		handler.processIncoming(2, 1, testPayload2);
		verifyFileExistsAndCorrect("test1", 1024);
		verifyFileExistsAndCorrect("test2", 2055);
	}

	// TODO: Expand the test suite to validate transmissions over 2^31 bytes. Not
	// strictly necessary for
	// nominal test campaigns.
	/*
	 * @Test void testFileOverIntLimit() { fail("Not yet implemented"); }
	 */
	
	@Test
	void testLinuxFilenameInterprettedCorrectlyOnWindows() {
		//Until Linux support is added, bypass this test
		if (!System.getProperty("os.name").contains("Windows")) {
			return;
		}
		byte[] testPayload = buildTestPayload(1024, "/" + TEST_BASE_FOLDER.toString() + "/test1", false);
		FileReceiverDatagramHandler handler = new FileReceiverDatagramHandler(CONTENT_DIR);
		handler.registerNewSession(2, 1, TEST_HOST_NAME);
		handler.processIncoming(2, 1, testPayload);
		verifyFileExistsAndCorrect("test1", 1024);
	}
	
	
	byte[] buildTestPattern(int len) {
		byte[] testPattern = new byte[len];
		byte nextFillChar = 65;
		for (int idx = 0; idx < len; idx++) {
			testPattern[idx] = nextFillChar;
			nextFillChar++;
			if (nextFillChar > 90) {
				nextFillChar = 65;
			}
		}
		return testPattern;
	}

	void generateTestFolderAndFiles() {
		try {
			Files.createDirectories(SINGLE_XMISSION_FILE_FOLDER);
			Files.createDirectories(DOUBLE_XMISSION_FILE_FOLDER);
			Files.createDirectories(TRIPLE_XMISSION_FILE_FOLDER);

			Files.write(SINGLE_FILE_PATH, buildTestPattern(50000));
			Files.write(DOUBLE_FILE_PATH, buildTestPattern(150000));
			Files.write(TRIPLE_FILE_PATH, buildTestPattern(250000));
		} catch (IOException ex) {
			fail(ex.getMessage());
		}
	}

	@Test
	void testFileTransmissionViaPythonHTTPS() {
		//Until Linux support is added, bypass this test
				if (!System.getProperty("os.name").contains("Windows")) {
					return;
				}
				testFileTransmissionViaPython(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE, false);
	}

	@Test
	void testFileTransmissionViaPythonDNS() {
		//Until Linux support is added, bypass this test
		if (!System.getProperty("os.name").contains("Windows")) {
			return;
		}
		testFileTransmissionViaPython(TestConstants.PYTHON_DNSDAEMON_TEST_EXE, false);
	}
	
	@Test
	void testFileTransmissionViaPythonSMTP() {
		
		//Only works on Windows && only test if email connection defined in config
		if (System.getProperty("os.name").contains("Windows")) {
			Path testPath = Paths.get("config", "test.properties");
			try (InputStream input = new FileInputStream(testPath.toFile())) {

				Properties prop = new Properties();

				// load a properties file
				prop.load(input);

				if(prop.getProperty(Constants.DAEMON_EMAIL_PORT) != null) {
					EmailHandlerTester.flushC2Emails();
					//TODO: Re-enable this test. It works when done manually, but for some reason automatic execution fails
					//testFileTransmissionViaPython(TestConstants.PYTHON_SMTPDAEMON_TEST_EXE, true);
				}
			} catch (IOException ex) {
				System.out.println("Unable to load config file");
			}
			
		}
	}
	
	void testFileTransmissionViaPython(String daemon, boolean smtpFlush) {
		// Tests file per 1 xmission
		// Tests file with 2 xmissions
		// Tests file with 3 xmissions

		generateTestFolderAndFiles();
		initiateServer();
		spawnClient(daemon);
		System.out.println("Transmitting commands");

		Time.sleepWrapped(5000);

		try {
			System.out.println("Connecting test commander...");
			Socket remote = new Socket("localhost", 8111);
			System.out.println("Locking test commander streams...");
			OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

			RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, false, false);

			OutputStreamWriterHelper.writeAndSend(bw, "cd test\\" + HARVEST_TEST_DIR);
			OutputStreamWriterHelper.writeAndSend(bw, "harvest_pwd");
			Path harvestDir = Paths.get("test", HARVEST_TEST_DIR);
			if(smtpFlush) {
				assertEquals("Daemon alive", br.readLine());
			}
			assertEquals(harvestDir.toAbsolutePath().toString(), br.readLine());
			assertEquals(br.readLine(), "Started Harvest: " + harvestDir.toAbsolutePath().toString());
			assertEquals(br.readLine(), "Harvest complete: " + harvestDir.toAbsolutePath().toString());
			
			OutputStreamWriterHelper.writeAndSend(bw, "die");

			Time.sleepWrapped(3000);

			bw.close();
			br.close();
			remote.close();

			verifyDaemonTransferredFilesExistAndCorrect();
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}

		teardown();
	}
	
	/*
	 * TODO This validation is not necessary to minimum success definition, expand
	 * later
	 * 
	 * @Test void testFileOverIntLimitViaPythonHTTPS() {
	 * fail("Not yet implemented"); }
	 */
}
