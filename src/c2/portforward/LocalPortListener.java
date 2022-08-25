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

	private CountDownLatch startLatch = new CountDownLatch(1);
	private CountDownLatch stopLatch = new CountDownLatch(1);
	
	public LocalPortListener(IOManager io, int sessionId, String remoteForwardAddr, int localListenPort) {
		this.io = io;
		this.sessionId = sessionId;
		this.remoteForwardAddr = remoteForwardAddr;
		this.localListenPort = localListenPort;
	}

	public void awaitStartup() {
		try {
			startLatch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
					service.submit(outwardLooper);
					service.submit(returnlooper);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}

		}
		
		stopLatch.countDown();
	}

	public void kill() {
		stayAlive = false;
		try {
			stopLatch.await();
		} catch (InterruptedException e) {
			
		}
	}

	public boolean acceptNewConnection() {
		try {
			ServerSocket ss = new ServerSocket(localListenPort);
			ss.setSoTimeout(1000);
			
			startLatch.countDown();

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
			try {
				while (stayAlive && !die) {

					byte[] reply = new byte[100000];
					int bytesRead;
					if (-1 != (bytesRead = inputStream.read(reply))) {
						String base64Forward = Base64.getEncoder().encodeToString(Arrays.copyOf(reply, bytesRead));
						//System.out.println("Local forwarding to " + remoteForwardAddr + " at " + sessionId + " : " + base64Forward);
						io.forwardTCPTraffic(sessionId, remoteForwardAddr, base64Forward);
						//System.out.println("Local forwarded to " + remoteForwardAddr + " at " + sessionId + " : " + base64Forward);
					}
					Time.sleepWrapped(50);

				}
			} catch (IOException ex) {
				needNewConnection = true;
				inputStream = null;
			}
			deathLatch.countDown();
		}

		public void kill() {
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
			

			try {
				while (stayAlive && !die) {

					//System.out.println("Checking: " + sessionId + " " + remoteForwardAddr);
					String response = io.receiveForwardedTCPTraffic(sessionId, remoteForwardAddr);
					//System.out.println("Returning: " + sessionId + " " + remoteForwardAddr);
					if (response != null) {
						byte[] traffic = Base64.getDecoder().decode(response);
						//System.out.println("Traffic");
						outputStream.write(traffic);
						outputStream.flush();
					}
					Time.sleepWrapped(100);

				}
			} catch (IOException ex) {
				// Why no signal that the socket is bad? Only need to signal that once.
				ex.printStackTrace();
			}
			deathLatch.countDown();
		}

		public void kill() {
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
