package util.test.socks5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import c2.portforward.socks.SocksHandler;
import util.Time;

public class SocksClientEmulator implements Runnable {

	public static String TEST_OUTGOING_MESSAGE = "This is a test of an outgoing message";
	public static String TEST_INCOMING_MESSAGE = "This is the message which should come in response";

	private boolean alive = true;
	private int localSocksPort;
	private int targetClientPort;
	private boolean sendIp;
	private TargetEmulator target;
	private boolean testBreak;// Tests that a connection was broken

	private boolean ranToConclusion = false;

	private boolean testEmailLatency = false;

	public SocksClientEmulator(int localSocksPort, boolean sendIp, TargetEmulator target, boolean testBreak, int targetClientPort) {
		this.localSocksPort = localSocksPort;
		this.sendIp = sendIp;
		this.target = target;
		this.testBreak = testBreak;
		this.targetClientPort = targetClientPort;
	}

	public SocksClientEmulator(int localSocksPort, boolean sendIp, TargetServerEmulator target, boolean testBreak,
			boolean testEmailLatency, int targetClientPort) {
		this.localSocksPort = localSocksPort;
		this.sendIp = sendIp;
		this.target = target;
		this.testBreak = testBreak;
		this.testEmailLatency = testEmailLatency;
		this.targetClientPort = targetClientPort;
	}

	private CountDownLatch cdl = new CountDownLatch(1);

	public boolean isComplete() {
		try {
			// System.out.println("Awaiting");
			cdl.await();
			// System.out.println("Await complete");
		} catch (InterruptedException e) {
			// Discard and move on
		}
		return ranToConclusion;
	}

	public void run() {
		while (alive) {
			try (Socket socket = new Socket("127.0.0.1", localSocksPort)) {
				socket.setSoTimeout(20000);

				OutputStream os = socket.getOutputStream();
				os.write(SocksHandler.SOCKS5_VER);// Open the transaction with the socks version
				byte[] bogusAuthenticationHeader = { 0x02, 0x01, 0x01 };
				os.write(bogusAuthenticationHeader);
				os.flush();

				InputStream is = socket.getInputStream();
				byte[] response = is.readNBytes(2);
				assertEquals(2, response.length);
				assertEquals(SocksHandler.ACCEPT_PROXY[0], response[0]);
				assertEquals(SocksHandler.ACCEPT_PROXY[1], response[1]);

				os.write(SocksHandler.SOCKS5_VER);// Open the command sequence with the socks version
				os.write(SocksHandler.SOCKS_CONNECT);// Send the command
				os.write(0x00);// Reserved bits
				if (sendIp) {
					os.write(1); // IPv4 address type
					os.write(127);// Order connection back to localhost
					os.write(0);
					os.write(0);
					os.write(1);
				} else {
					String localhost = new String("localhost");
					os.write(3);
					os.write(localhost.length());
					os.write(localhost.getBytes());
				}
				os.write((byte) ((targetClientPort & 0xFF00) >> 8));
				os.write((byte) (targetClientPort & 0xFF));
				//os.write(16);// Order connection to port 4096
				//os.write(0);
				os.flush();

				//System.out.println("Reading connect confirm code");
				response = is.readNBytes(10);
				//System.out.println("Connect good code: " + response.length);
				assertEquals(10, response.length);
				//System.out.println("Client Checking VER");
				assertEquals(SocksHandler.SOCKS5_VER, response[0]);
				//System.out.println("Client Checking Success");
				assertEquals(SocksHandler.getSuccessCode(), response[1]);
				assertEquals(0, response[2]);// Reserved
				//System.out.println("IPv4: " + response[3] );
				assertEquals(1, response[3]);// IPv4 connection
				//System.out.println("IPv4: " + response[4] );
				assertEquals(0, response[4]);// IPv4 byte
				//System.out.println("IPv4: " + response[5] );
				assertEquals(0, response[5]);// IPv4 byte
				//System.out.println("IPv4: " + response[6] );
				assertEquals(0, response[6]);// IPv4 byte
				//System.out.println("IPv4: " + response[7] );
				assertEquals(0, response[7]);// IPv4 byte
				//System.out.println("Port: " + response[8] );
				assertEquals((byte) ((localSocksPort & 0xFF00) >> 8), response[8]);// port
				//System.out.println("Port: " + response[9] );
				assertEquals((byte) (localSocksPort & 0x00FF), response[9]);// port
				//System.out.println("Client Checked Success");
				if (target != null) {
					//Sometimes we have to wait briefly for the target emulator to recognize it has a socket.
					//System.out.println("Client has connection? " + target.hasConnection());
					int counter = 0;
					while(!target.hasConnection() && counter < 100) {
						Time.sleepWrapped(10);
						counter++;
					}
					//System.out.println("Client has connection? " + target.hasConnection());
					assertTrue(target.hasConnection());
					//System.out.println("They do");
				}

				//System.out.println("Send Outgoing: " + TEST_OUTGOING_MESSAGE.getBytes(StandardCharsets.UTF_8).length);
				//System.out.println("Final byte: " + TEST_OUTGOING_MESSAGE.getBytes(StandardCharsets.UTF_8)[36]);
				os.write(TEST_OUTGOING_MESSAGE.getBytes(StandardCharsets.UTF_8));

				os.flush();
				//System.out.println("Flushed outgoing");
				if (testBreak) {
					if (target != null) {
						target.awaitSocketClose();
					}
					boolean foundException = false;
					if (testEmailLatency) {
						Time.sleepWrapped(30000);
						for (int idx = 0; idx < 20; idx++) {
							try {
								os.write(TEST_OUTGOING_MESSAGE.getBytes(StandardCharsets.UTF_8));
								os.flush();
								Time.sleepWrapped(30000);
							} catch (IOException ex) {
								foundException = true;
							}
						}
					} else {
						for (int idx = 0; idx < 100; idx++) {
							try {
								os.write(TEST_OUTGOING_MESSAGE.getBytes(StandardCharsets.UTF_8));
								os.flush();
								Time.sleepWrapped(50);
							} catch (IOException ex) {
								foundException = true;
							}
						}
					}
					//System.out.println("Did I get an exception: " + foundException);
					assertTrue(foundException);
				} else {
					byte[] buffer = new byte[4096];
					//System.out.println("Reading incoming message response");
					int len = is.read(buffer);
					//System.out.println("Read response: " + len);
					byte[] stringLimited = Arrays.copyOf(buffer, len);
					String testMsg = new String(stringLimited, StandardCharsets.UTF_8);
					//System.out.println("Read response:" + testMsg + ":");
					assertEquals(TEST_INCOMING_MESSAGE, testMsg);
				}
				alive = false;
			} catch (Exception ex) {
				ex.printStackTrace();
				fail();
			}
			ranToConclusion = true;
			cdl.countDown();
		}
	}

}
