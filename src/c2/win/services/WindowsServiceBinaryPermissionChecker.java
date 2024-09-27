package c2.win.services;

import java.util.List;

import c2.session.IOManager;
import c2.session.macro.MacroOutcome;

public class WindowsServiceBinaryPermissionChecker {

	// TODO: Improvement ideas
	// Check icacls for files fed as arguments to services, not just service
	// executables

	public static boolean processListOfServiceBinariesForProblematicPermissions(IOManager io, int sessionId,
			List<WindowsServiceInfo> serviceBinaries, String currentUsername, MacroOutcome outcome) {
		boolean foundOne = false;
		for (WindowsServiceInfo service : serviceBinaries) {
			if (!service.executable.toUpperCase().startsWith("C:\\WINDOWS\\SYSTEM32")) {
				io.sendCommand(sessionId, "icacls \"" + service.executable + "\"");
				String response = io.awaitDiscreteCommandResponse(sessionId);
				if (response.contains("The system cannot find the file specified.")) {
					outcome.addAuditFinding(
							"Warning: could not audit binary permissions for service: " + service.serviceName);
				} else {
					//Why not split by \r\n? Good question - sometimes it seems that the handling strips out \r, and we don't
					//need that formatting for readability, so we can split by \n
					String lines[] = response.split("\n");
					for (String line : lines) {
						line = line.toUpperCase();
						if ((line.contains("BUILTIN\\USERS") || line.contains(currentUsername)
								|| line.contains("EVERYONE") || line.contains("AUTHENTICATED"))
										&& (line.contains("(W)") || line.contains("(M)") || line.contains("(F)"))) {
							outcome.addAuditFinding("Warning: Service " + service.serviceName
									+ " has potentially insecure permissions, please review: " + response);
							foundOne = true;
						}
					}
				}
			}
		}
		return foundOne;
	}
}
