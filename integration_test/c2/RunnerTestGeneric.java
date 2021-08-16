package c2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import c2.session.SessionHandler;
import c2.session.SessionInitiator;
import util.Time;
import util.test.TestConfiguration;
import util.test.TestConstants;

public class RunnerTestGeneric {

	public static void testScreenshotsOnFS(String lang) {
		File dir = new File("test");
		File[] matches = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				try {
					String hostname = InetAddress.getLocalHost().getHostName().toUpperCase();
					return name.toUpperCase().equals(hostname + "-SCREEN");
				} catch (UnknownHostException e) {
					return false;
				}
				// test
			}
		});
		assertEquals(matches.length, 1);
		File[] userMatches = matches[0].listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.equals(System.getProperty("user.name"));
			}
		});
		assertEquals(userMatches.length, 1);
		File[] screenshots = userMatches[0].listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return true;
			}
		});
		assertTrue(screenshots.length >= 1);

		try {
			String hostname = InetAddress.getLocalHost().getHostName();
			if (lang.equals("C++")) {
				hostname = hostname.toUpperCase();
			}
			Path path = Paths.get("test", hostname + "-screen");
			if (Files.exists(path)) {
				Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			}
		} catch (IOException e2) {
			e2.printStackTrace();
			fail("Cannot clean up harvester");
		}
	}

	public static void connectionSetupGenericTwoClient(Socket remote, OutputStreamWriter bw, BufferedReader br,
			boolean testSecond) throws Exception {
		String output = br.readLine();
		assertEquals(output, SessionInitiator.AVAILABLE_SESSION_BANNER);
		output = br.readLine();
		assertTrue(output.contains("2:" + InetAddress.getLocalHost().getHostName())
				|| output.contains("2:" + InetAddress.getLocalHost().getHostName().toUpperCase()) || // C++ Daemon
				output.contains("3:" + InetAddress.getLocalHost().getHostName())
				|| output.contains("3:" + InetAddress.getLocalHost().getHostName().toUpperCase()) || // C++ Daemon
				output.equals("1:default:default:default"));
		System.out.println(output);
		System.out.println("Reading second session id...");
		output = br.readLine();
		System.out.println(output);
		assertTrue(output.contains("2:" + InetAddress.getLocalHost().getHostName())
				|| output.contains("2:" + InetAddress.getLocalHost().getHostName().toUpperCase()) || // C++ Daemon
				output.contains("3:" + InetAddress.getLocalHost().getHostName())
				|| output.contains("3:" + InetAddress.getLocalHost().getHostName().toUpperCase()) || // C++ Daemon
				output.equals("1:default:default:default"));

		System.out.println("Reading third session id...");
		output = br.readLine();
		System.out.println(output);
		assertTrue(output.contains("2:" + InetAddress.getLocalHost().getHostName())
				|| output.contains("2:" + InetAddress.getLocalHost().getHostName().toUpperCase()) || // C++ Daemon
				output.contains("3:" + InetAddress.getLocalHost().getHostName())
				|| output.contains("3:" + InetAddress.getLocalHost().getHostName().toUpperCase()) || // C++ Daemon
				output.equals("1:default:default:default"));

		if (testSecond) {
			bw.write("3" + System.lineSeparator());
		} else {
			bw.write("2" + System.lineSeparator());
		}
		bw.flush();

		output = br.readLine();
		assertTrue(output.startsWith(SessionHandler.NEW_SESSION_BANNER));

	}

	public static void validateTwoSessionBanner(Socket remote, OutputStreamWriter bw, BufferedReader br,
			boolean isLinux, int baseIndex, boolean isRemote) throws IOException {
		String output = br.readLine();
		assertEquals(output, SessionInitiator.AVAILABLE_SESSION_BANNER);
		output = br.readLine();
		System.out.println(output);
		if (isLinux) {
			assertTrue(
					output.contains(baseIndex + ":" + TestConstants.HOSTNAME_LINUX + ":" + TestConstants.USERNAME_LINUX)
							|| output.equals("1:default:default:default"));
		} else {
			if (isRemote) {
				assertTrue(output.contains(baseIndex + ":") || output.equals("1:default:default:default"));
			} else {
				assertTrue(output.contains(baseIndex + ":" + InetAddress.getLocalHost().getHostName())
						|| output.contains(baseIndex + ":" + InetAddress.getLocalHost().getHostName().toUpperCase()) || // C++
																														// Daemon
						output.equals("1:default:default:default"));
			}
		}
		System.out.println("Reading second session id...");
		output = br.readLine();
		System.out.println(output);
		if (isLinux) {
			assertTrue(
					output.contains(baseIndex + ":" + TestConstants.HOSTNAME_LINUX + ":" + TestConstants.USERNAME_LINUX)
							|| output.equals("1:default:default:default"));
		} else {
			if (isRemote) {
				assertTrue(output.contains(baseIndex + ":") || output.equals("1:default:default:default"));
			} else {
				assertTrue(output.contains(baseIndex + ":" + InetAddress.getLocalHost().getHostName())
						|| output.contains(baseIndex + ":" + InetAddress.getLocalHost().getHostName().toUpperCase()) || // C++
																														// Daemon
						output.equals("1:default:default:default"));
			}
		}
	}

	public static void connectionSetupGeneric(Socket remote, OutputStreamWriter bw, BufferedReader br, boolean isLinux,
			boolean isRemote) throws Exception {
		connectionSetupGeneric(remote, bw, br, isLinux, 2, isRemote);
	}

	public static void connectionSetupGeneric(Socket remote, OutputStreamWriter bw, BufferedReader br, boolean isLinux,
			int baseIndex, boolean isRemote) throws IOException {
		validateTwoSessionBanner(remote, bw, br, isLinux, baseIndex, isRemote);

		bw.write(baseIndex + System.lineSeparator());
		bw.flush();

		String output = br.readLine();
		assertTrue(output.startsWith(SessionHandler.NEW_SESSION_BANNER));

	}

	static void testLinuxLs(BufferedReader br, OutputStreamWriter bw) throws IOException {
		bw.write("ls" + System.lineSeparator());
		bw.flush();
		String output = br.readLine();
		assertEquals(output, "CMakeCache.txt");
		output = br.readLine();
		assertEquals(output, "CMakeFiles");
		output = br.readLine();
		assertEquals(output, "cmake_install.cmake");
		output = br.readLine();
		assertEquals(output, "CMakeLists.txt");
		output = br.readLine();
		assertEquals(output, "Common");
		output = br.readLine();
		assertEquals(output, "daemon");
		output = br.readLine();
		assertEquals(output, "daemon_dir");
		output = br.readLine();
		assertEquals(output, "dns_daemon");
		output = br.readLine();
		assertEquals(output, "Makefile");
		output = br.readLine();
		assertEquals(output, "punchlist");
		output = br.readLine();
		assertEquals(output, "test_uplink");
		output = br.readLine();
		assertEquals(output, "");
	}

	static int getFilesInFolder(String folder) {
		List<String> fileNames = new ArrayList<>();
		try {
			DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(folder));
			for (Path path : directoryStream) {
				fileNames.add(path.toString());
			}
		} catch (IOException ex) {
		}
		return fileNames.size();
	}

	static void testRootDirEnum(BufferedReader br, OutputStreamWriter bw) throws IOException {
		bw.write("dir" + System.lineSeparator());
		bw.flush();
		String output = br.readLine();
		assertEquals(output, " Volume in drive C is OS");
		output = br.readLine();
		assertTrue(output.startsWith(" Volume Serial Number is "));
		output = br.readLine();
		assertEquals(output, "");
		output = br.readLine();
		assertEquals(output, " Directory of " + TestConstants.TEST_EXECUTION_ROOT);
		output = br.readLine();
		assertEquals(output, "");
		output = br.readLine();
		assertTrue(output.contains("<DIR>          ."));
		output = br.readLine();
		assertTrue(output.contains("<DIR>          .."));
		// Sub out the hidden elements
		for (int idx = 0; idx < getFilesInFolder(TestConstants.TEST_EXECUTION_ROOT) - 2; idx++)
			br.readLine();

		output = br.readLine();
		if (!output.contains("bytes")) {
			output = br.readLine();
			assertTrue(output.contains("bytes"));
		}
		output = br.readLine();
		assertTrue(output.contains("bytes free"));
		output = br.readLine();
		assertEquals(output, "");
	}

	static void testCscDirEnum(BufferedReader br, OutputStreamWriter bw) throws IOException {
		bw.write("dir" + System.lineSeparator());
		bw.flush();
		String output = br.readLine();
		assertEquals(output, " Volume in drive C is OS");
		output = br.readLine();
		assertTrue(output.startsWith(" Volume Serial Number is "));
		output = br.readLine();
		assertEquals(output, "");
		output = br.readLine();
		assertEquals(output, " Directory of " + TestConstants.TEST_EXECUTION_ROOT + "\\localAgent\\csc");
		output = br.readLine();
		assertEquals(output, "");
		output = br.readLine();
		assertTrue(output.contains("<DIR>          ."));
		output = br.readLine();
		assertTrue(output.contains("<DIR>          .."));
		for (int idx = 0; idx < getFilesInFolder(TestConstants.TEST_EXECUTION_ROOT + "\\localAgent\\csc") - 2; idx++)
			br.readLine();
		output = br.readLine();
		System.out.println(output);
		assertTrue(output.contains("bytes"));
		output = br.readLine();
		assertTrue(output.contains("bytes free"));
		output = br.readLine();
		assertEquals(output, "");
	}

	public static void test(TestConfiguration config) {

		Properties prop = new Properties();
		try (InputStream input = new FileInputStream("test" + File.separator + config.getServerConfigFile())) {

			// load a properties file
			prop.load(input);

		} catch (IOException ex) {
			System.out.println("Unable to load config file");
			fail(ex.getMessage());
		}

		try {
			// This hack is b/c for some reason the C++ daemon doesn't create the dir on my
			// laptop
			Files.createDirectories(Paths.get(prop.getProperty(Constants.DAEMONLZHARVEST),
					InetAddress.getLocalHost().getHostName().toUpperCase() + "-screen", "matte"));
			// end hack

			Files.deleteIfExists(Paths.get("System.Net.Sockets.SocketException"));
			Files.deleteIfExists(Paths.get("localAgent", "csc", "System.Net.Sockets.SocketException"));

			try {
				Thread.sleep(5000);// allow both commander and daemon to start
			} catch (InterruptedException e) {
				// Ensure that python client has connected
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
				if (config.isTestTwoClients()) {
					connectionSetupGenericTwoClient(remote, bw, br, config.isTestSecondaryClient());
				} else {
					connectionSetupGeneric(remote, bw, br, config.os == TestConfiguration.OS.LINUX, config.isRemote());
				}
			} catch (Exception ex) {
				fail(ex.getMessage());
			}

			if (config.protocol.equals("SMTP")) {
				String output = br.readLine();
				assertEquals(output, "Daemon alive");
			}

			System.out.println("getUID test");
			bw.write("getuid" + System.lineSeparator());
			bw.flush();

			String output = br.readLine();
			// System.out.println("Username: " + output);
			if (config.os == TestConfiguration.OS.LINUX) {
				assertEquals("Username: " + TestConstants.USERNAME_LINUX, output);
			} else {
				if (config.isRemote()) {
					assertTrue(output.startsWith("Username: "));
				} else {
					assertEquals("Username: " + System.getProperty("user.name"), output);
				}
			}
			output = br.readLine();
			// System.out.println("Home Dir: " + output);
			if (config.lang.equals("C++")) {
				assertEquals(output, "Home Directory: Not supported");
			} else {
				if (config.os == TestConfiguration.OS.LINUX) {
					assertEquals(output, "Home Directory: /home/" + TestConstants.USERNAME_LINUX);
				} else {
					if (config.isRemote()) {
						assertTrue(output.startsWith("Home Directory: C:\\Users\\"));
					} else {
						assertEquals(output, "Home Directory: " + System.getProperty("user.home"));
					}
				}
			}
			output = br.readLine();
			// System.out.println("Hostname: " + output);
			if (config.os == TestConfiguration.OS.LINUX) {
				assertEquals(output, "Hostname: " + TestConstants.HOSTNAME_LINUX);
			} else {
				if (config.lang.equals("C++")) {
					assertEquals(output, "Hostname: " + InetAddress.getLocalHost().getHostName().toUpperCase());
				} else {
					if (config.isRemote()) {
						assertTrue(output.startsWith("Hostname: "));
					} else {
						assertEquals(output, "Hostname: " + InetAddress.getLocalHost().getHostName());
					}
				}
			}
			output = br.readLine();
			assertEquals(output, "");

			System.out.println("pwd test");
			bw.write("pwd" + System.lineSeparator());
			bw.flush();
			output = br.readLine();
			if (config.os == TestConfiguration.OS.LINUX) {
				assertEquals(output, TestConstants.EXECUTIONROOT_LINUX);
				// testdir
			} else if (config.lang.equals("Native")) {
				if (config.isRemote()) {
					assertTrue(output.startsWith("C:\\Users\\"));
				} else {
					assertEquals(output, TestConstants.TEST_EXECUTION_ROOT);
				}
			} else {
				if (config.isExecInRoot()) {
					assertEquals(output, TestConstants.TEST_EXECUTION_ROOT);
					System.out.println("dir test");
					testRootDirEnum(br, bw);
				} else {
					assertEquals(output, TestConstants.TEST_EXECUTION_ROOT + "\\localAgent\\csc");
					System.out.println("dir test");
					testCscDirEnum(br, bw);
				}
			}

			if(config.lang.equals("python") && config.protocol.equals("SMTP")) {
				System.out.println("Flushing");
				br.readLine();//Flush a bad line feed
			}
			
			System.out.println("uplink test");
			if (config.os == TestConfiguration.OS.LINUX) {
				bw.write("uplink test_uplink" + System.lineSeparator());
				bw.flush();
				output = br.readLine();
				assertEquals(output,
						"<control> uplinked test_uplink VGhpcyBpcyBhIHRlc3QgZmlsZSB0byB1cGxpbmsgb24gTGludXguIEl0IGhhcyBubyBwb2ludC4K");
			} else {
				if (config.isExecInRoot()) {
					bw.write("uplink " + "test\\default_commands" + System.lineSeparator());
				} else {
					bw.write("uplink " + "..\\..\\test\\default_commands" + System.lineSeparator());
				}
				bw.flush();
				output = br.readLine();
				if (config.lang.equals("C++") || config.lang.equals("Native")) {
					assertEquals(output,
							"<control> uplinked test\\default_commands OmFsbA0KcHdkDQo6dXNlci1tYXR0ZQ0KY2QgLi4NCnB3ZA0KOmhvc3QtR0xBTURSSU5HDQpwd2QNCmNkIC4NCnB3ZA==");
				} else {
					assertEquals(output,
							"<control> uplinked default_commands OmFsbA0KcHdkDQo6dXNlci1tYXR0ZQ0KY2QgLi4NCnB3ZA0KOmhvc3QtR0xBTURSSU5HDQpwd2QNCmNkIC4NCnB3ZA==");
				}
			}

			if (config.os != TestConfiguration.OS.LINUX) {
				System.out.println("Testing download");

				byte[] fileBytes = Files.readAllBytes(Paths.get("test\\default_commands"));
				byte[] encoded = Base64.getEncoder().encode(fileBytes);
				String encodedString = new String(encoded, StandardCharsets.US_ASCII);
				bw.write("<control> download "
						+ Paths.get("test\\default_commands").getFileName().toString().replaceAll(" ", "_") + " "
						+ encodedString + System.lineSeparator());
				bw.flush();
				// Give time for endpoint to receive
				Time.sleepWrapped(5000);
				output = br.readLine();
				assertEquals(output, "File written: default_commands");

				if (config.isRemote()) {
					bw.write("uplink msbuild.txt" + System.lineSeparator());
					bw.flush();
					output = br.readLine();
					assertEquals(output,
							"<control> uplinked msbuild.txt PFByb2plY3QgVG9vbHNWZXJzaW9uPSI0LjAiIHhtbG5zPSJodHRwOi8vc2NoZW1hcy5taWNyb3NvZnQuY29tL2RldmVsb3Blci9tc2J1aWxkLzIwMDMiPgogIDxUYXJnZXQgTmFtZT0iSGVsbG8iPgogICAgPFNpbXBsZVRhc2sxIE15UHJvcGVydHk9IkhlbGxvISIgLz4KICA8L1RhcmdldD4KICA8VXNpbmdUYXNrCiAgICBUYXNrTmFtZT0iU2ltcGxlVGFzazEuU2ltcGxlVGFzazEiCiAgICBBc3NlbWJseUZpbGU9Im15X3Rhc2suZGxsIiAvPgo8L1Byb2plY3Q+");
					bw.write("del msbuild.txt" + System.lineSeparator());
					bw.flush();
					output = br.readLine();// Blank line
					output = br.readLine();// Prompt
				} else {
					assertTrue(Files.exists(Paths.get("default_commands")));
					byte[] newFileBytes = Files.readAllBytes(Paths.get("default_commands"));
					assertEquals(newFileBytes.length, fileBytes.length);
					for (int idx = 0; idx < newFileBytes.length; idx++) {
						assertEquals(fileBytes[idx], newFileBytes[idx]);
					}
					Files.delete(Paths.get("default_commands"));
				}
			} else if (config.os == TestConfiguration.OS.LINUX) {
				System.out.println("Download test executing");
				byte[] fileBytes = Files.readAllBytes(Paths.get("execCentral.bat"));
				byte[] encoded = Base64.getEncoder().encode(fileBytes);
				String encodedString = new String(encoded, StandardCharsets.US_ASCII);
				bw.write("<control> download " + "execCentral.bat" + " " + encodedString + System.lineSeparator());
				bw.flush();
				// Give time for endpoint to receive
				try {
					Thread.sleep(5000);
				} catch (Exception ex) {
				}
				output = br.readLine();
				assertEquals(output, "File written: execCentral.bat");

				bw.write("uplink execCentral.bat" + System.lineSeparator());
				bw.flush();
				output = br.readLine();
				assertEquals(output,
						"<control> uplinked execCentral.bat amF2YSAtY3AgQzJDb21tYW5kZXIuamFyO2dzb24tMi44LjcuamFyO2pha2FydGEuYWN0aXZhdGlvbi0yLjAuMC5qYXI7amFrYXJ0YS5hY3RpdmF0aW9uLWFwaS0yLjAuMC5qYXI7amFrYXJ0YS5tYWlsLTIuMC4wLmphciBjMi5SdW5uZXIgdGVzdC5wcm9wZXJ0aWVz");
			}

			if (((config.lang.equals("C#") && !config.protocol.equals("DNS")) || config.lang.equals("C++")
					|| config.lang.equals("python")) && config.os != TestConfiguration.OS.LINUX) {
				System.out.println("Screenshot test");
				bw.write("screenshot" + System.lineSeparator());
				bw.flush();
				output = br.readLine();
				assertEquals(output, "Screenshot successful");

				testScreenshotsOnFS(config.lang);
			}

			if (config.os != TestConfiguration.OS.LINUX) {
				testClipboard(br, bw, config.lang, config.isRemote());
			}

			testCat(br, bw, config);
			// execCentral will be local to Linux after the uplink test, whereas for Windows
			// it is
			// already in root.
			if (config.os == TestConfiguration.OS.LINUX) {
				bw.write("rm execCentral.bat" + System.lineSeparator());
				bw.flush();
			}

			bw.write("die" + System.lineSeparator());
			bw.flush();

			try {
				Thread.sleep(2500);
			} catch (InterruptedException e) {
			}

			// TODO: PS test

			bw.close();
			br.close();
			remote.close();

			Files.deleteIfExists(Paths.get("System.Net.Sockets.SocketException"));
			Files.deleteIfExists(Paths.get("localAgent", "csc", "System.Net.Sockets.SocketException"));
		} catch (IOException ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}

		cleanup(config.lang);
	}

	public static void cleanup(String lang) {
		File dir = new File("test");
		File[] matches = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				String hostname;
				try {
					hostname = InetAddress.getLocalHost().getHostName().toUpperCase();
					return name.toUpperCase().startsWith(hostname);
				} catch (UnknownHostException e) {
					return false;
				}
			}
		});

		if (matches.length >= 1) {

			try {
				Files.walk(matches[0].toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile)
						.forEach(File::delete);
			} catch (IOException e2) {
				e2.printStackTrace();
				fail("Cannot clean up harvester");
			}
		}

		try {
			// Delete Cookie Deleter tmp files
			Files.deleteIfExists(Paths.get(TestConstants.TMP_DIR, TestConstants.TMP_CHROME_COOKIES));
			Files.deleteIfExists(Paths.get(TestConstants.TMP_DIR, TestConstants.TMP_FIREFOX_COOKIES));
			Files.deleteIfExists(Paths.get(TestConstants.TMP_DIR, TestConstants.TMP_EDGE_COOKIES));

			// Delete Cookie stealer tmp files
			Files.deleteIfExists(Paths.get(TestConstants.TMP_DIR, TestConstants.TMP_GENERIC));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void testClipboard(BufferedReader br, OutputStreamWriter bw, String lang, boolean isRemote)
			throws IOException {
		System.out.println("Testing clipboard");
		bw.write("clipboard" + System.lineSeparator());
		bw.flush();
		String output = br.readLine();
		assertEquals(output, "Clipboard captured");

		if (!isRemote) {// TODO: In project for remote test mgmt daemon, get hostname and add to cleanup
						// here
			File dir = new File("test");

			File[] matches = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					String hostname;
					try {
						hostname = InetAddress.getLocalHost().getHostName();
						if (lang.equals("C++")) {
							hostname = hostname.toUpperCase();
						}
						// The file name will be hostname-pid to start
						if (lang.equalsIgnoreCase("Native")) {
							return name.startsWith(hostname);
						} else {
							return name.startsWith(hostname) && name.matches(".*\\d.*");
						}
					} catch (UnknownHostException e) {
						return false;
					}
				}
			});
			assertTrue(matches.length > 0);
			File[] clipboard = matches[0].listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.startsWith("Clipboard");
				}
			});
			assertEquals(clipboard.length, 1);

			try {
				Files.walk(matches[0].toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile)
						.forEach(File::delete);
			} catch (IOException e2) {
				e2.printStackTrace();
				fail("Cannot clean up harvester");
			}
		}
	}

	static void testCat(BufferedReader br, OutputStreamWriter bw, TestConfiguration config) throws IOException {
		// Test simple CAT reading a file
		System.out.println("General cat test - reading file");
		String targetFileRoot = "execCentral.bat";
		String targetTempCopyRoot = "execCentral.bat.tmp";
		String targetFile = targetFileRoot;
		String targetTempCopy = targetTempCopyRoot;
		if (!config.isExecInRoot()) {
			targetFile = "..\\..\\execCentral.bat";
			targetTempCopy = "..\\..\\execCentral.bat.tmp";
		}
		bw.write("cat " + targetFile + System.lineSeparator());
		bw.flush();
		String output = br.readLine();
		assertEquals(output,
				"java -cp C2Commander.jar;gson-2.8.7.jar;jakarta.activation-2.0.0.jar;jakarta.activation-api-2.0.0.jar;jakarta.mail-2.0.0.jar c2.Runner test.properties");
		System.out.println("reading flush");
		output = br.readLine();
		if (config.lang.equals("Native") && config.os != TestConfiguration.OS.LINUX) {
			assertTrue(output.startsWith("C:\\"));
		} else {
			assertEquals(output, "");
		}

		if (!config.lang.equals("Native") || config.os == TestConfiguration.OS.LINUX) {
			// Test simple CAT reading a file with line numbers
			System.out.println("Cat test with line numbers");
			bw.write("cat -n " + targetFile + System.lineSeparator());
			bw.flush();
			output = br.readLine();
			if (config.os == TestConfiguration.OS.LINUX && config.lang.equals("Native")) {
				assertEquals(output,
						"     1\tjava -cp C2Commander.jar;gson-2.8.7.jar;jakarta.activation-2.0.0.jar;jakarta.activation-api-2.0.0.jar;jakarta.mail-2.0.0.jar c2.Runner test.properties");
			} else {
				assertEquals(output,
						"1: java -cp C2Commander.jar;gson-2.8.7.jar;jakarta.activation-2.0.0.jar;jakarta.activation-api-2.0.0.jar;jakarta.mail-2.0.0.jar c2.Runner test.properties");
			}
			output = br.readLine();
			assertEquals(output, "");
		}

		// Test CAT writing to new file
		if (config.lang.equals("Native") || config.lang.equals("C++") || config.lang.equals("python")) {// || isLinux) {
			if (config.isExecInRoot()) {
				bw.write("cat >newFile.txt" + System.lineSeparator());
			} else {
				bw.write("cat >..\\..\\newFile.txt" + System.lineSeparator());
			}
			bw.flush();
			bw.write("Line" + System.lineSeparator());
			bw.flush();
			bw.write("otherline and stuff" + System.lineSeparator());
			bw.flush();
			bw.write("<done>" + System.lineSeparator());
			bw.flush();

			// Minimum beacon time
			try {
				Thread.sleep(2500);
			} catch (InterruptedException ex) {
				// ignore
			}

			System.out.println("Checking for write confirmation");
			output = br.readLine();
			assertEquals(output, "Data written");

			if (config.os != TestConfiguration.OS.LINUX && !config.isRemote()) {
				BufferedReader fileReader = new BufferedReader(new FileReader("newFile.txt"));
				;
				String line = fileReader.readLine();
				assertEquals(line, "Line");
				line = fileReader.readLine();
				assertEquals(line, "otherline and stuff");
				line = fileReader.readLine();
				assertEquals(line, null);
				fileReader.close();
			} else {
				bw.write("cat newFile.txt" + System.lineSeparator());
				bw.flush();
				output = br.readLine();
				assertEquals(output, "Line");
				output = br.readLine();
				assertEquals(output, "otherline and stuff");
				output = br.readLine();
				assertEquals(output, "");
				if (config.lang.equals("Native")) {
					output = br.readLine();
					if (config.isRemote()) {
						assertTrue(output.startsWith("C:\\Users\\"));
					} else {
						assertEquals(output, "");
					}

				}
			}

			// Test CAT canceling writing to a new file
			System.out.println("Testing cat cancellation");
			if (config.isExecInRoot()) {
				bw.write("cat >newFile.txt" + System.lineSeparator());
			} else {
				bw.write("cat >..\\..\\newFile.txt" + System.lineSeparator());
			}
			bw.write("Line" + System.lineSeparator());
			bw.write("otherline and stuff" + System.lineSeparator());
			bw.write("<cancel>" + System.lineSeparator());
			bw.flush();

			// Minimum beacon time
			try {
				Thread.sleep(2500);
			} catch (InterruptedException ex) {
				// ignore
			}
			output = br.readLine();
			assertEquals(output, "Abort: No file write");

			System.out.println("Test CAT appending to an existing file");
			if (config.isExecInRoot()) {
				bw.write("cat >>newFile.txt" + System.lineSeparator());
			} else {
				bw.write("cat >>..\\..\\newFile.txt" + System.lineSeparator());
			}
			bw.flush();
			bw.write("Line" + System.lineSeparator());
			bw.flush();
			bw.write("otherline and stuff" + System.lineSeparator());
			bw.flush();
			bw.write("<done>" + System.lineSeparator());
			bw.flush();

			// Minimum beacon time
			try {
				Thread.sleep(2500);
			} catch (InterruptedException ex) {
				// ignore
			}

			System.out.println("Checking for write confirmation");
			output = br.readLine();
			assertEquals(output, "Data written");

			if (config.os == TestConfiguration.OS.LINUX || config.isRemote()) {
				bw.write("cat newFile.txt" + System.lineSeparator());
				bw.flush();
				output = br.readLine();
				assertEquals(output, "Line");
				output = br.readLine();
				assertEquals(output, "otherline and stuff");
				output = br.readLine();
				assertEquals(output, "Line");
				output = br.readLine();
				assertEquals(output, "otherline and stuff");
				output = br.readLine();
				assertEquals(output, "");
				// output = br.readLine();
				// assertEquals(output, "");

				if (config.os != TestConfiguration.OS.LINUX) {
					bw.write("del newFile.txt" + System.lineSeparator());
					bw.flush();
					output = br.readLine();
					assertTrue(output.startsWith("C:\\Users\\"));
					output = br.readLine();// newline flush
					output = br.readLine();// prompt flush
				} else {
					bw.write("rm newFile.txt" + System.lineSeparator());
					bw.flush();
					output = br.readLine();
					assertEquals(output, "");
				}
			} else {
				BufferedReader fileReader = new BufferedReader(new FileReader("newFile.txt"));
				;
				String line = fileReader.readLine();
				assertEquals(line, "Line");
				line = fileReader.readLine();
				assertEquals(line, "otherline and stuff");
				line = fileReader.readLine();
				assertEquals(line, "Line");
				line = fileReader.readLine();
				assertEquals(line, "otherline and stuff");
				line = fileReader.readLine();
				assertEquals(line, null);
				fileReader.close();
				Files.deleteIfExists(Paths.get("newFile.txt"));
			}
		}

		// Test CAT copying to file
		System.out.println("Test cat copying file");
		bw.write("cat " + targetFile + " > " + targetTempCopy + System.lineSeparator());
		bw.flush();
		// Minimum beacon time
		try {
			Thread.sleep(2500);
		} catch (InterruptedException ex) {
			// ignore
		}
		output = br.readLine();
		assertEquals(output, "File write executed");
		// We will test in the >> use case that this file write occured
		byte[] f1;
		byte[] f2;
		if (config.os != TestConfiguration.OS.LINUX && !config.isRemote()) {
			f1 = Files.readAllBytes(Paths.get(targetFileRoot));
			f2 = Files.readAllBytes(Paths.get(targetTempCopyRoot));
			assertTrue(Arrays.equals(f1, f2));
		}
		// Test CAT appending to an existing file from existing file
		System.out.println("Test cat copying appended file");
		bw.write("cat " + targetFile + " >> " + targetTempCopy + System.lineSeparator());
		bw.flush();
		// Minimum beacon time
		try {
			Thread.sleep(2500);
		} catch (InterruptedException ex) {
			// ignore
		}
		output = br.readLine();
		assertEquals(output, "Appended file");
		if (config.os == TestConfiguration.OS.LINUX || config.isRemote()) {
			bw.write("uplink execCentral.bat.tmp" + System.lineSeparator());
			bw.flush();
			output = br.readLine();
			assertEquals(output,
					"<control> uplinked execCentral.bat.tmp amF2YSAtY3AgQzJDb21tYW5kZXIuamFyO2dzb24tMi44LjcuamFyO2pha2FydGEuYWN0aXZhdGlvbi0yLjAuMC5qYXI7amFrYXJ0YS5hY3RpdmF0aW9uLWFwaS0yLjAuMC5qYXI7amFrYXJ0YS5tYWlsLTIuMC4wLmphciBjMi5SdW5uZXIgdGVzdC5wcm9wZXJ0aWVzamF2YSAtY3AgQzJDb21tYW5kZXIuamFyO2dzb24tMi44LjcuamFyO2pha2FydGEuYWN0aXZhdGlvbi0yLjAuMC5qYXI7amFrYXJ0YS5hY3RpdmF0aW9uLWFwaS0yLjAuMC5qYXI7amFrYXJ0YS5tYWlsLTIuMC4wLmphciBjMi5SdW5uZXIgdGVzdC5wcm9wZXJ0aWVz");
			bw.write("rm execCentral.bat.tmp" + System.lineSeparator());
			bw.flush();
		} else {
			f1 = Files.readAllBytes(Paths.get(targetFileRoot));
			f2 = Files.readAllBytes(Paths.get(targetTempCopyRoot));
			assertTrue(f1.length * 2 == f2.length);
		}

		Files.deleteIfExists(Paths.get(targetTempCopyRoot));
	}
}
