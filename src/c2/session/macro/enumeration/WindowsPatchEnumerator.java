package c2.session.macro.enumeration;

import c2.session.macro.AbstractCommandMacro;
import c2.session.macro.MacroOutcome;

public class WindowsPatchEnumerator extends AbstractCommandMacro {

	public static final String COMMAND = "enum_patches";
	public static final String ARG_WMIC = "wmic";
	public static final String ARG_PS = "ps";
	public static final String WMIC_COMMAND = "wmic qfe list full /format:list";
	public static final String PS_COMMAND = "powershell -c \"get-hotfix\"";
	
	@Override
	public boolean isCommandMatch(String cmd) {
		String elements[] = cmd.split(" ");
		return elements[0].equalsIgnoreCase(COMMAND);
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		String elements[] = cmd.split(" ");
		if(elements.length == 2) {
			if(elements[1].equalsIgnoreCase(ARG_WMIC)) {
				sendCommand(WMIC_COMMAND, sessionId, outcome);
				awaitResponse(sessionId, outcome);
			}else if(elements[1].equalsIgnoreCase(ARG_PS)) {
				sendCommand(PS_COMMAND, sessionId, outcome);
				awaitResponse(sessionId, outcome);
			}else {
				outcome.addError("Invalid argument: " + elements[1]);
			}
		}else {
			outcome.addError(COMMAND+": requires a single argument - ps or wmic");
		}
		return outcome;
	}

}
