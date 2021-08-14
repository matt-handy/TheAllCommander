package c2.session;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;

public class SessionInitiator implements Runnable {

	public static final String AVAILABLE_SESSION_BANNER = "Available sessions:";
	
	private SessionManager sessionManager;
	private IOManager ioManager;
	private int port;
	private boolean stayAlive = true;
	private CountDownLatch stopLatch = new CountDownLatch(1);
	private CommandMacroManager cmm;
	
	public SessionInitiator(SessionManager sessionManager, IOManager ioManager, int port, CommandMacroManager cmm) {
		this.sessionManager = sessionManager;
		this.ioManager = ioManager;
		this.port = port;
		this.cmm = cmm;
	}
	
	public void stop() {
		stayAlive = false;
		try {
			stopLatch.await();
		} catch (InterruptedException e) {
			//Continue
		}
	}

	@Override
	public void run() {
		try {
			ServerSocket ss = new ServerSocket(port);
			ss.setSoTimeout(1000);

			while (!Thread.interrupted() && stayAlive) {
				Socket newSession = null;
				try {
					newSession = ss.accept();
				}catch(SocketTimeoutException ex) {
					continue;//No worries 
				}
				
				try {
					OutputStreamWriter bw = new OutputStreamWriter(
							new BufferedOutputStream(newSession.getOutputStream()));
					BufferedReader br = new BufferedReader(new InputStreamReader(newSession.getInputStream()));

					printAvailableSessions(bw, ioManager);

					int count = 0;
					while (count < 10 && !(newSession.getInputStream().available() > 1)) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// continue
						}
						count++;
					}
					
					if (br.ready()) {
						System.out.println("Reading");
						String input = br.readLine();
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
								sessionManager.addSession(new SessionHandler(ioManager, newSession, sessionId, sessionName, cmm));
							} else {
								bw.write("Invalid Session id, bye-bye!");
								newSession.close();
							}
							
						} catch (NumberFormatException e) {
							bw.write(input + " is not a number.");
							newSession.close();
						}
					}else {
						System.out.println("This guy isn't talking to us");
						newSession.close();
					}

				} catch (IOException e) {
					e.printStackTrace();
					newSession.close();
				}
			}
			ss.close();
			stopLatch.countDown();
		} catch (IOException e) {
			System.out.println("Can't lock session");
		}
	}
	
	public static void printAvailableSessions(OutputStreamWriter bw, IOManager io) throws IOException {
		bw.write(AVAILABLE_SESSION_BANNER);
		bw.write(System.lineSeparator());
		for (Session session : io.getSessions()) {
			bw.write(session.id + ":" + session.uid);
			bw.write(System.lineSeparator());
		}
		bw.flush();
	}

}
