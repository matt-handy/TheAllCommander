package c2.session.macro.enumeration;

import c2.session.macro.AbstractCommandMacro;
import c2.session.macro.MacroOutcome;

public class WindowsNetworkShareEnumerator extends AbstractCommandMacro {

	public static final String COMMAND = "enum_network_share";
	public static final String WMIC_COMMAND = "wmic share get caption,name,path";
	
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

	@Override
	public String getReadableName() {
		return "Windows Network Share Enumerator";
	}

	@Override
	public String getInvocationCommandDescription() {
		return COMMAND;
	}

	@Override
	public String getBehaviorDescription() {
		return "This macro will use WMIC to enumerate all network shares";
	}

}
