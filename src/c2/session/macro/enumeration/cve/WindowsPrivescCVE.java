package c2.session.macro.enumeration.cve;

import java.util.List;

import c2.WindowsConstants;
import c2.session.macro.AbstractAuditMacro;
import c2.session.macro.MacroOutcome;
import c2.win.WindowsPatchLevelCVEChecker;
import c2.win.WindowsSystemInfoParser;
import c2.win.remotefiles.WindowsRemoteFileGatherer;
import c2.win.remotefiles.WindowsRemoteFileInfo;

public class WindowsPrivescCVE extends AbstractAuditMacro {

	public static final String CMD = "audit_win_privesc_cve";

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
			for (String cve : cves) {
				macro.addAuditFinding("Applicable CVE: " + cve);
			}
			boolean foundLegacyMS = testLegacyMicrosoftBulletins(sessionId, macro);
			if (cves.size() == 0 && !foundLegacyMS) {
				macro.addMacroMessage("No findings");
			}
		} catch (Exception ex) {
			macro.addError("Cannot process Windows Privesc CVE Audit Data: " + ex.getMessage());
		}
		return macro;
	}

	private boolean testLegacyMicrosoftBulletins(int sessionId, MacroOutcome outcome) {
		boolean hasHadFinding = false;
		// MS10-092
		try {
			WindowsRemoteFileInfo info = WindowsRemoteFileGatherer.gather("%SYSTEMROOT%\\System32\\schedsvc.dll",
					sessionId, io);
			if (info.fileBuildPart == 7600 && info.filePrivatePart <= 20830) {
				outcome.addAuditFinding("Applicable Vulnerability: MS10-092");
				hasHadFinding = true;
			}
		} catch (Exception ex) {
			outcome.addError("Cannot audit MS10-092");
		}

		
		try {
			// MS14-058
			WindowsRemoteFileInfo info = WindowsRemoteFileGatherer.gather("%SYSTEMROOT%\\System32\\win32k.sys",
					sessionId, io);
			if ((info.fileBuildPart == 7600 && info.filePrivatePart >= 18000)
					|| (info.fileBuildPart == 7601 && info.filePrivatePart <= 22823)
					|| (info.fileBuildPart == 9200 && info.filePrivatePart <= 21247)
					|| (info.fileBuildPart == 9600 && info.filePrivatePart <= 17353)) {
				outcome.addAuditFinding("Applicable Vulnerability: MS14-058");
				hasHadFinding = true;
			}
			
			//MS15-051
			if ((info.fileBuildPart == 7600 && info.filePrivatePart <= 18000)
					|| (info.fileBuildPart == 7601 && info.filePrivatePart <= 22823)
					|| (info.fileBuildPart == 9200 && info.filePrivatePart <= 21247)
					|| (info.fileBuildPart == 9600 && info.filePrivatePart <= 17353)) {
				outcome.addAuditFinding("Applicable Vulnerability: MS15-051");
				hasHadFinding = true;
			}
			
			//MS16-034
			if ((info.fileBuildPart == 6002 && info.filePrivatePart < 19597)
					|| (info.fileBuildPart == 7601 && info.filePrivatePart < 19145)
					|| (info.fileBuildPart == 9200 && info.filePrivatePart < 17647)
					|| (info.fileBuildPart == 9600 && info.filePrivatePart < 18228)) {
				outcome.addAuditFinding("Applicable Vulnerability: MS16-034");
				hasHadFinding = true;
			}
			
			//MS16-135
			if ((info.fileBuildPart == 7601 && info.filePrivatePart < 23584) ||
	                (info.fileBuildPart == 9600 && info.filePrivatePart <= 18524) ||
	                (info.fileBuildPart == 10240 && info.filePrivatePart <= 16384) ||
	                (info.fileBuildPart == 10586 && info.filePrivatePart <= 19) ||
	                (info.fileBuildPart == 14393 && info.filePrivatePart <= 446))
	            {
				outcome.addAuditFinding("Applicable Vulnerability: MS16-135");
				hasHadFinding = true;
	            }
		} catch (Exception ex) {
			outcome.addError("Cannot audit MS14-058 or MS15-051");
		}

		//MS15-078
		try {
			WindowsRemoteFileInfo info = WindowsRemoteFileGatherer.gather("%SYSTEMROOT%\\System32\\atmfd.dll",
					sessionId, io);
			if ( info.filePrivatePart == 243) {
				outcome.addAuditFinding("Applicable Vulnerability: MS15-078");
				hasHadFinding = true;
			}
		} catch (Exception ex) {
			//Ignore, modern Windows doesn't have this file so it may not show up
		}
		
		return hasHadFinding;
	}

}
