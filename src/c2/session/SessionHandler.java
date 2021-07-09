package c2.session;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import c2.Constants;

public class SessionHandler implements Runnable {

	public static final String NEW_SESSION_BANNER = "New session: ";
	
	private IOManager ioManager;
	private Socket socket;
	private int sessionId;
	private String sessionName;
	private boolean stayAlive = true;
	private CommandMacroManager cmm;

	public SessionHandler(IOManager ioManager, Socket socket, int sessionId, String sessionName, CommandMacroManager cmm) {
		this.ioManager = ioManager;
		this.socket = socket;
		this.sessionId = sessionId;
		this.cmm = cmm;
		this.sessionName = sessionName;
	}
	
	public void stop() {
		stayAlive = false;
	}

	@Override
	public void run() {
		try {
			OutputStreamWriter bw = new OutputStreamWriter(new BufferedOutputStream(socket.getOutputStream()));
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			bw.write(NEW_SESSION_BANNER + sessionName + System.lineSeparator());
			bw.flush();
			
			while (!Thread.interrupted() && stayAlive) {
				if(br.ready()) {
					String command = br.readLine();
					if(Constants.DEBUG) {
						System.out.println("Received command for session: " + sessionId + ": " + command);
					}
					if(command.equals("quit_session")) {
						switchSessions(bw, br);
					}else if(command.equals("list_sessions")) {
						SessionInitiator.printAvailableSessions(bw, ioManager);
					}else if(command.startsWith("kill_session")) {
						String[] elements = command.split(" ");
						try {
							int candidateSession = Integer.parseInt(elements[1]);
							if(ioManager.getSession(candidateSession) != null) {
								ioManager.removeSession(candidateSession);
							}else {
								bw.write("Invalid session id." + System.lineSeparator());
							}
							
							if(candidateSession == sessionId) {
								switchSessions(bw, br);
							}
						}catch(NumberFormatException ex) {
							bw.write("Invalid session id." + System.lineSeparator());
						}
						bw.flush();
					}else if(!cmm.processCmd(command, sessionId, sessionName)) {
						ioManager.sendCommand(sessionId, command);
					}
				}
				
				String latestOutput = ioManager.pollIO(sessionId);
				if (latestOutput != null) {
					if(latestOutput.equals("qSession")){
						stayAlive = false;
						socket.close();
					}else{
						//if(!latestOutput.endsWith("\n") && !latestOutput.endsWith("\r")) {
						//	latestOutput += System.lineSeparator();
						//}
						bw.write(latestOutput);
						bw.flush();
					}
				}
				
				try{
					Thread.sleep(50);
				}catch(Exception e){
					
				}
			}
		} catch (IOException e) {
			System.out.println("Session unavailable: " + sessionId);
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				System.out.println("Can't close socket");
			}
		}
	}

	public int getSessionId() {
		return sessionId;
	}

	private void switchSessions(OutputStreamWriter bw, BufferedReader br) throws IOException {
		SessionInitiator.printAvailableSessions(bw, ioManager);
		String input = br.readLine();
		int newSessionId = Integer.parseInt(input);
		
		String newSessionName = null;
		for (Session session : ioManager.getSessions()) {
			if(session.id == sessionId) {
				newSessionName = session.uid;
			}
		}
		if(newSessionName == null) {
			bw.write("Invalid Session id, will continue with prior session id." + System.lineSeparator());
		}else {
			bw.write(NEW_SESSION_BANNER + newSessionName + System.lineSeparator());
			sessionId = newSessionId;
			sessionName = newSessionName;
		}
		bw.flush();
	}
}
