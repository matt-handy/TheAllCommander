package c2.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommandLoader {

	private Map<String, List<String>> userCommands;
	private Map<String, List<String>> hostCommands;
	private List<String> defaultCommands;
	
	public CommandLoader(Map<String, List<String>> userCommands, Map<String, List<String>> hostCommands,
			List<String> defaultCommands) {
		this.userCommands = userCommands;
		this.hostCommands = hostCommands;
		this.defaultCommands = defaultCommands;
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
}
