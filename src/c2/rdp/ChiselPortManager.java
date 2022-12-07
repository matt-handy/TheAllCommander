package c2.rdp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 * This class is used to manage port allocations for WindowsRDPManager. On
 * startup it will load the current port allocations from a file, and one each
 * new session, or removed session, it will save the configuration file.
 * 
 * The file has the format:
 * Client Session ID: String based client session ID
 * Server forward port: integer //This is the locally bound port for the RDP client
 * Server incoming port: integer //This is the open port to which the client will connect
 *
 */
public final class ChiselPortManager {

	public static final String CLIENT_SESSION_ID = "Client Session ID: ";
	public static final String SERVER_FORWARD_PORT = "Server forward port: ";
	public static final String SERVER_INCOMING_PORT = "Server incoming port: ";
	
	public final Path file;
	
	private Set<RDPSessionInfo> info = new HashSet<>();
	
	public static ChiselPortManager loadFromConfig(Path file) throws Exception {
		if(!Files.exists(file) || Files.isDirectory(file)) {
			throw new Exception("Give me a real file path to load chisel configs");
		}
		
		Set<RDPSessionInfo> info = new HashSet<>();
		
		//This code is a little brittle and assumes that the data is loaded in order
		List<String> lines = Files.readAllLines(file);
		String clientId = null;
		int forwardPort = -1;
		int incomingPort = -1;
		for(String line : lines) {
			if(line.startsWith(CLIENT_SESSION_ID)) {
				clientId = line.substring(CLIENT_SESSION_ID.length());
			}else if(line.startsWith(SERVER_FORWARD_PORT)) {
				try {
					forwardPort = Integer.parseInt(line.substring(SERVER_FORWARD_PORT.length()));
				}catch(NumberFormatException ex) {
					forwardPort = -1;
				}
			}else if(line.startsWith(SERVER_INCOMING_PORT)) {
				try {
					incomingPort = Integer.parseInt(line.substring(SERVER_INCOMING_PORT.length()));
				}catch(NumberFormatException ex) {
					incomingPort = -1;
				}
				
				if(incomingPort != -1 &&
						forwardPort != -1 &&
						clientId != null) {
					info.add(new RDPSessionInfo(clientId, forwardPort, incomingPort));
				}else {
					//Something went wrong and we discard that config
				}
			}
		}
		
		return new ChiselPortManager(file, info);
	}
	
	
	private void persist() {
		StringBuilder sb = new StringBuilder();
		
		for(RDPSessionInfo session : info) {
			sb.append(CLIENT_SESSION_ID);
			sb.append(session.sessionId);
			sb.append(System.lineSeparator());
			
			sb.append(SERVER_FORWARD_PORT);
			sb.append(session.localForwardPort);
			sb.append(System.lineSeparator());
			
			sb.append(SERVER_INCOMING_PORT);
			sb.append(session.localClientIncomingPort);
			sb.append(System.lineSeparator());
		}
		try {
			Files.write(file, sb.toString().getBytes());
		} catch (IOException e) {
			//Path has been validated on construction of the file
			//Sure, someone could be doing a simultaneous access, but meh, we're
			//not as worried about that.
		}
	}
	
	public void addRDPSessionInfo(RDPSessionInfo info) {
		this.info.add(info);
		persist();
	}
	
	public void removeRDPSessionInfo(String session) {
		RDPSessionInfo target = null;
		for(RDPSessionInfo candidate : info) {
			if(candidate.sessionId.equals(session)) {
				target = candidate;
			}
		}
		info.remove(target);
		persist();
	}
	
	public Set<RDPSessionInfo> getInfo() {
		return new HashSet<>(info);
	}

	private ChiselPortManager(Path path, Set<RDPSessionInfo> info) {
		this.file = path;
		this.info = info;
	}
}
