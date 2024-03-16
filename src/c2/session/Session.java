package c2.session;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import c2.Constants;

public class Session implements Comparable<Session>{

	private Queue<String> commands = new ConcurrentLinkedDeque<String>();
	private Queue<String> returnString = new ConcurrentLinkedDeque<String>();
	private Map<String, Queue<String>> portForwardOutboundQueues = new HashMap<>();
	private Map<String, Queue<String>> portForwardInboundQueues = new HashMap<>();
	public final int id;
	public final String uid;
	public final String hostname;
	public final String username;
	public final String protocol;
	
	private String daemonUID = null;
	private Date lastContactTime = new Date();//Assumption is valid, session created on new contact.
	
	public Session(int id, String hostname, String username, String protocol) {
		this.id = id;
		String uid = hostname+":"+username+":"+protocol;
		this.uid = uid;
		this.hostname = hostname;
		this.username = username;
		this.protocol = protocol;
	}
	
	public Session(int id, String hostname, String username, String protocol, String daemonUID) {
		this.id = id;
		String uid = hostname+":"+username+":"+protocol+":"+daemonUID;
		this.uid = uid;
		this.hostname = hostname;
		this.username = username;
		this.protocol = protocol;
		this.daemonUID = daemonUID;
	}
	
	public void updateSessionContactTime() {
		lastContactTime = new Date();
	}
	
	public boolean isContactLate() {
		Date currentTime = new Date();
		long diffInMillis = currentTime.getTime() - lastContactTime.getTime();
		Constants theOnlyOne = Constants.getConstants();
		if(diffInMillis > theOnlyOne.getExpectedMaxClientReportingInterval() * theOnlyOne.getMultiplesOfExpectedMaxClientReportingToWait()) {
			return true;
		}else {
			return false;
		}
	}
	
	public void updateDaemonUID(String daemonUID) {
		this.daemonUID = daemonUID;
	}
	
	public String getDaemonUID() {
		return daemonUID;
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
	
	public synchronized void forwardTCPTraffic(String forwardUrl, String base64Forward) {
		synchronized(portForwardOutboundQueues){
			
		
		if(!portForwardOutboundQueues.containsKey(forwardUrl)) {
			portForwardOutboundQueues.put(forwardUrl, new ConcurrentLinkedDeque<>());
		}
		portForwardOutboundQueues.get(forwardUrl).add(base64Forward);
		}
	}
	
	public synchronized String grabForwardedTCPTraffic(String forwardUrl) {
		synchronized(portForwardOutboundQueues){
		if(!portForwardOutboundQueues.containsKey(forwardUrl)) {
			return null;
		}else {
			return portForwardOutboundQueues.get(forwardUrl).poll();
		}
		}
	}
	
	public synchronized String receiveForwardedTCPTraffic(String forwardUrl) {
		synchronized(portForwardInboundQueues){
		if(!portForwardInboundQueues.containsKey(forwardUrl)) {
			return null;
		}else {
			return portForwardInboundQueues.get(forwardUrl).poll();
		}
		}
	}
	
	public synchronized void queueForwardedTCPTraffic(String forwardUrl, String base64Forward) {
		synchronized(portForwardInboundQueues){
		if(!portForwardInboundQueues.containsKey(forwardUrl)) {
			portForwardInboundQueues.put(forwardUrl, new ConcurrentLinkedDeque<>());
		}
		portForwardInboundQueues.get(forwardUrl).add(base64Forward);
		}
	}
	
	public Set<String> availableForwards(){
		return portForwardOutboundQueues.keySet();
	}

	@Override
	public int compareTo(Session o) {
		Integer myId = id;
		Integer otherId = ((Session) o).id;
		return myId.compareTo(otherId);
	}
	
	
}
