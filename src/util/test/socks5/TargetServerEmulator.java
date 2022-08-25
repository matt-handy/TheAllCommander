package util.test.socks5;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import c2.portforward.socks.SocksHandler;

public class TargetServerEmulator implements Runnable, TargetEmulator {
	private boolean alive = true;
	private int listenPort;
	private boolean testBreak;
	
	private boolean passedConnectionMark = false;

	public TargetServerEmulator(int listenPort, boolean testBreak) {
		this.listenPort = listenPort;
		this.testBreak = testBreak;
	}

	private CountDownLatch cdl = new CountDownLatch(1);
	private CountDownLatch startLatch = new CountDownLatch(1);
	private CountDownLatch socketCloseLatch = new CountDownLatch(1);

	public void awaitStartup() {
		try {
			cdl.await();
		} catch (InterruptedException e) {
			// Ignore
		}
	}

	public boolean hasConnection() {
		return passedConnectionMark;
	}
	
	@Override
	public void awaitSocketClose() {
		try {
			socketCloseLatch.await();
		}catch(InterruptedException ex) {
			
		}
		
	}

	public void run() {
		try (ServerSocket ss = new ServerSocket(listenPort)) {

			ss.setSoTimeout(SocksHandler.MAIN_SOCKET_TIMEOUT);

			cdl.countDown();

			while (alive) {
				try {
					Socket socket = ss.accept();
					startLatch.countDown();
					passedConnectionMark = true;
					byte[] buffer = new byte[4096];
					//System.out.println("Reading initial buffer");
					int len = socket.getInputStream().read(buffer);
					byte[] stringLimited = Arrays.copyOf(buffer, len);
					//System.out.println("Read initial buffer: " + len);
					String testMessage = new String(stringLimited, StandardCharsets.UTF_8);
					//System.out.println("Testing string:" + testMessage + ":");
					assertEquals(SocksClientEmulator.TEST_OUTGOING_MESSAGE, testMessage);
					if (testBreak) {
						socket.close();
						socketCloseLatch.countDown();
					} else {
						//System.out.println("Writing response");
						socket.getOutputStream().write(SocksClientEmulator.TEST_INCOMING_MESSAGE.getBytes(StandardCharsets.UTF_8));
						socket.getOutputStream().flush();
						//System.out.println("Written response");
						socket.close();
					}
					alive = false;
				} catch (SocketTimeoutException ex) {
					//System.out.println("Timeout, resetting");
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
