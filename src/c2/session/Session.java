package c2.session;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Session {

	private Queue<String> commands = new ConcurrentLinkedDeque<String>();
	private Queue<String> returnString = new ConcurrentLinkedDeque<String>();
	private Map<String, Queue<String>> portForwardOutboundQueues = new HashMap<>();
	private Map<String, Queue<String>> portForwardInboundQueues = new HashMap<>();
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
	
	public String pollCommand(){
		return commands.poll();
	}
	
	public void sendCommand(String command){
		commands.offer(command);
	}
	
	public String pollIO(){
		return returnString.poll();
	}
	
	public void sendIO(String io){
		returnString.offer(io);
	}
	
	public void forwardTCPTraffic(String forwardUrl, String base64Forward) {
		if(!portForwardOutboundQueues.containsKey(forwardUrl)) {
			portForwardOutboundQueues.put(forwardUrl, new ConcurrentLinkedDeque<>());
		}
		portForwardOutboundQueues.get(forwardUrl).add(base64Forward);
	}
	
	public String grabForwardedTCPTraffic(String forwardUrl) {
		if(!portForwardOutboundQueues.containsKey(forwardUrl)) {
			return null;
		}else {
			return portForwardOutboundQueues.get(forwardUrl).poll();
		}
	}
	
	public String receiveForwardedTCPTraffic(String forwardUrl) {
		if(!portForwardInboundQueues.containsKey(forwardUrl)) {
			return null;
		}else {
			return portForwardInboundQueues.get(forwardUrl).poll();
		}
	}
	
	public void queueForwardedTCPTraffic(String forwardUrl, String base64Forward) {
		if(!portForwardInboundQueues.containsKey(forwardUrl)) {
			portForwardInboundQueues.put(forwardUrl, new ConcurrentLinkedDeque<>());
		}
		portForwardInboundQueues.get(forwardUrl).add(base64Forward);
	}
	
	public Set<String> availableForwards(){
		return portForwardOutboundQueues.keySet();
	}
}
