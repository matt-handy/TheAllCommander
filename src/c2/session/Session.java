package c2.session;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Session {

	private Queue<String> commands = new ConcurrentLinkedDeque<String>();
	private Queue<String> returnString = new ConcurrentLinkedDeque<String>();
	public final int id;
	public final String uid;
	
	public Session(int id, String uid) {
		this.id = id;
		this.uid = uid;
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
