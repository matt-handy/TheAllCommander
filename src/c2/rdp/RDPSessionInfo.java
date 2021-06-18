package c2.rdp;

import java.util.ArrayList;
import java.util.List;


public class RDPSessionInfo {
	public final String sessionId;
	public final int localForwardPort;
	public final int localClientIncomingPort;
	
	private List<String> activeErrors;
	
	public RDPSessionInfo(String sessionId, int localForwardPort, int localClientIncomingPort) {
		this.sessionId = sessionId;
		this.localForwardPort = localForwardPort;
		this.localClientIncomingPort = localClientIncomingPort;
	}
	
	public void addError(String activeError) {
		if(activeErrors == null) {
			activeErrors = new ArrayList<>();
		}
		activeErrors.add(activeError);
	}
	
	public boolean hasErrors() {
		return activeErrors != null;
	}
	
	public List<String> getErrors(){
		return activeErrors;
	}
}
