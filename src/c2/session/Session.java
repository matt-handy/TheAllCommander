package c2.session;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Session {

	private Queue<String> commands = new ConcurrentLinkedDeque<String>();
	private Queue<String> returnString = new ConcurrentLinkedDeque<String>();
	public final int id;
	public final String uid;
	public final String hostname;
	public final String username;
	public final String protocol;
	
	public Session(int id, String hostname, String username, String protocol) {
		this.id = id;
		String uid = hostname+":"+username+":"+protocol;
		this.uid = uid;
		this.hostname = hostname;
		this.username = username;
		this.protocol = protocol;
	}
	
	public synchronized String pollCommand(){
		return commands.poll();
	}
	
	public synchronized void sendCommand(String command){
		commands.offer(command);
	}
	
	public synchronized String pollIO(){
		return returnString.poll();
	}
	
	public synchronized void sendIO(String io){
		returnString.offer(io);
	}
}
