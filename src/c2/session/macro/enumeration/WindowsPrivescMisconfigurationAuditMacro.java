package c2.session.macro.enumeration;

import java.util.List;

import c2.session.macro.AbstractAuditMacro;
import c2.session.macro.MacroOutcome;
import c2.win.WindowsQuotedServiceChecker;
import c2.win.WindowsToolOutputParseException;
import c2.win.WindowsUserPriviledgeParser;

public class WindowsPrivescMisconfigurationAuditMacro extends AbstractAuditMacro{

	public static final String CMD = "audit_win_privesc_misconfig";
	public static final String ALL_CLEAR_MSG = "Unauthorized Priviledge Escalation Audit Complete, No Findings";
	
	@Override
	public String getReadableName() {
		return "Audit Windows Misconfigurations Vulnerable to Unauthorized Priviledge Escalation";
	}

	@Override
	public String getInvocationCommandDescription() {
		return CMD;
	}

	@Override
	public String getBehaviorDescription() {
		return "This macro audits Windows for common misconfigurations that an adversary can use to elevate privledges";
	}

	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equals(CMD);
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		boolean hasFindings = false;
		
		sendCommand(WindowsQuotedServiceChecker.SERVICE_PATH_QUERY, sessionId, outcome);
		String value = io.awaitDiscreteCommandResponse(sessionId);
		try {
			List<String> vulnServices = WindowsQuotedServiceChecker.parseServicesQueryOutput(value);
			if(!vulnServices.isEmpty()) {
				hasFindings = true;
				for(String service : vulnServices) {
					outcome.addAuditFinding("Warning, service path is unquoted and may be used for priviledge escalation: " + service);
				}
			}
		}catch(WindowsToolOutputParseException ex) {
			outcome.addError("Could not audit Windows Services for quoted spaces");
		}
		
		sendCommand(WindowsUserPriviledgeParser.QUERY, sessionId, outcome);
		value = io.awaitDiscreteCommandResponse(sessionId);
		try {
			WindowsUserPriviledgeParser privs = new WindowsUserPriviledgeParser(value);
			if(privs.isHasSeImpersonate()) {
				hasFindings = true;
				outcome.addAuditFinding("Warning: user has SeImpersonatePriviledge. This may be used to hijack the identity of another user that authenticates to a process controlled by this user.");
			}
		}catch(WindowsToolOutputParseException ex) {
			outcome.addError("Could not enumerate user priviledges");
		}
		
		if(!hasFindings) {
			outcome.addMacroMessage(ALL_CLEAR_MSG);
		}
		
		return outcome;
	}

}
