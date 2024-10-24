package c2.win.services;

import java.util.List;

import c2.session.IOManager;
import c2.session.macro.MacroOutcome;

public class WindowsServiceBinaryPermissionChecker {

	
	
	private static boolean isAConfigurationFile(String arg) {
		return arg.contains(".xml") || arg.contains(".ini") || arg.contains(".txt");
	}
	
	public static boolean processListOfServicesForAccessibleArgumentFiles(IOManager io, int sessionId,
			List<WindowsServiceInfo> serviceBinaries, String currentUsername, MacroOutcome outcome) {
		boolean foundOne = false;
		for (WindowsServiceInfo service : serviceBinaries) {
			for(String arg : service.getServiceArgs()) {
				if(isAConfigurationFile(arg)) {
					io.sendCommand(sessionId, "icacls \"" + arg + "\"");
					String response = io.awaitDiscreteCommandResponse(sessionId);
					if (response.contains("The system cannot find the file specified.")) {
						//Ignore, this wasn't a valid file
					} else {
						String lines[] = response.split("\n");
						for (String line : lines) {
							line = line.toUpperCase();
							if(givesCurrentUserControl(line, currentUsername)) {
								outcome.addAuditFinding("Warning: Service " + service.serviceName
										+ " has potentially insecure permissions on argument, please review: " + response);
								foundOne = true;
							}
						}
					}
				}
			}
		}
		return foundOne;
	}
	
	private static boolean givesCurrentUserControl(String icaclsOutput, String username) {
		return (icaclsOutput.contains("BUILTIN\\USERS") || icaclsOutput.contains(username)
				|| icaclsOutput.contains("EVERYONE") || icaclsOutput.contains("AUTHENTICATED"))
						&& (icaclsOutput.contains("(W)") || icaclsOutput.contains("(M)") || icaclsOutput.contains("(F)"));
	}

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
						if (givesCurrentUserControl(line, currentUsername)) {
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
