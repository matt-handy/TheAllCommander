package c2.portforward;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Base64;
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
			if (needNewConnection) {
				if (acceptNewConnection()) {
					needNewConnection = false;
					if (outwardLooper == null) {
						outwardLooper = new OutwardLooper();
						service.submit(outwardLooper);
					}
					if (returnlooper == null) {
						returnlooper = new ReturnLooper();
						System.out.println("Submitting looper");
						service.submit(returnlooper);
					}
				}
			} else {
				Time.sleepWrapped(100);
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
		public void run() {
			System.out.println("Running forward");
			InputStream inputStream = null;
			while (stayAlive) {

				try {
					if (inputStream == null) {
						inputStream = newSession.getInputStream();
					}
					byte[] reply = new byte[100000];
					int bytesRead;
					if (-1 != (bytesRead = inputStream.read(reply))) {
						String base64Forward = Base64.getEncoder().encodeToString(Arrays.copyOf(reply, bytesRead));
						System.out.println("Forwarding to " + remoteForwardAddr + " at " + sessionId);
						io.forwardTCPTraffic(sessionId, remoteForwardAddr, base64Forward);
					}
					Time.sleepWrapped(1);
				} catch (IOException ex) {
					needNewConnection = true;
					inputStream = null;
				}
			}

		}
	}

	private class ReturnLooper implements Runnable {
		public void run() {
			OutputStream outputStream = null;

			System.out.println("Running Return");
			while (stayAlive) {
				try {
					if (outputStream == null) {
						outputStream = newSession.getOutputStream();
					}
					
					String response = io.receiveForwardedTCPTraffic(sessionId, remoteForwardAddr);
					if (response != null) {
						System.out.println("Received: " + sessionId + " " + remoteForwardAddr);
						byte[] traffic = Base64.getDecoder().decode(response);
						System.out.println("Writing");
						outputStream.write(traffic);
						outputStream.flush();
						System.out.println("Written");
					}
					Time.sleepWrapped(10);
				} catch (IOException ex) {
					//Why no signal that the socket is bad? Only need to signal that once.
					outputStream = null;
				}
			}

		}
	}

}
