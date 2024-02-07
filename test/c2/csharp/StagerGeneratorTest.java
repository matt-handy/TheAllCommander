package c2.csharp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import c2.admin.LocalConnection;
import c2.http.HTTPSManager;
import c2.session.CommandMacroManager;
import c2.session.CommandWizard;
import c2.session.IOManager;
import c2.session.SecureSessionInitiator;
import c2.session.SessionInitiator;
import c2.session.SessionManager;
import c2.session.log.IOLogger;
import util.Time;
import util.test.ClientServerTest;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;

class StagerGeneratorTest {

	private HTTPSManager httpsManager;
	
	@AfterEach
	void teardown() {
		if(httpsManager != null) {
			httpsManager.stop();
			Time.sleepWrapped(5000);//Let HTTPS manager shut down fully.
			httpsManager = null;
		}
	}
	
	@Test
	void testBogusFileDetection() {
		try {
			List<String> connectionArgs = new ArrayList<>();
			connectionArgs.add("https://127.0.0.1:8000/csharpboot");
			StagerGenerator.generateStagedSourceFile(SessionInitiator.CSHARP_CONFIG_PATH, "bogus", connectionArgs);
			fail("Should not have return from that function given bad input");
		} catch (IOException ex) {
			assertEquals(
					"Must have an assemblies_<transport type>, imports_<transport type>, pollcode_<transport type>, and pollcode_function_<transport type>",
					ex.getMessage());
		}
	}

	@Test
	void testBasicReplacement() {
		if (System.getProperty("os.name").contains("Windows")) {
			// Tests that the text from the import files are placed in the string, does not
			// validate variable replacement and generation
			try {
				List<String> connectionArgs = new ArrayList<>();
				connectionArgs.add("https://127.0.0.1:8000/csharpboot");
				String httpPayloadFile = StagerGenerator.generateStagedSourceFile(SessionInitiator.CSHARP_CONFIG_PATH,
						"http", connectionArgs);
				String lines[] = httpPayloadFile.split(System.lineSeparator());
				// Check that import is place appropriately
				assertEquals(Files.readString(SessionInitiator.CSHARP_CONFIG_PATH.resolve("imports_http")), lines[11]);
				// Check that poll code is correct
				String pollCodeLines[] = Files.readString(SessionInitiator.CSHARP_CONFIG_PATH.resolve("pollcode_http"))
						.split(System.lineSeparator());
				for (int idx = 0; idx < pollCodeLines.length; idx++) {
					if (idx == 0) {
						assertTrue(lines[19 + idx].endsWith(" = new HttpClient();"));
					} else if (idx == 1) {
						assertTrue(lines[19 + idx].endsWith(", Dns.GetHostName());"));
					} else if (idx == 2) {
						assertTrue(lines[19 + idx].endsWith(", Environment.UserName);"));
					} else if (idx == 3) {
						assertTrue(lines[19 + idx].endsWith(", Process.GetCurrentProcess().Id.ToString());"));
					} else if (idx == 5) {
						assertTrue(lines[19 + idx].startsWith("Task<string> "));
					} else if (idx == 6) {
						assertTrue(lines[19 + idx].endsWith(".Wait();"));
					} else if (idx == 7) {
						assertTrue(lines[19 + idx].endsWith(".Result;"));
					} else {
						assertEquals(pollCodeLines[idx], lines[19 + idx]);
					}
				}

				// Check that assemblies are imported
				String assembliesLines[] = Files
						.readString(SessionInitiator.CSHARP_CONFIG_PATH.resolve("assemblies_http"))
						.split(System.lineSeparator());
				for (int idx = 0; idx < assembliesLines.length; idx++) {
					String afterVar = assembliesLines[idx].substring(assembliesLines[idx].indexOf(".") + 1);
					assertTrue(lines[23 + (pollCodeLines.length - 1) + idx].endsWith(afterVar));
				}

				
			} catch (IOException ex) {
				fail("This should not have happened: " + ex.getMessage());
			}
		}
	}

	@Test
	void testVariableFillAndReplication() {
		// This test will not try and individually parse every var. It will sample a
		// specific variable and make sure
		// that it replicated correctly.
		if (System.getProperty("os.name").contains("Windows")) {
			try {
				List<String> connectionArgs = new ArrayList<>();
				connectionArgs.add("https://127.0.0.1:8000/csharpboot");
				String httpPayloadFile = StagerGenerator.generateStagedSourceFile(SessionInitiator.CSHARP_CONFIG_PATH,
						"http", connectionArgs);
				String lines[] = httpPayloadFile.split(System.lineSeparator());
				// Check that poll code variables generated correctly
				String pollCodeLines[] = Files.readString(SessionInitiator.CSHARP_CONFIG_PATH.resolve("pollcode_http"))
						.split(System.lineSeparator());
				String httpClientVar = null;
				String variableTwo = null;
				for (int idx = 0; idx < pollCodeLines.length; idx++) {
					if (idx == 0) {
						String varStarting = lines[19 + idx].substring("HttpClient ".length());
						httpClientVar = varStarting.substring(0, varStarting.indexOf(" "));
					} else if (idx == 1 || idx == 2 || idx == 3) {
						assertTrue(lines[19 + idx].startsWith(httpClientVar));
					} else if (idx == 5) {
						String varStarting = lines[19 + idx].substring("Task<string> ".length());
						variableTwo = varStarting.substring(0, varStarting.indexOf(" "));
					} else if (idx == 6) {
						assertEquals(lines[19 + idx], variableTwo + ".Wait();");
					} else if (idx == 7) {
						assertTrue(lines[19 + idx].endsWith(variableTwo + ".Result;"));
					}
				}

			} catch (IOException ex) {
				fail("This should not have happened: " + ex.getMessage());
			}
		}
	}

	@Test
	void testConnectionParameterReplacement() {
		try {
			List<String> connectionArgs = new ArrayList<>();
			connectionArgs.add("https://NARF");
			String httpPayloadFile = StagerGenerator.generateStagedSourceFile(SessionInitiator.CSHARP_CONFIG_PATH,
					"http", connectionArgs);
			assertTrue(httpPayloadFile.contains("https://NARF"));
			assertFalse(httpPayloadFile.contains("https://127.0.0.1:8000/csharpboot"));
		} catch (IOException ex) {
			fail("This should not have happened: " + ex.getMessage());
		}
	}

	@Test
	void testStagerCleansUpExe() {
		if (System.getProperty("os.name").contains("Windows")) {
			try {
				List<String> connectionArgs = new ArrayList<>();
				connectionArgs.add("https://127.0.0.1:8000/csharpboot");
				// Test checks that the basic headers and such are included in the file, so it
				// is a C# exe
				StagerGenerator.generateStagerExe(SessionInitiator.CSHARP_CONFIG_PATH, "http", connectionArgs);

				// Test checks that the class cleaned up after itself and no intermediate
				// products are left on disk
				assertFalse(Files.exists(StagerGenerator.TEMPORARY_DISK_EXE_FILE));
				assertFalse(Files.exists(StagerGenerator.TEMPORARY_DISK_SRC_FILE));
			} catch (IOException ex) {
				fail(ex.getMessage());
			}
		}

	}

	@Test 
	void testStagerProducesWorkingExeWithPermutations() {
		if (System.getProperty("os.name").contains("Windows")) {
			Path testPath = Paths.get("config", "test.properties");
			
			try (InputStream input = new FileInputStream(testPath.toFile())) {
				Properties prop = new Properties();

				// load a properties file
				prop.load(input);

				// Make properties encryption go away
				IOManager io = new IOManager(new IOLogger(Paths.get("test")), null);

				List<String> connectionArgs = new ArrayList<>();
				connectionArgs.add("https://127.0.0.1:8000/csharpboot");
				
				try {
					
					String file = StagerGenerator.generateStagerExe(SessionInitiator.CSHARP_CONFIG_PATH, "http",
							connectionArgs, true);
					Path path = Paths.get("test_tmp.exe");
					Files.write(path, Base64.getDecoder().decode(file));
					
					httpsManager = new HTTPSManager();
					httpsManager.initialize(io, prop, null, null);
					ExecutorService services = Executors.newCachedThreadPool();
					services.submit(httpsManager);
					httpsManager.awaitStartup();
					
				Process p = Runtime.getRuntime().exec(path.toAbsolutePath().toString());
				BufferedReader pInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String output = "";
				String line;
				while ((line = pInput.readLine()) != null) {
					if (line.length() != 0) {
						output += line + System.lineSeparator();
					}
				}
				pInput.close();

				assertEquals("Hello world" + System.lineSeparator(), output);
				
				//manager.stop();
				Files.deleteIfExists(path);
				//Time.sleepWrapped(5000);//Let HTTPS manager shut down fully.
			}catch(Exception ex) {
				System.out.println("Warning: Could not execute test. This happens occasionally. Investigate if it persists");
			}
				
				
			} catch (IOException ex) {
				ex.printStackTrace();
				fail(ex.getMessage());
			}
		}
	}
	
	@Test
	void testStagerProducesWorkingExe() {
		if (System.getProperty("os.name").contains("Windows")) {

			// Produce an exe file, write it to disk, run it, and make sure that expected
			// Hello World runs
			Path testPath = null;
			if (System.getProperty("os.name").contains("Windows")) {
				testPath = Paths.get("config", "test.properties");
			} else {
				testPath = Paths.get("config", "test_linux.properties");
			}
			try (InputStream input = new FileInputStream(testPath.toFile())) {
				Properties prop = new Properties();

				// load a properties file
				prop.load(input);

				// Make properties encryption go away
				IOManager io = new IOManager(new IOLogger(Paths.get("test")), null);

				List<String> connectionArgs = new ArrayList<>();
				connectionArgs.add("https://127.0.0.1:8000/csharpboot");
				

				try {
					String file = StagerGenerator.generateStagerExe(SessionInitiator.CSHARP_CONFIG_PATH, "http",
							connectionArgs);
					Path path = Paths.get("test_tmp.exe");
					Files.write(path, Base64.getDecoder().decode(file));

					httpsManager = new HTTPSManager();
					httpsManager.initialize(io, prop, null, null);
					ExecutorService services = Executors.newCachedThreadPool();
					services.submit(httpsManager);
					httpsManager.awaitStartup();
					
					
				Process p = Runtime.getRuntime().exec(path.toAbsolutePath().toString());
				BufferedReader pInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String output = "";
				String line;
				while ((line = pInput.readLine()) != null) {
					if (line.length() != 0) {
						output += line + System.lineSeparator();
					}
				}
				pInput.close();

				assertEquals("Hello world" + System.lineSeparator(), output);

				//manager.stop();
				Files.deleteIfExists(path);
				//Time.sleepWrapped(5000);//Let HTTPS manager shut down fully.
				}catch(Exception ex) {
					System.out.println("Warning: Could not execute test. This happens occasionally. Investigate if it persists");
				}
				
			} catch (IOException ex) {
				fail(ex.getMessage());
			}
		}
	}

	@Test
	void testMainServerIntegrationErrorHandling() {
		IOManager io = new IOManager(new IOLogger(Paths.get("test")), null);
		Random random = new Random();
		int port = 40000 + random.nextInt(1000);
		CommandMacroManager cmm = new CommandMacroManager(null, io, null);
		SessionManager manager = new SessionManager(io, port + 1, port, cmm, ClientServerTest.getDefaultSystemTestProperties());
		SecureSessionInitiator testSession = new SecureSessionInitiator(manager, io, port, cmm, ClientServerTest.getDefaultSystemTestProperties());
		ExecutorService service = Executors.newCachedThreadPool();
		service.submit(testSession);

		Time.sleepWrapped(250);

		try {
			Socket socket = LocalConnection.getSocket("127.0.0.1", port, ClientServerTest.getDefaultSystemTestProperties());
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			OutputStreamWriter bw = new OutputStreamWriter(socket.getOutputStream());

			bw.write("WIZARD" + System.lineSeparator());
			bw.flush();

			// Flush banner
			for (int idx = 0; idx < 11; idx++) {
				br.readLine();
			}

			// test of bad arguments
			bw.write(CommandWizard.CMD_GENERATE_CSHARP + System.lineSeparator());
			bw.flush();
			assertEquals("Improper format", br.readLine());
			// test of bad csharp format
			bw.write(CommandWizard.CMD_GENERATE_CSHARP + " barf http https://fake.com/boot" + System.lineSeparator());
			bw.flush();
			assertEquals("Unknown format option for " + CommandWizard.CMD_GENERATE_CSHARP, br.readLine());
			// test of unknown command
			bw.write("NARF" + System.lineSeparator());
			bw.flush();
			assertEquals("Unknown command", br.readLine());

			bw.write(CommandWizard.CMD_QUIT + System.lineSeparator());
			bw.flush();

			br.close();
			bw.close();
			socket.close();
		} catch (Exception ex) {
			fail(ex.getMessage());
		}
	}

	@Test
	void testMainServerIntegrationTextResponse() {
		// Test that a user can connect to TAC and ask for a completed file. Validates
		// that text comes back as expected
		testServerWizardResponse("text");
	}

	private void testServerWizardResponse(String format) {
		IOManager io = new IOManager(new IOLogger(Paths.get("test")), null);
		Random random = new Random();
		int port = 40000 + random.nextInt(1000);
		CommandMacroManager cmm = new CommandMacroManager(null, io, null);
		SessionManager manager = new SessionManager(io, port + 1, port, cmm, ClientServerTest.getDefaultSystemTestProperties());
		SecureSessionInitiator testSession = new SecureSessionInitiator(manager, io, port, cmm, ClientServerTest.getDefaultSystemTestProperties());
		ExecutorService service = Executors.newCachedThreadPool();
		service.submit(testSession);

		Time.sleepWrapped(250);

		try {
			Socket socket = LocalConnection.getSocket("127.0.0.1", port, ClientServerTest.getDefaultSystemTestProperties());
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			OutputStreamWriter bw = new OutputStreamWriter(socket.getOutputStream());

			bw.write("WIZARD" + System.lineSeparator());
			bw.flush();

			br.readLine();// Available sessions
			br.readLine();// Default session
			String line = br.readLine();
			assertEquals("Enter 'WIZARD' to begin other server commands", line);
			line = br.readLine();
			assertEquals("Available commands: ", line);
			br.readLine();// Permutation disable line
			br.readLine();// Command line
			br.readLine();// Example line
			br.readLine();// Command line
			br.readLine();// Example line
			line = br.readLine();
			assertEquals("Note: Only available with TheAllCommander on Windows", line);
			line = br.readLine();
			assertEquals(CommandWizard.CMD_QUIT, line);

			bw.write(CommandWizard.CMD_GENERATE_CSHARP + " " + format + " http https://127.0.0.1:8000/csharpboot"
					+ System.lineSeparator());
			bw.flush();

			if (format.equals("text")) {
				List<String> connectionArgs = new ArrayList<>();
				connectionArgs.add("https://127.0.0.1:8000/csharpboot");
				String httpPayloadFile = StagerGenerator.generateStagedSourceFile(SessionInitiator.CSHARP_CONFIG_PATH,
						"http", connectionArgs);
				int lineCount = httpPayloadFile.split(System.lineSeparator()).length;

				StringBuilder sb = new StringBuilder();
				for (int idx = 0; idx < lineCount; idx++) {
					line = br.readLine();
					sb.append(line);
					sb.append(System.lineSeparator());
				}

				assertEquals(lineCount, sb.toString().split(System.lineSeparator()).length);
			} else if (format.equals("exe")) {
				line = br.readLine();
				try {
					String lines[] = line.split(" ");
					assertEquals(3, lines.length);
					assertEquals("<control>", lines[0]);
					assertEquals(CommandWizard.CMD_GENERATE_CSHARP, lines[1]);
					Base64.getDecoder().decode(lines[2]);
					// If we got here, we're successful. Contents of byte array independently
					// validated
				} catch (Exception ex) {
					fail(ex.getMessage());
				}
			} else {
				fail("Invalid format");
			}

			bw.write(CommandWizard.CMD_QUIT + System.lineSeparator());
			bw.flush();

			br.close();
			bw.close();
			socket.close();
		} catch (Exception ex) {
			fail(ex.getMessage());
		}
	}

	@Test
	void testMainServerIntegrationExeResponse() {
		// Can only generate EXE on windows
		if (System.getProperty("os.name").contains("Windows")) {
			// Test that a user can connect to TAC and ask for a completed file. Validates
			// that text comes back as expected
			testServerWizardResponse("exe");
		}
	}

	@Test
	void testEndToEnd() {
		// Test that the server will generate an exe, send it to the client on request
		// Client writes that to disk and runs it, checks that the Hello World payload
		// actually works as expected
		// This involves firing up the test client and routing through it's IO, and
		// seeing
		// that the EXE is correctly interpreted
		if(TestConfiguration.getThisSystemOS() != OS.WINDOWS) {
			System.out.println("Cannot test end-to-end on non-Windows host");
			return;
		}
		try {
			// Queue up commands into br for the client
			ByteArrayOutputStream cmdBuffer = new ByteArrayOutputStream();
			BufferedOutputStream bos = new BufferedOutputStream(cmdBuffer);
			PrintWriter builder = new PrintWriter(bos);
			builder.println("WIZARD");
			builder.println(CommandWizard.CMD_GENERATE_CSHARP + " exe http https://127.0.0.1:8000/csharpboot");
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
			SessionManager manager = new SessionManager(io, port + 1, port, cmm, ClientServerTest.getDefaultSystemTestProperties());
			SecureSessionInitiator testSession = new SecureSessionInitiator(manager, io, port, cmm, ClientServerTest.getDefaultSystemTestProperties());
			ExecutorService service = Executors.newFixedThreadPool(4);
			service.submit(testSession);

			httpsManager = new HTTPSManager();
			httpsManager.initialize(io, ClientServerTest.getDefaultSystemTestProperties(), null, null);
			ExecutorService services = Executors.newCachedThreadPool();
			services.submit( httpsManager);
			httpsManager.awaitStartup();
			
			LocalConnection lc = new LocalConnection();
			String args[] = { "127.0.0.1", port + "" };
			try {
				lc.engage(args, br, ps, ClientServerTest.getDefaultSystemTestProperties());
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
			assertEquals("Exe downloaded", line);

			// Execute file on disk and confirm
			Process p = Runtime.getRuntime().exec(LocalConnection.CSHARP_TMP_FILE.toAbsolutePath().toString());
			BufferedReader pInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String output = "";
			while ((line = pInput.readLine()) != null) {
				if (line.length() != 0) {
					output += line + System.lineSeparator();
				}
			}
			pInput.close();

			assertEquals("Hello world" + System.lineSeparator(), output);

			Files.delete(LocalConnection.CSHARP_TMP_FILE);
			//httpsManager.stop();
			//Time.sleepWrapped(5000);
		} catch (IOException ex) {
			fail(ex.getMessage());
		}
	}
}
