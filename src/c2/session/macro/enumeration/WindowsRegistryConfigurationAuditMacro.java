package c2.session.macro.enumeration;

import c2.WindowsConstants;
import c2.session.macro.AbstractAuditMacro;
import c2.session.macro.MacroOutcome;

public class WindowsRegistryConfigurationAuditMacro extends AbstractAuditMacro {

	public static final String CMD = "win_reg_config_audit";

	public static final String CHECK_REG_FOR_UAC_CMD = "REG QUERY HKEY_LOCAL_MACHINE\\Software\\Microsoft\\Windows\\CurrentVersion\\Policies\\System\\ /v EnableLUA";
	public static final String CHECK_REG_FOR_LSA_CMD = "REG QUERY HKLM\\SYSTEM\\CurrentControlSet\\Control\\LSA /v RunAsPPL";
	public static final String CHECK_REG_FOR_LSA_CFG_CMD = "REG QUERY HKLM\\SYSTEM\\CurrentControlSet\\Control\\LSA /v LsaCfgFlags";
	public static final String CHECK_LEGACY_LAPS_INSTALLED = "reg query \"HKLM\\Software\\Policies\\Microsoft Services\\AdmPwd\" /v AdmPwdEnabled";
	
	public static final String CHECK_PLAINTEXT_DIGEST_CMD = "reg query HKLM\\SYSTEM\\CurrentControlSetControlSecurityProviders\\WDigest /v UseLogonCredential";
	
	public static final String CHECK_AUTOLOGON = "reg query \"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Winlogon\"";
	
	@Override
	public String getReadableName() {
		return "Windows Registry Configuration Audit Macro";
	}

	@Override
	public String getInvocationCommandDescription() {
		return CMD;
	}

	@Override
	public String getBehaviorDescription() {
		return "This macro audits the Windows registry for a variety of configurations changes that may leave a system less secure";
	}

	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equals(CMD);
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		// Check to ensure UAC is enabled
		sendCommand(CHECK_REG_FOR_UAC_CMD, sessionId, outcome);
		boolean hasFindings = false;
		String response = awaitResponse(sessionId, outcome);
		if (!response.contains("EnableLUA    REG_DWORD    0x1")) {
			hasFindings = true;
			outcome.addAuditFinding(
					"Warning: UAC protection may not be enabled. Check the following registry settings to validate "
							+ CHECK_REG_FOR_UAC_CMD);
		}

		// Check to ensure LSA is enabled
		sendCommand(CHECK_REG_FOR_LSA_CMD, sessionId, outcome);
		response = awaitResponse(sessionId, outcome);
		if (!response.contains("0x1")) {
			hasFindings = true;
			outcome.addAuditFinding(
					"Warning: LSA protection may not be enabled. LSA protection provides additional protection against credential dumping and should be enabled if possible. Check the following registry settings to validate "
							+ CHECK_REG_FOR_LSA_CMD);
		}

		// Check to ensure LSA Credential Guard is enabled
		sendCommand(CHECK_REG_FOR_LSA_CFG_CMD, sessionId, outcome);
		response = awaitResponse(sessionId, outcome);
		if (!response.contains("0x1")) {
			hasFindings = true;
			outcome.addAuditFinding(
					"Warning: LSA Credential Guard protection may not be enabled. LSA protection provides additional protection against credential dumping and should be enabled if possible. Check the following registry settings to validate "
							+ CHECK_REG_FOR_LSA_CFG_CMD);
		}

		sendCommand(CHECK_LEGACY_LAPS_INSTALLED, sessionId, outcome);
		response = io.awaitMultilineCommands(sessionId);
		if(!response.contains(WindowsConstants.WINDOWS_NO_REGISTRY_KEY)) {
			hasFindings = true;
			outcome.addAuditFinding("Warning: This machine has been configured to use Legacy LAPS. Consider upgrading to the modern implementation of LAPS.");
		}
		
		// Check for WDigest plaintext passwords
		sendCommand(CHECK_PLAINTEXT_DIGEST_CMD, sessionId, outcome);
		response = awaitResponse(sessionId, outcome);
		if (response.contains("0x1")) {
			hasFindings = true;
			outcome.addAuditFinding(
					"Warning: plaintext password storage appears to be enabled. Please confirm with "
							+ CHECK_PLAINTEXT_DIGEST_CMD);
		}
		
		sendCommand(CHECK_AUTOLOGON, sessionId, outcome);
		response = awaitResponse(sessionId, outcome);
		if(!response.contains("AutoAdminLogon    REG_SZ    0")) {
			hasFindings = true;
			outcome.addAuditFinding("Warning: AutoAdminLogon is not disabled set to 0");
		}
		
		if(!hasFindings) {
			outcome.addMacroMessage("No findings");
		}
		
		return outcome;
	}

}
