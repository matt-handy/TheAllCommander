package c2.session;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import c2.Constants;

public class SessionInitiator implements Runnable {

	public static final String AVAILABLE_SESSION_BANNER = "Available sessions:";
	public static final String WIZARD_BANNER = "Enter 'WIZARD' to begin other server commands";

	//TODO: Make configurable
	public static final Path CSHARP_CONFIG_PATH = Paths.get("config", "csharp_staging"); 
	
	private SessionManager sessionManager;
	private IOManager ioManager;
	protected Properties properties;
	protected int port;
	protected boolean stayAlive = true;
	private CountDownLatch stopLatch = new CountDownLatch(1);
	private CommandMacroManager cmm;
	
	private ExecutorService commandWizardManager = Executors.newCachedThreadPool();
	
	public SessionInitiator(SessionManager sessionManager, IOManager ioManager, int port, CommandMacroManager cmm, Properties properties) {
		this.sessionManager = sessionManager;
		this.ioManager = ioManager;
		this.port = port;
		this.cmm = cmm;
		this.properties = properties;
	}
	
	public void stop() {
		stayAlive = false;
		try {
			stopLatch.await();
		} catch (InterruptedException e) {
			//Continue
		}
	}

	protected void processNewSocket(Socket newSession) {
		try {
			newSession.setSoTimeout(10000);
			OutputStreamWriter bw = new OutputStreamWriter(
					new BufferedOutputStream(newSession.getOutputStream()));
			BufferedReader br = new BufferedReader(new InputStreamReader(newSession.getInputStream()));

			if(properties.get(Constants.COMMANDERUSERNAME) != null) {
				String user = properties.getProperty(Constants.COMMANDERUSERNAME);
				String pass = properties.getProperty(Constants.COMMANDERSECRET);
				bw.write("Username:" + System.lineSeparator());
				bw.flush();
				String line = br.readLine();
				if(!user.equals(line)) {
					bw.write("Invalid Username" + System.lineSeparator());
					bw.flush();
					newSession.close();
					return;
				}
				bw.write("Password:" + System.lineSeparator());
				bw.flush();
				line = br.readLine();
				if(!pass.equals(line)) {
					bw.write("Invalid Password" + System.lineSeparator());
					bw.flush();
					newSession.close();
					return;
				}
				bw.write("Access Granted" + System.lineSeparator());
				bw.flush();
			}

			printAvailableSessions(bw, ioManager);
			
			
				String input = br.readLine();
				if(input == null) {
					System.out.println("Connection reset");
					newSession.close();
					return;
				}
				try {
					// TODO: need to make sure that only one session is
					// locked
					// Do this by adding hook in SessionManager so that
					// dead SessionHandlers
					// release their sessions
					int sessionId = Integer.parseInt(input);
					
					String sessionName = null;
					for (Session session : ioManager.getSessions()) {
						if(session.id == sessionId) {
							sessionName = session.uid;
						}
					}
				
					if (sessionName != null) {
						newSession.setSoTimeout(1000);
						sessionManager.addSession(new SessionHandler(ioManager, newSession, sessionId, sessionName, cmm));
					} else {
						bw.write("Invalid Session id, bye-bye!");
						newSession.close();
					}
					
				} catch (NumberFormatException e) {
					if(input.equalsIgnoreCase("WIZARD")) {
						//We pass the reader to ensure the input stream is not reset
						CommandWizard wizard = new CommandWizard(newSession, CSHARP_CONFIG_PATH, br);
						commandWizardManager.submit(wizard);
					}else {
						bw.write(input + " is not a number.");
						newSession.close();
					}
				}
				
		}catch (SocketTimeoutException ex) {
			System.out.println("This guy isn't talking to us");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected ServerSocket getServerSocket() throws Exception {
		return new ServerSocket(port);	
	}
	
	@Override
	public void run() {
		try {
			ServerSocket ss = getServerSocket();
			ss.setSoTimeout(1000);

			while (!Thread.interrupted() && stayAlive) {
				Socket newSession = null;
				try {
					newSession = ss.accept();
				}catch(SocketTimeoutException ex) {
					continue;//No worries 
				}
				processNewSocket(newSession);
				 
			}
			ss.close();
			stopLatch.countDown();
		} catch (Exception e) {
			System.out.println("Can't lock session");
			e.printStackTrace();
		}
	}
	
	public static void printAvailableSessions(OutputStreamWriter bw, IOManager io) throws IOException {
		bw.write(AVAILABLE_SESSION_BANNER);
		bw.write(System.lineSeparator());
		List<Session> sessions = new ArrayList<>();
		sessions.addAll(io.getSessions());
		Collections.sort(sessions);
		for (Session session : sessions) {
			bw.write(session.id + ":" + session.uid);
			bw.write(System.lineSeparator());
		}
		bw.write(WIZARD_BANNER);
		bw.write(System.lineSeparator());
		bw.flush();
	}

}
