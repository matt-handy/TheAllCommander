package c2.session;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import c2.Constants;
import util.Time;

public class IOManager {

	public static final SimpleDateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
	
	private Map<Integer, Session> sessions = new HashMap<>();
	private int nextSessionId = 2;
	
	private Map<Session, File> logs = new HashMap<>();
	private Path logRoot;
	
	private CommandLoader cl;
	
	private void writeReceivedIO(String io, Session session) throws Exception {
		String message = "Received IO: '" + io + "'";
		writeLog(message, session);
	}
	
	private void writeSendCommand(String cmd, Session session) throws Exception{
		String message = "Send command: '" + cmd + "'";
		writeLog(message, session);
	}
	
	private void writeLog(String message, Session session) throws Exception {
		File logFile = logs.get(session);
		if(logFile == null) {
			Path logFilePath = logRoot.resolve(session.uid.replace(":", "")); 
			if(Files.exists(logFilePath)) {
				logFile = logFilePath.toFile();
			}else {
				logFile = Files.createFile(logFilePath).toFile();
			}
		}
		
		FileWriter fw = new FileWriter(logFile,true);
    	BufferedWriter bw = new BufferedWriter(fw);
    	bw.write(ISO8601.format(new Date()));
    	bw.write(" ");
    	bw.write(message);
    	bw.write(System.lineSeparator());
    	bw.close();
	}
	
	public IOManager(Path logRoot, CommandLoader cl) {
		if(Files.notExists(logRoot)) {
			try {
				Files.createDirectories(logRoot);
			} catch (IOException e) {
				System.out.println("Unable to create logging root");
				e.printStackTrace();
			}
		}
		sessions.put(1, new Session(1, "default"));
		this.logRoot = logRoot;
		this.cl = cl;
	}
	
	public synchronized String pollCommand(int sessionId){
		if(sessions.containsKey(sessionId)){
			String command = sessions.get(sessionId).pollCommand(); 
			return command;
		}else{
			throw new IllegalArgumentException("Invalid session id");
		}
	}
	
	public synchronized void sendCommand(int sessionId, String command){
		if(sessions.containsKey(sessionId)){
			Session session = sessions.get(sessionId);
			session.sendCommand(command);
			try {
				writeSendCommand(command, session);
			} catch (Exception e) {
				System.out.println("Can't write to log file");
				e.printStackTrace();
			}
		}else{
			throw new IllegalArgumentException("Invalid session id");
		}
	}
	
	public synchronized String pollIO(int sessionId){
		if(sessions.containsKey(sessionId)){
			return sessions.get(sessionId).pollIO();
		}else{
			throw new IllegalArgumentException("Invalid session id");
		}
	}
	
	public synchronized void sendIO(int sessionId, String command){
		if(sessions.containsKey(sessionId)){
			Session session = sessions.get(sessionId);
			session.sendIO(command);
			try {
				writeReceivedIO(command, session);
			} catch (Exception e) {
				System.out.println("Can't write to log file");
				e.printStackTrace();
			}
		}else{
			throw new IllegalArgumentException("Invalid session id");
		}
	}
	
	public synchronized Set<Session> getSessions(){
		return new HashSet<Session>(sessions.values());
	}
	
	//Available sessions are added by HTTPS Listener
	public synchronized int addSession(String uid, String username, String hostname){
		for(Session session : sessions.values()) {
			if(session.uid.contentEquals(uid)) {
				throw new IllegalArgumentException("Session Id Already Exists");
			}
		}
		int newSessionId = nextSessionId++;
		sessions.put(newSessionId, new Session(newSessionId, uid));
		
		if(!cl.getDefaultCommands().isEmpty()) {
			for(String cmd : cl.getDefaultCommands()) {
				System.out.println("Add default cmd: " + cmd);
				sendCommand(newSessionId, cmd);
			}
		}
		
		List<String> userCmds = cl.getUserCommands(username);
		if(userCmds != null) {
			for(String cmd : userCmds) {
				System.out.println("Adding user cmd: " + cmd);
				sendCommand(newSessionId, cmd);
			}
		}
		
		List<String> hostCmds = cl.getHostCommands(hostname);
		if(hostCmds != null) {
			for(String cmd : hostCmds) {
				System.out.println("Adding host cmd: " + cmd);
				sendCommand(newSessionId, cmd);
			}
		}
		
		return newSessionId;
	}
	
	public Integer getSessionId(String uid) {
		for(Session session : sessions.values()) {
			if(session.uid.contentEquals(uid)) {
				return session.id;
			}
		}
		return null;
	}
	
	public Session getSession(int id) {
		return sessions.get(id);
	}
	
	public synchronized void removeSession(int sessionId){
		sessions.remove(sessionId);
	}
	
	public String readAllMultilineCommands(int sessionId) {
		String nextIo = pollIO(sessionId);
		StringBuilder sb = new StringBuilder();
		while(nextIo != null) {
			sb.append(nextIo);
			sb.append(System.lineSeparator());
			nextIo = pollIO(sessionId);
		}
		return sb.toString();
	}
	
	public String awaitMultilineCommands(int sessionId) {
		String nextIo = readAllMultilineCommands(sessionId);
		int counter = 0;
		while(nextIo.length() == 0 && counter < Constants.getConstants().getMaxResponseWait()) {
			nextIo = readAllMultilineCommands(sessionId);
			counter += Constants.getConstants().getRepollForResponseInterval();
			Time.sleepWrapped(Constants.getConstants().getRepollForResponseInterval());
		}
		return nextIo;
	}
	
}
