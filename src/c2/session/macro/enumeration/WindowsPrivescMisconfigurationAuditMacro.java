package c2.session.macro.enumeration;

import java.util.List;

import c2.Commands;
import c2.WindowsConstants;
import c2.session.IOManager;
import c2.session.macro.AbstractAuditMacro;
import c2.session.macro.MacroOutcome;
import c2.win.WindowsFileSystemTraverser;
import c2.win.WindowsToolOutputParseException;
import c2.win.WindowsUserPriviledgeParser;
import c2.win.services.WindowsServiceBinaryPermissionChecker;
import c2.win.services.WindowsServiceParser;

public class WindowsPrivescMisconfigurationAuditMacro extends AbstractAuditMacro{

	public static final String CMD = "audit_win_privesc_misconfig";
	public static final String ALL_CLEAR_MSG = "Unauthorized Priviledge Escalation Audit Complete, No Findings";
	
	public static final String CHECK_AUTO_ELEVATE_INSTALLER_CU = "reg query HKCU\\SOFTWARE\\Policies\\Microsoft\\Windows\\Installer /v AlwaysInstallElevated 2>&1";
	public static final String CHECK_AUTO_ELEVATE_INSTALLER_LM = "reg query HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows\\Installer /v AlwaysInstallElevated 2>&1";

	public static final String CHECK_FOR_GPP_HISTORY_FILES = "dir /s/b Groups.xml == Services.xml == Scheduledtasks.xml == DataSources.xml == Printers.xml == Drives.xml 2>&1";
	
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
		
		//sendCommand(WindowsServiceParser.SERVICE_PATH_QUERY, sessionId, outcome);
		sendCommand(WindowsServiceParser.SERVICE_PATH_QUERY_CSV, sessionId, outcome);
		sendCommand(WindowsServiceParser.CAT_CSV, sessionId, outcome);
		String value = io.awaitDiscreteCommandResponse(sessionId);
		try {
			WindowsServiceParser parser = WindowsServiceParser.parseServicesCSVQueryOutput(value);
			List<String> vulnServices = parser.getUnquotedServicesList();
			if(!vulnServices.isEmpty()) {
				hasFindings = true;
				for(String service : vulnServices) {
					outcome.addAuditFinding("Warning, service path is unquoted and may be used for priviledge escalation: " + service);
				}
			}
			if(WindowsServiceBinaryPermissionChecker.processListOfServiceBinariesForProblematicPermissions(io, sessionId, parser.getServices(), io.getSessionDescriptor(sessionId).username, outcome)) {
				hasFindings = true;
			}
		}catch(WindowsToolOutputParseException ex) {
			outcome.addError("Could not audit Windows Services");
		}
		sendCommand(WindowsServiceParser.DEL_CSV, sessionId, outcome);
		
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
		
		sendCommand(CHECK_AUTO_ELEVATE_INSTALLER_CU, sessionId, outcome);
		value = io.awaitMultilineCommands(sessionId);
		if(!value.contains(WindowsConstants.WINDOWS_NO_REGISTRY_KEY)) {
			hasFindings = true;
			outcome.addAuditFinding("Warning: current user is configured for installers to auto-elevate to administrator rights. This settings is rarely appropriate and should be evaluated.");
		}
		
		sendCommand(CHECK_AUTO_ELEVATE_INSTALLER_LM, sessionId, outcome);
		value = io.awaitMultilineCommands(sessionId);
		if(!value.contains(WindowsConstants.WINDOWS_NO_REGISTRY_KEY)) {
			hasFindings = true;
			outcome.addAuditFinding("Warning: local machine is configured for installers to auto-elevate to administrator rights. This settings is rarely appropriate and should be evaluated.");
		}
		
		try {
		String originalDrive = WindowsFileSystemTraverser.getCurrentDaemonDirectory(io, sessionId);
		//CD to windows directory
		sendCommand("echo %SYSTEMDRIVE%", sessionId, outcome);
		String output = io.awaitDiscreteCommandResponse(sessionId);
		String windowsCd = "cd " + output.substring(0, 2) + "\\Windows";
		sendCommand(windowsCd, sessionId, outcome);
		output = io.awaitDiscreteCommandResponse(sessionId);
		if(!output.substring(1).toLowerCase().startsWith(":\\windows")) {
			outcome.addError("Cannot audit Windows directory for GPP files");
		}else {
			//Search for GPP history files
			sendCommand(CHECK_FOR_GPP_HISTORY_FILES, sessionId, outcome);
			value = io.awaitMultilineCommands(sessionId);
			if(!value.contains("File Not Found")) {
				hasFindings = true;
				outcome.addAuditFinding("Warning: Password may be recoverable from group policy password files, please check: '" + value + "'");
			}
			sendCommand("cd " + originalDrive, sessionId, outcome);
			output = io.awaitDiscreteCommandResponse(sessionId);
			if(!output.startsWith(originalDrive)) {
				outcome.addMacroMessage("Warning: unable to restore working directory after GPP audit");
			}
		}
		}catch(WindowsToolOutputParseException ex) {
			outcome.addMacroMessage("Warning: unable to query current working directory before GPP audit");
		}
		
		if(!hasFindings) {
			outcome.addMacroMessage(ALL_CLEAR_MSG);
		}
		
		return outcome;
	}

}
