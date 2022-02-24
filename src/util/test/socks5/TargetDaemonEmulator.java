package util.test.socks5;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;

import c2.session.IOManager;

public class TargetDaemonEmulator implements Runnable, TargetEmulator {

	private IOManager io;
	private int sessionId;
	private int proxyId;
	private boolean sendIp;
	private boolean testBreak;
	private boolean socksEstablished = false;
	
	private CountDownLatch socketCloseLatch = new CountDownLatch(1);

	public TargetDaemonEmulator(IOManager io, int sessionId, int proxyId, boolean sendIp, boolean testBreak) {
		this.io = io;
		this.sessionId = sessionId;
		this.proxyId = proxyId;
		this.sendIp = sendIp;
		this.testBreak = testBreak;
	}

	public void run() {
		String proxyStartCmd = null;
		while (proxyStartCmd == null) {
			proxyStartCmd = io.pollCommand(sessionId);
			Thread.yield();
		}
		// System.out.println("Have command from io:" + proxyStartCmd + ":");
		if (sendIp) {
			assertEquals("startSocks proxyID:" + proxyId + " /127.0.0.1:4096", proxyStartCmd);
		} else {
			assertEquals("startSocks proxyID:" + proxyId + " localhost:4096", proxyStartCmd);
		}
		// System.out.println("sending ack");
		io.sendIO(sessionId, "socksEstablished" + System.lineSeparator());

		socksEstablished = true;
		
		String proxyOutgoing = io.grabForwardedTCPTraffic(sessionId, "socksproxy:" + proxyId);
		while (proxyOutgoing == null) {
			proxyOutgoing = io.grabForwardedTCPTraffic(sessionId, "socksproxy:" + proxyId);
			Thread.yield();
		}
		// System.out.println(proxyOutgoing);
		byte[] messageBytes = Base64.getDecoder().decode(proxyOutgoing);
		String outgoingMessage = new String(messageBytes, StandardCharsets.UTF_8);
		// System.out.println(":" + outgoingMessage + ":");
		assertEquals(SocksClientEmulator.TEST_OUTGOING_MESSAGE, outgoingMessage);
		if (testBreak) {
			// System.out.println("Daemon emulator send terminate");
			io.queueForwardedTCPTraffic(sessionId, "socksproxy:" + proxyId,
					Base64.getEncoder().encodeToString("socksterminatedatdaemon".getBytes()));
			socketCloseLatch.countDown();
		} else {
			// System.out.println("Daemon emulator send msg");
			io.queueForwardedTCPTraffic(sessionId, "socksproxy:" + proxyId, Base64.getEncoder()
					.encodeToString(SocksClientEmulator.TEST_INCOMING_MESSAGE.getBytes(StandardCharsets.UTF_8)));
		}
		// System.out.println("Finished daemon emulator");
	}

	@Override
	public void awaitSocketClose() {
		try {
			socketCloseLatch.await();
		} catch (InterruptedException e) {
		}

	}
	
	@Override
	public boolean hasConnection() {
		return socksEstablished;
	}

}
