package util.test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import c2.Constants;
import util.Time;

public class KeyLoggerTest {
	public static void testGenericKeylogger(Properties prop, boolean testScreenshot) {
		try {
			Robot robot = new Robot();
			robot.setAutoDelay(40);
			robot.setAutoWaitForIdle(true);

			robot.delay(100);
			robot.keyPress(KeyEvent.VK_G);
			robot.keyRelease(KeyEvent.VK_G);

			// Thread.sleep(30);
			robot.delay(250);
			robot.keyPress(KeyEvent.VK_8);
			robot.keyRelease(KeyEvent.VK_8);

			// Thread.sleep(30);
			robot.delay(250);
			robot.keyPress(KeyEvent.VK_9);
			robot.keyRelease(KeyEvent.VK_9);

			// Thread.sleep(30);
			robot.delay(250);
			robot.keyPress(KeyEvent.VK_H);
			robot.keyRelease(KeyEvent.VK_H);
		} catch (AWTException e) {
			fail(e.getMessage());
		}

		try {
			Thread.sleep(65000);
		} catch (InterruptedException e) {
			// Ignore, continue
		}

		try {
			FileReader fr = new FileReader(prop.getProperty(Constants.DAEMONLZLOGGER) + File.separator
					+ InetAddress.getLocalHost().getHostName());
			BufferedReader br = new BufferedReader(fr);
			StringBuilder fileContent = new StringBuilder();
			String nextLine;
			while ((nextLine = br.readLine()) != null) {
				fileContent.append(nextLine);
				fileContent.append(System.lineSeparator());
			}
			String fileString = fileContent.toString();
			// Python keylogger not smart enough for windows
			// assertTrue(fileString.contains("Window:"));
			assertTrue(fileString.contains("g") || fileString.contains("G"));
			assertTrue(fileString.contains("8"));
			assertTrue(fileString.contains("9"));
			assertTrue(fileString.contains("h") || fileString.contains("H"));
			assertTrue((fileString.indexOf("g") < fileString.indexOf("8"))
					|| (fileString.indexOf("G") < fileString.indexOf("8")));
			assertTrue(fileString.indexOf("8") < fileString.indexOf("9"));
			br.close();
		} catch (FileNotFoundException e1) {
			fail("Log file was not created");
		} catch (UnknownHostException e1) {
			fail("Can't find system hostname");
		} catch (IOException e) {
			fail("Can't read file");
		}
		if (testScreenshot) {
			RunnerTestGeneric.testScreenshotsOnFS("python");
		}

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
