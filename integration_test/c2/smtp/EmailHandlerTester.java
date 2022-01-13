package c2.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import c2.Constants;
import util.Time;
import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;

public class EmailHandlerTester extends ClientServerTest{


	static Properties setup() {
		try (InputStream input = new FileInputStream("config" + File.separator + "test.properties")) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			return prop;
		} catch (IOException ex) {
			System.out.println("Unable to load config file");
			fail(ex.getMessage());
			return null;
		}
	}
	
	@Test
	void testLocal() {
		test();
	}
	
	public static void test() {
		testNoServer();
		testIntegratedWithC2();
	}
	
	public static void flushC2Emails() {
		EmailHandler receiveHandler = new EmailHandler();
		try {
			receiveHandler.initialize(null, setup(), null, null);
		} catch (Exception ex) {
			fail(ex.getMessage());
		}
		
		SimpleEmail email = receiveHandler.getNextMessage();
		while(email != null) {
			email = receiveHandler.getNextMessage();
		}
	}
	
	static void testNoServer() {
		final String SUBJECT = "Test";
		final String BODY = "This is my message";
		// Ports 26 and 587 will connect, 465 seems to hang
		EmailHandler receiveHandler = new EmailHandler();
		EmailHandler xMitHandler = new EmailHandler();
		try {
			System.out.println("Initialize receiver");
			receiveHandler.initialize(null, setup(), null, null);
			System.out.println("Initialize xmitter");
			xMitHandler.initialize(null, setup(), null, null);
		} catch (Exception ex) {
			fail(ex.getMessage());
		}
		System.out.println("Sending email");
		xMitHandler.sendEmail(SUBJECT, BODY, setup().getProperty(Constants.DAEMON_EMAIL_USERNAME));
		Time.sleepWrapped(500);
		SimpleEmail msg = receiveHandler.getNextMessage();
		assertNotNull(msg);
		assertTrue(msg.body.startsWith(BODY));
		assertEquals(msg.subject, SUBJECT);
		assertEquals(msg.sender, setup().getProperty(Constants.DAEMON_EMAIL_USERNAME));
	}

	static void testIntegratedWithC2() {
		String TEST_IO = "This is some IO";
		try {
			initiateServer();
		} catch (Exception e2) {
			fail(e2.getMessage());
		}

		EmailHandler xMitHandler = new EmailHandler();
		try {
			System.out.println("Initialize xmitter");
			xMitHandler.initialize(null, setup(), null, null);
		} catch (Exception ex) {
			fail(ex.getMessage());
		}
		System.out.println("Sending email");
		try {
			xMitHandler.sendEmail(
					"HOSTNAME:" + InetAddress.getLocalHost().getHostName() + " USERNAME:HAXOR PROTOCOL:SMTP", TEST_IO,
					setup().getProperty(Constants.DAEMON_EMAIL_USERNAME));
		} catch (UnknownHostException e1) {
			fail(e1.getMessage());
		}

		// Wait for email to get where it needs to go
		Time.sleepWrapped(1500);

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
			RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, false, false);
			bw.write("this is a sample command" + System.lineSeparator());
			bw.flush();

			String response = br.readLine();
			assertEquals(response, TEST_IO);

		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}

		teardown();

	}
}
