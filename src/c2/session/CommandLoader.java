package c2.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandLoader {

	private Map<String, List<String>> userCommands;
	private Map<String, List<String>> hostCommands;
	private List<String> defaultCommands;
	private Map<SessionAttributeDescriptor, List<String>> oneTimeCommands = new HashMap<>();
	
	public CommandLoader(Map<String, List<String>> userCommands, Map<String, List<String>> hostCommands,
			List<String> defaultCommands) {
		this.userCommands = userCommands;
		this.hostCommands = hostCommands;
		this.defaultCommands = defaultCommands;
	}
	
	public CommandLoader() {
		this.userCommands = new HashMap<>();
		this.hostCommands = new HashMap<>();
		this.defaultCommands = new ArrayList<>();
	}
	
	public List<String> getDefaultCommands() {
		return new ArrayList<>(defaultCommands);
	}
	
	public List<String> getUserCommands(String user){
		if(userCommands.containsKey(user)) {
			return new ArrayList<>(userCommands.get(user));
		}else {
			return null;
		}
	}
	
	public List<String> getHostCommands(String host){
		if(hostCommands.containsKey(host)) {
			return new ArrayList<>(hostCommands.get(host));
		}else {
			return null;
		}
	}
	
	public void addOneTimeSessionInitiationCommands(SessionAttributeDescriptor desc, List<String> commands) {
		oneTimeCommands.put(desc, commands);
	}
	
	public synchronized List<String> getOneTimeSessionCommands(SessionAttributeDescriptor desc){
		List<String> returnVal = null;
		SessionAttributeDescriptor selectedCandidate = null;
		for(SessionAttributeDescriptor candidate : oneTimeCommands.keySet()) {
			if(candidate.username.equals(desc.username) && candidate.hostname.equals(desc.hostname)) {
				boolean match = true;
				if(candidate.isElevated != null && candidate.isElevated != desc.isElevated) {
					match = false;
				}
				if(candidate.protocol != null && candidate.protocol != desc.protocol) {
					match = false;
				}
				if(match) {
					selectedCandidate = candidate;
					returnVal = oneTimeCommands.get(candidate);
					break;
				}
			}
		}
		if(selectedCandidate != null) {
			oneTimeCommands.remove(selectedCandidate);
		}
		return returnVal;
	}
}
