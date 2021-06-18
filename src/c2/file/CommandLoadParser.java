package c2.file;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import c2.session.CommandLoader;

public class CommandLoadParser {
	
	private Map<String, List<String>> userCommands = new HashMap<>();
	private Map<String, List<String>> hostCommands = new HashMap<>();
	private List<String> defaultCommands = new ArrayList<>();
	
	private boolean inAllCmds = false;
	private String currentUser = null;
	private String currentHost = null;
	
	private List<String> cmds = new ArrayList<>();
	
	public CommandLoader buildLoader(String filename) throws Exception{
		BufferedReader br = new BufferedReader(new FileReader(filename));
		
		String line;
		
		while((line = br.readLine()) != null) {
			if(line.equals(":all")) {
				inAllCmds = true;
				flushUserState();
				flushHostState();
			}else if(line.startsWith(":user-")) {
				currentUser = line.substring(":user-".length());
				flushAllCmds();
				flushHostState();
			}else if(line.startsWith(":host-")) {
				currentHost = line.substring(":host-".length());
				flushAllCmds();
				flushUserState();
			}else {
				cmds.add(line);
			}
		}
		
		flushAllCmds();
		flushUserState();
		flushHostState();
		
		br.close();
		
		return new CommandLoader(userCommands, hostCommands, defaultCommands);
	}
	
	private void flushUserState() {
		if(currentUser != null) {
			userCommands.put(currentUser, cmds);
			currentUser = null;
			cmds = new ArrayList<>();
		}
	}
	private void flushHostState() {
		if(currentHost != null) {
			hostCommands.put(currentHost, cmds);
			currentHost = null;
			cmds = new ArrayList<>();
		}
	}
	private void flushAllCmds() {
		if(inAllCmds) {
			defaultCommands.addAll(cmds);
			inAllCmds = false;
			cmds = new ArrayList<>();
		}
	}
}
