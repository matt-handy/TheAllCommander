package c2.session.macro.enumeration.cve;

import java.util.List;

import c2.WindowsConstants;
import c2.session.macro.AbstractAuditMacro;
import c2.session.macro.MacroOutcome;
import c2.win.WindowsPatchLevelCVEChecker;
import c2.win.WindowsSystemInfoParser;

public class WindowsPrivescCVE extends AbstractAuditMacro {

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
		MacroOutcome macro = new MacroOutcome();
		sendCommand(WindowsConstants.SYSTEMINFO_CMD, sessionId, macro);
		String info = awaitResponse(sessionId, macro);
		try {
			WindowsSystemInfoParser parser = new WindowsSystemInfoParser(info);
			List<String> cves = WindowsPatchLevelCVEChecker.getApplicableCVEs(parser);		
			for(String cve : cves) {
				macro.addAuditFinding("Applicable CVE: " + cve);
			}
			if(cves.size() == 0) {
				macro.addMacroMessage("No findings");
			}
		}catch(Exception ex) {
			macro.addError("Cannot process Windows Privesc CVE Audit Data: " + ex.getMessage());
		}
		return macro;
	}

}
