package c2.session.macro.users;

import c2.Commands;
import c2.HarvestProcessor;
import c2.session.IOManager;
import c2.session.macro.AbstractCommandMacro;
import c2.session.macro.MacroOutcome;

public class UserEnumeratorMacro extends AbstractCommandMacro {

	public static String COMMAND_STR = "enumerate_users";
	
	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equalsIgnoreCase("enumerate_users");
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		WindowsUserEnumeratorMacro windowsMacro = new WindowsUserEnumeratorMacro(io);
		sendCommand(Commands.OS_HERITAGE, sessionId, outcome);
		String response = awaitResponse(sessionId, outcome);
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

	@Override
	public String getReadableName() {
		return "User Enumerator Macro";
	}

	@Override
	public String getInvocationCommandDescription() {
		return COMMAND_STR;
	}

	@Override
	public String getBehaviorDescription() {
		return "This macro will enumerate all users and groups for the host system. Not currently implemented on Linux or Mac";
	}

}
