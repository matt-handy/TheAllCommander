package c2.session.macro.enumeration;

import java.util.ArrayList;
import java.util.List;

import c2.session.IOManager;
import c2.session.macro.AbstractAuditMacro;
import c2.session.macro.MacroOutcome;
import c2.win.WindowsToolOutputParseException;
import c2.win.process.WindowsModule;
import c2.win.process.WindowsProcessInfo;
import c2.win.process.WindowsProcessInfoGatherer;
import c2.win.remotefiles.WindowsFileSystemAcl;
import c2.win.remotefiles.WindowsRemoteFileGatherer;

public class WindowsAuditDllIntegrityMacro extends AbstractAuditMacro {

	public static final String NAME = "Windows DLL Permissions Integrity Auditor";
	public static final String CMD = "audit_dll_integrity";
	public static final String GET_SYSTEMDRIVE_CMD = "echo %SYSTEMDRIVE%";
	public static final String GET_PATH_CMD= "powershell -c \"Get-Item Env:Path | Select-Object -ExpandProperty Value\"";
	
	@Override
	public String getReadableName() {
		return NAME;
	}

	@Override
	public String getInvocationCommandDescription() {
		return CMD;
	}

	@Override
	public String getBehaviorDescription() {
		return "This macro checks to see if the DLLs used by someone else's process or common DLLs loaded from PATH are vulnerable to modification by the current user";
	}

	@Override
	public boolean isCommandMatch(String cmd) {
		return CMD.equals(cmd);
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		String username = io.getSessionDescriptor(sessionId).username;
		
		MacroOutcome outcome = new MacroOutcome();
		
		//TODO: Make this common code
		io.sendCommand(sessionId, GET_SYSTEMDRIVE_CMD);
		String systemDriveRaw = io.awaitMultilineCommands(sessionId);		
		String sysdriveLetter = systemDriveRaw.split("\r\n")[0].trim();
		if(sysdriveLetter.length() != 2 && sysdriveLetter.charAt(1) != ':') {
			outcome.addError("Cannot audit DLL integrity, cannot query system drive letter");
			return outcome;
		}
	
		//Inspired loosely by Powerup's Find-ProcessDLLHijack
		List<String> knownDlls = new ArrayList<>();
		try {
		knownDlls = WindowsRemoteFileGatherer.getKnownDlls(sessionId, io);
		}catch(WindowsToolOutputParseException ex) {
			outcome.addMacroMessage("Warning: unable to get list of KnownDlls to calibrate service dll permission check");
		}
		
		try {
		List<WindowsProcessInfo> processes = WindowsProcessInfoGatherer.gatherWindowsProcessInfo(io, sessionId, true);
		for(WindowsProcessInfo process : processes) {
			if(!process.username.equals(username) && process.pid != 0 && !process.username.equals("N/A")) {
				for(WindowsModule module : process.modules) {
					if(knownDlls.contains(module.name) || module.path.toLowerCase().startsWith(sysdriveLetter + "\\windows")) {
						//skip
					}else {
						//System.out.println("Checking DLL: " + module.path);
						if(WindowsRemoteFileGatherer.canUserModifyFileSystemObject(username, module.path, sessionId, io)) {
							outcome.addAuditFinding("Warning: current user can modify other processes DLL. Process: " + process.name + " and module " + module.name);
						}
					}
				}
			}
		}
		}catch(WindowsToolOutputParseException ex) {
			outcome.addError("Warning, cannot audit process DLL integrity. " + ex.getMessage());
		}
		//System.out.println("Time to audit path");
		
		//Directly inspired by PowerUp's Find-PathDLLHijack
		//TODO: Make getting the PATH a function
		sendCommand(GET_PATH_CMD, sessionId, outcome);
		String pathVar = awaitResponse(sessionId, outcome);
		pathVar = pathVar.replace("\n", "").replace("\r", "");
		//System.out.println("Trying to work with: " + pathVar);
		String paths[] = pathVar.split(";");
		for(String path : paths) {
			try {
				//System.out.println("Checking: " + path);
			if(WindowsRemoteFileGatherer.canUserModifyFileSystemObject(username, path, sessionId, io)) {
				outcome.addAuditFinding("Warning: user can modify path directory: " + path + " which may lead to insecure user action." );
			}
			}catch(WindowsToolOutputParseException ex) {
				outcome.addError("Warning: cannot audit path element: " + path + ", " + ex.getMessage());
			}
		}
		
		outcome.addMacroMessage(NAME + " Complete");
		return outcome;
	}

}
