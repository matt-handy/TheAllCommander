package c2.tcp.filereceiver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileReceiverSessionReceiver implements Runnable {

	private boolean stayAlive = true;
	private CountDownLatch deathLatch = new CountDownLatch(1);
	private int listenPort;
	private Path basePath;

	private ExecutorService threadPool = Executors.newCachedThreadPool();
	private Set<FileReceiverSessionHandler> handlers = new HashSet<>();

	public FileReceiverSessionReceiver(int listenPort, Path basePath) {
		this.listenPort = listenPort;
		this.basePath = basePath;
	}

	private boolean startedListening;
	private CountDownLatch startLatch = new CountDownLatch(1);
	
	private ServerSocket ss;
	
	public boolean awaitOnline() {
		try {
			startLatch.await();
		} catch (InterruptedException e) {
			// Carry on
		}
		return startedListening;
	}
	
	@Override
	public void run() {
		try {
			ss = new ServerSocket(listenPort);
			ss.setSoTimeout(2000);

			startedListening = true;
			startLatch.countDown();
			
			while (stayAlive) {
				try {
					Socket newSession = ss.accept();
					newSession.setSoTimeout(2000);
					FileReceiverSessionHandler handler = new FileReceiverSessionHandler(basePath, newSession);
					threadPool.submit(handler);
					handlers.add(handler);
				} catch (SocketTimeoutException e) {
					// Ignore, ahead full
				} catch(IOException ex) {
					//Socket closed, ahead full
					ex.printStackTrace();
				}
			}

			deathLatch.countDown();
		} catch (IOException ex) {
			ex.printStackTrace();
			startedListening = false;
			startLatch.countDown();
		}

		try {
			ss.close();
		}catch(Exception ex) {
			
		}
		deathLatch.countDown();
	}

	public void kill() {
		stayAlive = false;
		
		try {
			deathLatch.await();
		} catch (InterruptedException e) {
			// Ignore
		}
		for (FileReceiverSessionHandler handler : handlers) {
			handler.kill();
		}
	}
}
