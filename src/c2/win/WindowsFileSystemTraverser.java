package c2.win;

import java.util.ArrayList;
import java.util.List;

import c2.Commands;
import c2.WindowsConstants;
import c2.session.IOManager;

public class WindowsFileSystemTraverser {

	public static String getCommandForDriveLetterDiscovery() {
		return "wmic logicaldisk get deviceid";
	}
	
	public static String getCurrentDaemonDirectory(IOManager io, int sessionId) throws WindowsToolOutputParseException{
		//Get current working directory
		io.sendCommand(sessionId, Commands.CLIENT_CMD_PWD);
		String output = io.awaitDiscreteCommandResponse(sessionId);
		String[] elements = output.split(WindowsConstants.WINDOWS_LINE_SEP);
		// TODO to better validatation!
		if(elements.length > 0) {
			return elements[0];
		}else {
			throw new WindowsToolOutputParseException("Daemon did not return current working directory, unable to parse");
		}
	}

	public static List<String> getListOfDriveLettersFromOutput(String output) throws WindowsToolOutputParseException {
		List<String> drives = new ArrayList<>();
		String lineSplitter = WindowsConstants.WINDOWS_LINE_SEP;
		// if(!output.contains(WindowsConstants.WINDOWS_LINE_SEP)) {
		// lineSplitter = Constants.NEWLINE;
		// }
		String outputLines[] = output.split(lineSplitter);
		if (outputLines.length < 2 && !outputLines[0].startsWith("DeviceId")) {
			throw new WindowsToolOutputParseException("Tool output should start with DeviceId");
		}
		try {
			for (int idx = 1; idx < outputLines.length; idx++) {
				String line = outputLines[idx].trim();
				if (line.length() > 0) {
					drives.add(line.charAt(0) + "");
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new WindowsToolOutputParseException("Unable to parse DeviceID query");
		}
		return drives;
	}

	public static List<String> getRemoteDirectoriesFromDir(String output, String currentPath)
			throws WindowsToolOutputParseException {
		List<String> folders = new ArrayList<>();
		String lines[] = output.split(WindowsConstants.WINDOWS_LINE_SEP);
		int idxOfDirectoryMarker = -1;

		for (int idx = 0; idx < lines.length; idx++) {
			if (lines[idx].contains("Directory of ")) {
				idxOfDirectoryMarker = idx;
				break;
			}
		}

		if (idxOfDirectoryMarker == -1) {
			throw new WindowsToolOutputParseException("dir output not recognized");
		}

		for (int idx = idxOfDirectoryMarker + 2; idx < lines.length; idx++) {
			String line = lines[idx];
			if (line.contains("<DIR>")) {
				// Find where the DIR tag is, and take all leadning and trailing whitespace off
				// the remainder of the string to get the directory
				line = line.substring(line.indexOf("<DIR>") + 5).trim();
				folders.add(currentPath + "\\" + line);
			}
		}

		return folders;
	}

}
