package c2.session.filereceiver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import c2.Constants;
import c2.admin.LocalConnection;
import c2.smtp.EmailHandler;
import c2.smtp.SimpleEmail;
import util.Time;
import util.test.ClientServerTest;
import util.test.OutputStreamWriterHelper;
import util.test.RunnerTestGeneric;
import util.test.TestCommons;
import util.test.TestConstants;

class FileReceiverDatagramHandlerTest extends ClientServerTest {

	static final String TEST_BASE_FOLDER = "test_subfolder";
	static final String TEST_HOST_NAME = "test_hostname";
	

	static final Path SINGLE_XMISSION_FILE_FOLDER = Paths.get("test", TestCommons.HARVEST_TEST_DIR, "single_file");
	static final Path DOUBLE_XMISSION_FILE_FOLDER = Paths.get("test", TestCommons.HARVEST_TEST_DIR, "double_file");
	static final Path TRIPLE_XMISSION_FILE_FOLDER = Paths.get("test", TestCommons.HARVEST_TEST_DIR, "triple_file");
	static final Path SINGLE_FILE_PATH = Paths.get(SINGLE_XMISSION_FILE_FOLDER.toString(), "s_file");
	static final Path DOUBLE_FILE_PATH = Paths.get(DOUBLE_XMISSION_FILE_FOLDER.toString(), "d_file");
	static final Path TRIPLE_FILE_PATH = Paths.get(TRIPLE_XMISSION_FILE_FOLDER.toString(), "t_file");

	public static void flushC2Emails() {
		EmailHandler receiveHandler = new EmailHandler();
		try {
			receiveHandler.initialize(null, ClientServerTest.getDefaultSystemTestProperties(), null, null);
		} catch (Exception ex) {
			fail(ex.getMessage());
		}

		SimpleEmail email = receiveHandler.getNextMessage();
		while (email != null) {
			email = receiveHandler.getNextMessage();
		}
	}

	@AfterEach
	void cleanupTestArtifacts() {
		TestCommons.cleanFileHarvesterDir();
	}

	@BeforeEach
	void checkAndFlushEmail() {
		if (System.getProperty("os.name").contains("Windows")) {
			Properties prop = ClientServerTest.getDefaultSystemTestProperties();
			if (prop.getProperty(Constants.COMMSERVICES).contains("EmailHandler")) {
				flushC2Emails();
			}
		}
		TestCommons.cleanFileHarvesterDir();
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
					Paths.get(TestCommons.HARVEST_LANDING_DIR.toString(), InetAddress.getLocalHost().getHostName()).toString())
							.listFiles(File::isDirectory);
			// There should only be one session in the directory
			assertEquals(1, directories.length);
			Path files[] = { SINGLE_FILE_PATH, DOUBLE_FILE_PATH, TRIPLE_FILE_PATH };
			for (Path filePath : files) {
				String cleanedPath = filePath.toAbsolutePath().toString().replaceAll(":", "");
				Path target = Paths.get(directories[0].toPath().toString(), cleanedPath);
				assertTrue(Files.exists(target));
				assertFalse(Files.isDirectory(target));
				
				FileInputStream isReference = new FileInputStream(filePath.toFile());
				FileInputStream isContent = new FileInputStream(target.toFile());
			    byte[] chunk = new byte[1024];
			    byte[] chunkTarget = new byte[1024];
			    int chunkLen = 0;
			    while ((chunkLen = isReference.read(chunk)) != -1) {
			    	assertEquals(chunkLen, isContent.read(chunkTarget), "Files not the same size!");
			    	for(int idx = 0; idx < chunkLen; idx++) {
			    		assertEquals(chunk[idx], chunkTarget[idx]);
			    	}
			    }
			    isReference.close();
			    isContent.close();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}

	void verifyFileExistsAndCorrect(String name, long expectedLen) {
		File[] directories = new File(Paths.get(TestCommons.HARVEST_LANDING_DIR.toString(), TEST_HOST_NAME).toString())
				.listFiles(File::isDirectory);
		// There should only be one session in the directory
		assertEquals(1, directories.length);
		Path firstChildFolder = Paths.get(directories[0].toPath().toString(), TEST_BASE_FOLDER);
		assertTrue(Files.exists(firstChildFolder));
		assertTrue(Files.isDirectory(firstChildFolder));
		Path targetFile = Paths.get(firstChildFolder.toString(), name);
		assertTrue(Files.exists(targetFile));
		try {
			/*
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
			*/
			FileInputStream is = new FileInputStream(targetFile.toFile());
		    byte[] chunk = new byte[1024];
		    int chunkLen = 0;
		    byte currentChar = 65;
		    long masterCount = 0;
		    while ((chunkLen = is.read(chunk)) != -1) {
		    	masterCount += chunkLen;
		    	for(int idx = 0; idx < chunkLen; idx++) {
		    		assertEquals(currentChar, chunk[idx]);
		    		currentChar++;
					if (currentChar > 90) {
						currentChar = 65;
					}
		    	}
		    }
		    assertEquals(expectedLen, masterCount);
		    is.close();
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
		FileReceiverDatagramHandler handler = new FileReceiverDatagramHandler(TestCommons.HARVEST_LANDING_DIR);
		handler.registerNewSession(2, 1, TEST_HOST_NAME);
		assertTrue(handler.hasSessionCurrently(2, 1));
		handler.processIncoming(2, 1, buildSessionCloseout());
		assertFalse(handler.hasSessionCurrently(2, 1));
	}

	@Test
	void testCanRegisterNewSession() {
		FileReceiverDatagramHandler handler = new FileReceiverDatagramHandler(TestCommons.HARVEST_LANDING_DIR);
		handler.registerNewSession(2, 1, TEST_HOST_NAME);
		assertTrue(handler.hasSessionCurrently(2, 1));
		assertFalse(handler.hasSessionCurrently(2, 2));
	}

	@Test
	void testSingleFileTransmission() {
		byte[] testPayload = buildTestPayload(1024, "test1", true);
		FileReceiverDatagramHandler handler = new FileReceiverDatagramHandler(TestCommons.HARVEST_LANDING_DIR);
		handler.registerNewSession(2, 1, TEST_HOST_NAME);
		handler.processIncoming(2, 1, testPayload);
		verifyFileExistsAndCorrect("test1", 1024);
	}

	@Test
	void testFileOverTwoTransmissions() {
		byte[] testPayload = buildTestPayload(1024, "test1", true);
		FileReceiverDatagramHandler handler = new FileReceiverDatagramHandler(TestCommons.HARVEST_LANDING_DIR);
		handler.registerNewSession(2, 1, TEST_HOST_NAME);
		byte[] xMission = Arrays.copyOfRange(testPayload, 0, 512);
		handler.processIncoming(2, 1, xMission);
		xMission = Arrays.copyOfRange(testPayload, 512, testPayload.length);
		handler.processIncoming(2, 1, xMission);
		verifyFileExistsAndCorrect("test1", 1024);
	}

	@Test
	void testFileOverThreeTransmissions() {
		byte[] testPayload = buildTestPayload(2048, "test1", true);
		FileReceiverDatagramHandler handler = new FileReceiverDatagramHandler(TestCommons.HARVEST_LANDING_DIR);
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
		byte[] testPayload = buildTestPayload(1024, "test1", true);
		byte[] testPayload2 = buildTestPayload(2055, "test2", true);
		FileReceiverDatagramHandler handler = new FileReceiverDatagramHandler(TestCommons.HARVEST_LANDING_DIR);
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
		// Cross platform test is meaningless to run on Linux
		if (!System.getProperty("os.name").contains("Windows")) {
			return;
		}
		byte[] testPayload = buildTestPayload(1024, "/" + TEST_BASE_FOLDER.toString() + "/test1", false);
		FileReceiverDatagramHandler handler = new FileReceiverDatagramHandler(TestCommons.HARVEST_LANDING_DIR);
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

	void generateTestFolderAndFiles(boolean supersizeLast) {
		try {
			Files.createDirectories(SINGLE_XMISSION_FILE_FOLDER);
			Files.createDirectories(DOUBLE_XMISSION_FILE_FOLDER);
			Files.createDirectories(TRIPLE_XMISSION_FILE_FOLDER);

			Files.write(SINGLE_FILE_PATH, buildTestPattern(50000));
			Files.write(DOUBLE_FILE_PATH, buildTestPattern(150000));
			if(supersizeLast) {
				Files.write(TRIPLE_FILE_PATH, buildTestPattern(26));
				for(int idx = 0; idx < 2000; idx++) {
					Files.write(TRIPLE_FILE_PATH, buildTestPattern(2600000), StandardOpenOption.APPEND);
				}
			}else {
				Files.write(TRIPLE_FILE_PATH, buildTestPattern(250000));
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}

	@Test
	void testFileTransmissionViaPythonHTTPS() {
		testFileTransmissionViaPython(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE, false, false);
	}

	@Test
	void testFileTransmissionViaPythonDNS() {
		// This test is currently bypassed on Linux. There is a padding issued with the
		// Linux based
		// DNS decryption which does not manifest on other testing. This will be
		// revisited when the
		// DNS integration tests are made part of regular Linux baseline
		if (!System.getProperty("os.name").contains("Windows")) {
			return;
		}
		testFileTransmissionViaPython(TestConstants.PYTHON_DNSDAEMON_TEST_EXE, false, false);
	}

	@Test
	void testFileTransmissionViaPythonSMTP() {

		// Only works on Windows && only test if email connection defined in config
		if (System.getProperty("os.name").contains("Windows")) {
			Properties prop = ClientServerTest.getDefaultSystemTestProperties();

			if (prop.getProperty(Constants.COMMSERVICES).contains("EmailHandler")) {
				flushC2Emails();
				// TODO: Re-enable this test. It works when done manually, but for some reason
				// automatic execution fails
				// testFileTransmissionViaPython(TestConstants.PYTHON_SMTPDAEMON_TEST_EXE,
				// true);
			}

		}
	}

	void testFileTransmissionViaPython(String daemon, boolean smtpFlush, boolean supersize) {
		// Tests file per 1 xmission
		// Tests file with 2 xmissions
		// Tests file with 3 xmissions

		generateTestFolderAndFiles(supersize);
		if (System.getProperty("os.name").contains("Windows")) {
			initiateServer();
		} else {
			initiateServer("test_linux.properties");
		}
		spawnClient(daemon);
		System.out.println("Transmitting commands");

		Time.sleepWrapped(5000);

		try {
			System.out.println("Connecting test commander...");
			Socket remote = LocalConnection.getSocket("127.0.0.1", Integer.parseInt(ClientServerTest.getDefaultSystemTestProperties().getProperty(Constants.SECURECOMMANDERPORT)), getDefaultSystemTestProperties());
			System.out.println("Locking test commander streams...");
			OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

			RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, false, false);

			OutputStreamWriterHelper.writeAndSend(bw,
					"cd test" + FileSystems.getDefault().getSeparator() + TestCommons.HARVEST_TEST_DIR);
			OutputStreamWriterHelper.writeAndSend(bw, "harvest_pwd");
			Path harvestDir = Paths.get("test", TestCommons.HARVEST_TEST_DIR);
			if (smtpFlush) {
				assertEquals("Daemon alive", br.readLine());
			}
			assertEquals(harvestDir.toAbsolutePath().toString(), br.readLine());
			assertEquals(br.readLine(), "Started Harvest: " + harvestDir.toAbsolutePath().toString());
			assertEquals(br.readLine(), "Harvest complete: " + harvestDir.toAbsolutePath().toString());

			OutputStreamWriterHelper.writeAndSend(bw, "die");

			awaitClient();

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

	@Test
	//We need to validate that this works to support Outlook auditing as .ost and .pst files can be multi-gig
	void testFileOverIntLimitViaPythonHTTPS() {
		if(TestConstants.LARGE_HARVEST_TEST_ENABLE) {
			testFileTransmissionViaPython(TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE, false, true);
		}else {
			System.out.println("Skipping large file test, can be enabled with largeharvest.test.enable=true");
		}
	}
	 
}
