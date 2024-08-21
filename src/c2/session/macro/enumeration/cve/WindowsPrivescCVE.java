package c2.session.macro.enumeration.cve;

import c2.session.macro.AbstractCommandMacro;
import c2.session.macro.MacroOutcome;

public class WindowsPrivescCVE extends AbstractCommandMacro {

	public static final String CMD = "win_privesc_cve_check";
	
	@Override
	public String getReadableName() {
		return "Windows Privesc CVE Enumerator";
	}

	@Override
	public String getInvocationCommandDescription() {
		return CMD;
	}

	@Override
	public String getBehaviorDescription() {
		return "This macro enumerates Windows version information and checks for critical CVEs that can be used to escalate priviledges";
	}

	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equalsIgnoreCase(CMD);
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		// TODO Auto-generated method stub
		return null;
	}

}
