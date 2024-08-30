package c2.tcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import c2.Constants;
import c2.session.IOManager;
import c2.session.log.IOLogger;
import util.Time;
import util.test.RunnerTestGeneric;

class GenericTCPInitiatorTest {

	private ExecutorService service;
	GenericTCPInitiator testObject;

	@AfterEach
	void cleanup() {
		RunnerTestGeneric.cleanup();
	}
	
	private void spinUpGenericTCPInitiatorTest(int port, IOManager io) {
		service = Executors.newCachedThreadPool();
		testObject = new GenericTCPInitiator();
		Properties prop = new Properties();
		prop.put(Constants.NATIVESHELLPORT, new String(port + ""));
		prop.put(Constants.DAEMONLZHARVEST, "fake_dir");
		try {
			testObject.initialize(io, prop, null, null);
		} catch (Exception ex) {
			fail(ex.getMessage());
		}
		service.submit(testObject);
	}

	private void stopGenericTCPInitiator() {
		testObject.stop();
	}

	@Test
	void testWindows() {
		IOManager io = new IOManager(new IOLogger(Paths.get("test", "log")), null);
		Random rnd = new Random();
		int testPort = 40000 + rnd.nextInt(1000);
		spinUpGenericTCPInitiatorTest(testPort, io);
		testObject.awaitStartup();
		try {
			Socket clientSocket = new Socket("127.0.0.1", testPort);
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

			String windowsHeader = "Microsoft Windows [Version 10.0.19042.1415]\r\n"
					+ "(c) Microsoft Corporation. All rights reserved.\r\n" + "\r\n"
					+ "C:\\Users\\fake_user\\OneDrive\\Documents\\Software\\TheAllCommander\\agents\\python>";

			out.write(windowsHeader);
			out.flush();

			String cmd = in.readLine();
			assertEquals("cmd /k hostname", cmd);

			String hostnameResponse = "cmd /k hostname\r\n" + "Glamdring\r\n" + "\r\n"
					+ "C:\\Users\\fake_user\\OneDrive\\Documents\\Software\\TheAllCommander\\agents\\python>";

			out.write(hostnameResponse);
			out.flush();

			cmd = in.readLine();
			assertEquals("echo %username%", cmd);

			String usernameResponse = "echo %username%\r\n" + "matte\r\n" + "\r\n"
					+ "C:\\Users\\fake_user\\OneDrive\\Documents\\Software\\TheAllCommander\\agents\\python>";

			out.write(usernameResponse);
			out.flush();
			// Contact and send header
			
			assertEquals("net session 2>&1", in.readLine());
			out.write("net session 2>&1\r\nAccess is denied.\r\n\r\n" + "C:\\Users\\fake_user\\OneDrive\\Documents\\Software\\TheAllCommander\\agents\\python>");
			out.flush();

			int counter = 0;
			while (!io.hasSession(2) && counter < 10) {
				Time.sleepWrapped(100);
				counter++;
			}
			assertTrue(io.hasSession(2));

			testWindowsCd(in, out, io, 2);
			testWindowsArbitraryCmd(in, out, io, 2);
			testWindowsCatBasic(in, out, io, 2);
			testWindowsBuilderCat(in, out, io, 2);

			in.close();
			out.close();
			clientSocket.close();
		} catch (IOException ex) {
			fail(ex.getMessage());
		}
		stopGenericTCPInitiator();
	}

	@Test
	void testLinux() {
		IOManager io = new IOManager(new IOLogger(Paths.get("test", "log")), null);
		Random rnd = new Random();
		int testPort = 40000 + rnd.nextInt(1000);
		spinUpGenericTCPInitiatorTest(testPort, io);
		testObject.awaitStartup();
		try {
			Socket clientSocket = new Socket("127.0.0.1", testPort);
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

			String cmd = in.readLine();
			assertEquals("uname -a", cmd);

			String unameResp = "Linux kali 5.10.0-kali3-amd64 #1 SMP Debian 5.10.13-1kali1 (2021-02-08) x86_64 GNU/Linux";

			out.write(unameResp);
			out.flush();

			cmd = in.readLine();
			assertEquals("hostname", cmd);

			String hostnameResp = "24601";

			out.write(hostnameResp);
			out.flush();

			cmd = in.readLine();
			assertEquals("whoami", cmd);

			String usernameResponse = "kali";

			out.write(usernameResponse);
			out.flush();
			// Contact and send header

			int counter = 0;
			while (!io.hasSession(2) && counter < 10) {
				Time.sleepWrapped(100);
				counter++;
			}
			assertTrue(io.hasSession(2));

			testLinuxDieTranslation(in, out, io, 2);
			testLinuxUnalteredCmd(in, out, io, 2);
			testLinuxCd(in, out, io, 2);

			in.close();
			out.close();
			clientSocket.close();
		} catch (IOException ex) {
			fail(ex.getMessage());
		}
		stopGenericTCPInitiator();
	}

	private void testLinuxUnalteredCmd(BufferedReader in, PrintWriter out, IOManager session, int sessionId) throws IOException {
		session.sendCommand(sessionId, "ls");
		String cmd = in.readLine();
		assertEquals("ls", cmd);
		out.write("total 6936\n"
				+ "drwxr-xr-x 11 kali kali    4096 Jan 12 15:38 .\n"
				+ "drwxr-xr-x 34 kali kali    4096 Jan 11 22:04 ..\n"
				+ "-rw-r--r--  1 kali kali   81272 Jun  9  2021 C2Commander.jar\n"
				+ "");
		out.flush();
		
		String response = session.pollIO(sessionId);
		int counter = 0;
		while (response == null && counter < 15) {
			response = session.pollIO(sessionId);
			counter++;
			Time.sleepWrapped(100);
		}
		
		assertEquals("total 6936\n"
				+ "drwxr-xr-x 11 kali kali    4096 Jan 12 15:38 .\n"
				+ "drwxr-xr-x 34 kali kali    4096 Jan 11 22:04 ..\n"
				+ "-rw-r--r--  1 kali kali   81272 Jun  9  2021 C2Commander.jar\n"
				+ "", response);
	}
	
	private void testLinuxDieTranslation(BufferedReader in, PrintWriter out, IOManager session, int sessionId) throws IOException {
		session.sendCommand(sessionId, "die");
		String cmd = in.readLine();
		assertEquals("exit", cmd);
	}
	
	private void testLinuxCd(BufferedReader in, PrintWriter out, IOManager session, int sessionId) throws IOException {
		session.sendCommand(sessionId, "cd ..");
		String cmd = in.readLine();
		assertEquals("cd ..", cmd);
		cmd = in.readLine();
		assertEquals("pwd", cmd);

		String cdResponse = "/home/kali/test";
		out.write(cdResponse);
		out.flush();

		String response = session.pollIO(sessionId);
		int counter = 0;
		while (response == null && counter < 15) {
			response = session.pollIO(sessionId);
			counter++;
			Time.sleepWrapped(100);
		}
		assertEquals("/home/kali/test", response);
	}

	private void testWindowsCatBasic(BufferedReader in, PrintWriter out, IOManager session, int sessionId) throws IOException {
		session.sendCommand(sessionId, "cat a_file");
		String cmd = in.readLine();
		assertEquals("type \"a_file\"", cmd);
	}

	private void testWindowsBuilderCat(BufferedReader in, PrintWriter out, IOManager session, int sessionId) throws IOException {
		session.sendCommand(sessionId, "cat >>a_file");
		session.sendCommand(sessionId, "content");
		session.sendCommand(sessionId, "more content");
		session.sendCommand(sessionId, "<done>");


		String cmd = in.readLine();
		assertEquals("powershell -c \"add-content a_file 'content' \"", cmd);
		cmd = in.readLine();
		assertEquals("powershell -c \"add-content a_file 'more content' \"", cmd);
	}

	private void testWindowsCd(BufferedReader in, PrintWriter out, IOManager session, int sessionId) throws IOException {
		session.sendCommand(sessionId, "cd ..");
		String cmd = in.readLine();
		assertEquals("cd ..", cmd);

		String cdResponse = "cd ..\r\n" + "\r\n"
				+ "C:\\Users\\fake_user\\OneDrive\\Documents\\Software\\TheAllCommander\\agents\\python>";
		out.write(cdResponse);
		out.flush();

		cmd = in.readLine();
		assertEquals("echo %CD%", cmd);
		String shadowPwdResponse = "echo %CD%\r\n"
				+ "C:\\Users\\fake_user\\OneDrive\\Documents\\Software\\TheAllCommander\\agents\\python\r\n" + "\r\n"
				+ "C:\\Users\\fake_user\\OneDrive\\Documents\\Software\\TheAllCommander\\agents\\python>";
		out.write(shadowPwdResponse);
		out.flush();

		String response = session.pollIO(sessionId);
		int counter = 0;
		while (response == null && counter < 10) {
			response = session.pollIO(sessionId);
			counter++;
			Time.sleepWrapped(100);
		}
		assertEquals("C:\\Users\\fake_user\\OneDrive\\Documents\\Software\\TheAllCommander\\agents\\python" + System.lineSeparator(),
				response);
	}

	private void testWindowsArbitraryCmd(BufferedReader in, PrintWriter out, IOManager session, int sessionId) throws IOException {
		session.sendCommand(sessionId, "dir");
		String cmd = in.readLine();
		assertEquals("dir", cmd);

		String dirResponse = "dir\r\n" + " Volume in drive C is OS\r\n" + " Volume Serial Number is AAA2-0F24\r\n"
				+ "\r\n"
				+ " Directory of C:\\Users\\fake_dir\\OneDrive\\Documents\\Software\\TheAllCommander\\agents\\python\r\n"
				+ "\r\n" + "01/12/2022  12:09 PM    <DIR>          .\r\n"
				+ "01/12/2022  12:09 PM    <DIR>          ..\r\n"
				+ "06/17/2021  08:53 PM                14 .gitignore\r\n"
				+ "07/08/2021  09:01 AM                67 default_commands\r\n"
				+ "09/08/2021  12:06 PM             2,098 directoryHarvester.py\r\n"
				+ "08/16/2021  02:43 PM             6,217 dnsAgent.py\r\n"
				+ "01/13/2022  08:28 AM             6,882 emailAgent.py\r\n"
				+ "08/16/2021  02:43 PM             4,242 httpsAgent.py\r\n"
				+ "09/13/2021  03:59 PM            17,749 localAgent.py\r\n"
				+ "09/14/2021  07:05 AM    <DIR>          __pycache__\r\n"
				+ "               7 File(s)         37,269 bytes\r\n"
				+ "               3 Dir(s)  13,530,017,792 bytes free\r\n" + "\r\n"
				+ "C:\\Users\\fake_dir\\OneDrive\\Documents\\Software\\TheAllCommander\\agents\\python>";
		out.write(dirResponse);
		out.flush();

		String response = session.pollIO(sessionId);
		int counter = 0;
		while (response == null && counter < 15) {
			response = session.pollIO(sessionId);
			counter++;
			Time.sleepWrapped(100);
		}
		// Did we strip the echo'd command
		assertTrue(response.startsWith(" Volume in drive C is OS"));
		assertTrue(response.endsWith("3 Dir(s)  13,530,017,792 bytes free" + System.lineSeparator()));
	}

}
