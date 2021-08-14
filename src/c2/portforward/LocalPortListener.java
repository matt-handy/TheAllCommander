package c2.portforward;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import c2.session.IOManager;
import util.Time;

public class LocalPortListener implements Runnable {

	private IOManager io;
	public final int sessionId;
	public final String remoteForwardAddr;
	public final int localListenPort;
	private boolean stayAlive = true;
	private Socket newSession;
	private boolean needNewConnection = true;

	public LocalPortListener(IOManager io, int sessionId, String remoteForwardAddr, int localListenPort) {
		this.io = io;
		this.sessionId = sessionId;
		this.remoteForwardAddr = remoteForwardAddr;
		this.localListenPort = localListenPort;
	}

	@Override
	public void run() {
		ExecutorService service = Executors.newCachedThreadPool();
		ReturnLooper returnlooper = null;
		OutwardLooper outwardLooper = null;
		while (stayAlive) {
			// We're going to keep listening for an incoming connection, and will allow a
			// new connection
			// to supersede prior ones
			if (acceptNewConnection()) {
				needNewConnection = false;
				if (outwardLooper != null) {
					outwardLooper.kill();

				}
				
				if (returnlooper != null) {
					returnlooper.kill();
				}
				try {
					outwardLooper = new OutwardLooper(newSession.getInputStream());
					returnlooper = new ReturnLooper(newSession.getOutputStream());
					System.out.println("Submitting looper");
					service.submit(outwardLooper);
					service.submit(returnlooper);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}

		}
	}

	public void kill() {
		stayAlive = false;
	}

	public boolean acceptNewConnection() {
		try {
			System.out.println("Listening: " + localListenPort);
			ServerSocket ss = new ServerSocket(localListenPort);
			ss.setSoTimeout(1000);

			while (!Thread.interrupted() && stayAlive) {
				newSession = null;
				try {
					newSession = ss.accept();
				} catch (SocketTimeoutException ex) {
					continue;// No worries
				}
				ss.close();
				return true;
			}
			ss.close();
			return false;
		} catch (IOException ex) {
			return false;
		}
	}

	private class OutwardLooper implements Runnable {
		private boolean die = false;
		private CountDownLatch deathLatch = new CountDownLatch(1);
		private InputStream inputStream;
		
		public OutwardLooper(InputStream inputStream) {
			this.inputStream = inputStream;
		}
		
		public void run() {
			System.out.println("Running forward");
			try {
				while (stayAlive && !die) {

					byte[] reply = new byte[100000];
					int bytesRead;
					if (-1 != (bytesRead = inputStream.read(reply))) {
						String base64Forward = Base64.getEncoder().encodeToString(Arrays.copyOf(reply, bytesRead));
						System.out.println("Local forwarding to " + remoteForwardAddr + " at " + sessionId + " : " + base64Forward);
						io.forwardTCPTraffic(sessionId, remoteForwardAddr, base64Forward);
						System.out.println("Local forwarded to " + remoteForwardAddr + " at " + sessionId + " : " + base64Forward);
					}
					Time.sleepWrapped(1);

				}
			} catch (IOException ex) {
				System.out.println("Exception!!!");
				needNewConnection = true;
				inputStream = null;
			}
			deathLatch.countDown();
			System.out.println("Forward loop closing");

		}

		public void kill() {
			System.out.println("Forward looper die");
			die = true;
			try {
				deathLatch.await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private class ReturnLooper implements Runnable {
		private boolean die = false;
		private OutputStream outputStream;
		private CountDownLatch deathLatch = new CountDownLatch(1);
		
		public ReturnLooper(OutputStream outputStream) {
			this.outputStream = outputStream;
		}

		public void run() {
			

			System.out.println("Running Return");
			try {
				while (stayAlive && !die) {

					//System.out.println("Checking: " + sessionId + " " + remoteForwardAddr);
					String response = io.receiveForwardedTCPTraffic(sessionId, remoteForwardAddr);
					//System.out.println("Returning: " + sessionId + " " + remoteForwardAddr);
					if (response != null) {
						System.out.println("Received: " + sessionId + " " + remoteForwardAddr);
						System.out.println(response);
						System.out.println("Received: " + sessionId + " " + remoteForwardAddr + " : " + response);
						byte[] traffic = Base64.getDecoder().decode(response);
						System.out.println("Writing");
						outputStream.write(traffic);
						outputStream.flush();
						System.out.println("Written");
					}
					Time.sleepWrapped(100);

				}
			} catch (IOException ex) {
				// Why no signal that the socket is bad? Only need to signal that once.
				ex.printStackTrace();
			}
			deathLatch.countDown();
			System.out.println("Return loop closing");
		}

		public void kill() {
			System.out.println("Return looper die");
			die = true;
			try {
				deathLatch.await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
