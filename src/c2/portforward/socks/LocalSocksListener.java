package c2.portforward.socks;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import c2.session.IOManager;

public class LocalSocksListener implements Runnable {

	private final int port;
	private final int c2SessionId;

	private AtomicBoolean stayAlive = new AtomicBoolean(true);
	private CountDownLatch deathLatch = new CountDownLatch(1);
	
	public int nextProxyId = 1;
	
	private boolean localSocks;
	
	private List<SocksHandler> handlers = new ArrayList<>();
	private ExecutorService threadPool = Executors.newCachedThreadPool();
	
	private final IOManager io;
	
	public LocalSocksListener(IOManager io, int port, int c2SessionId, boolean localSocks) {
		this.port = port;
		this.c2SessionId = c2SessionId;
		this.localSocks = localSocks;
		this.io = io;
	}
	
	private CountDownLatch startLatch = new CountDownLatch(1);

	public void awaitStartup() {
		try {
			startLatch.await();
		} catch (InterruptedException e) {
			// Ignore
		}
	}
	
	@Override
	public void run() {
		try (ServerSocket socket = new ServerSocket(port)) {
			socket.setSoTimeout(SocksHandler.MAIN_SOCKET_TIMEOUT);
			startLatch.countDown();
			while (stayAlive.get()) {
				try {
					Socket incoming = socket.accept();
					SocksHandler handler = new SocksHandler(incoming, c2SessionId, nextProxyId, localSocks, io);
					nextProxyId++;
					handlers.add(handler);
					threadPool.execute(handler);
					purgeChildThreads();
				} catch (SocketTimeoutException ex) {
					// Continue
				}
			}

		} catch (IOException ex) {

		}
		deathLatch.countDown();
	}

	private void purgeChildThreads() {
		List<SocksHandler> deadHandlers = new ArrayList<>();
		for(SocksHandler handler : handlers) {
			if(!handler.isAlive()) {
				deadHandlers.add(handler);
			}
		}
		handlers.removeAll(deadHandlers);
	}
	
	public void kill() {
		stayAlive.set(false);
		for(SocksHandler handler : handlers) {
			handler.stop();
		}
		try {
			deathLatch.await();
		} catch (InterruptedException e) {
			// Discard, move on
		}
	}
}
