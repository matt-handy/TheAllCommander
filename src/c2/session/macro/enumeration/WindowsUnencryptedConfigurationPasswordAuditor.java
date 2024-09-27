package c2.session.macro.enumeration;

import java.util.List;

import c2.Commands;
import c2.WindowsConstants;
import c2.session.macro.AbstractAuditMacro;
import c2.session.macro.MacroOutcome;
import c2.win.WindowsFileSystemTraverser;
import c2.win.WindowsToolOutputParseException;

public class WindowsUnencryptedConfigurationPasswordAuditor extends AbstractAuditMacro {

	public static final String CMD = "audit_password_storage";
	
	public static final String AUDIT_PASSWORDS_CMD = "findstr /s /n /m /i password *.xml *.ini *.txt *.cfg *.config";

	@Override
	public String getReadableName() {
		return "Password Storage Auditor";
	}

	@Override
	public String getInvocationCommandDescription() {
		return CMD;
	}

	@Override
	public String getBehaviorDescription() {
		return "This macro audits the file system for configuration files that have stored passwords unencrypted and flags them for review.";
	}

	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equalsIgnoreCase(CMD);
	}

	private static boolean isDirectoryOnPasswordAuditCheckList(String folderName) {
		//Don't audit Windows and Program Files directories
		if(folderName.endsWith("Windows") || folderName.endsWith("Program Files") || 
				folderName.endsWith("Program Files (x86)")) {
			return false;
		}else {
			return true;
		}
	}
	
	//These packages are often nested in other installation directories, so we can't rely on them being at the start of a path
	//TODO: Migrate these to a configuration file
	private static boolean isKnownStaticPackage(String path) {
		if(path.contains("MailKit\\submodules\\MimeKit") || path.contains("MailKit\\rfc") ||
				path.contains("packages\\Portable.BouncyCastle") || path.contains("packages\\MailKit") || path.contains("MailKit\\bin\\Debug")) {
			return true;
		}else{
			return false;
		}
	}
	
	//These packages are always located at one of the root folders that is scanned, so the position at the start of a path
	//is fixed
	private static boolean isDirectoryOnFolderWhitelist(String path) {
		if(path.startsWith("Corel\\Messages") || path.startsWith("AppData\\Local\\packages\\MicrosoftWindows") ||
				path.startsWith("MySQL\\MySQL Installer for Windows") || path.startsWith("AppData\\Local\\Microsoft\\VisualStudio") ||
				path.startsWith("Microsoft\\ClickToRun\\MachineData\\Catalog\\Packages") || path.startsWith("Microsoft\\VisualStudio\\Packages\\AndroidEmulator")) {
			return true;
		}
		return false;
	}
	
	private boolean auditPwdForPasswords(int sessionId, MacroOutcome outcome, String dir) {
		sendCommand(AUDIT_PASSWORDS_CMD, sessionId, outcome);
		sendCommand("echo TAC_COMPLETE_SCAN", sessionId, outcome);
		String output = io.awaitMultilineCommands(sessionId, 1000 * 60 * 15);//15 minutes of waiting
		//System.out.println("Got back " + output);
		while(output == null || !output.contains("TAC_COMPLETE_SCAN")) {
			//System.out.println("SCANNING AGAIN!!!!");
			String testOutput = io.awaitMultilineCommands(sessionId, 1000 * 60 * 15);//15 minutes of waiting
			if(testOutput != null) {
				output = output + WindowsConstants.WINDOWS_LINE_SEP + testOutput;
			}
		}
		String lines[] = output.split(WindowsConstants.WINDOWS_LINE_SEP);
		boolean hadFinding = false;
		for(String line : lines) {
			line = line.trim();
			//Skip empty lines and 
			//System.out.println("Checking out: '" + line + "'");
			if(!line.equalsIgnoreCase("") && !line.startsWith("TAC_COMPLETE_SCAN") && !line.startsWith("FINDSTR") && !line.startsWith("Use /OFFLINE for not skipping such files.") && !isDirectoryOnFolderWhitelist(line) && !isKnownStaticPackage(line)) {
				outcome.addAuditFinding(dir + ": File contains potential unencrypted password: " + line);
				hadFinding  = true;
			}
		}
		return hadFinding;
	}
	
	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();

		try {
			String originalDrive = WindowsFileSystemTraverser.getCurrentDaemonDirectory(io, sessionId);
			
			// TODO audit Registry keys

			// TODO audit network shares (punt to a later feature)
			
			//Enumerate local filesystem
			try {
				sendCommand(WindowsFileSystemTraverser.getCommandForDriveLetterDiscovery(), sessionId, outcome);
				String output = awaitResponse(sessionId, outcome);
				//System.out.println("Drives: '" + output + "'");
				List<String> drives = WindowsFileSystemTraverser.getListOfDriveLettersFromOutput(output);
				//System.out.println("Returned?");
				boolean hasFinding = false;
				for (String drive : drives) {
					//System.out.println("Going to drive: " + drive);
					sendCommand("cd /d " + drive + ":\\ ", sessionId, outcome);
					output = awaitResponse(sessionId, outcome);
					if(!output.startsWith(drive + ":\\")) {
						outcome.addMacroMessage("Unable to relocate to audit drive: " + drive);
						continue;
					}
					sendCommand("dir", sessionId, outcome);
					output = awaitResponse(sessionId, outcome);
					//System.out.println("Dir content: " + output);
					List<String> directories = WindowsFileSystemTraverser.getRemoteDirectoriesFromDir(output, drive + ":");
					for(String dir : directories) {
						//System.out.println("Going to dir: " + dir);
						if(isDirectoryOnPasswordAuditCheckList(dir)) {
							//System.out.println("CD to dir: " + dir);
							sendCommand("cd " + dir, sessionId, outcome);
							output = awaitResponse(sessionId, outcome);
							//System.out.println("CD resp: " + output);
							if(!output.startsWith(dir)) {
								outcome.addMacroMessage("Unable to relocate to audit drive: " + dir);
								continue;
							}
							if(dir.endsWith(":\\Users")) {
								sendCommand("dir", sessionId, outcome);
								output = awaitResponse(sessionId, outcome);
								List<String> userDirectories = WindowsFileSystemTraverser.getRemoteDirectoriesFromDir(output, dir);
								for(String userDir : userDirectories) {
									//System.out.println("Audit User Dir: " + userDir);
									sendCommand("cd " + userDir, sessionId, outcome);
									output = awaitResponse(sessionId, outcome);
									//System.out.println("User CD resp: " + output);
									if(!output.startsWith(userDir)) {
										outcome.addMacroMessage("Unable to relocate to audit drive: " + userDir);
										continue;
									}
									if(!userDir.equalsIgnoreCase("All Users")) {
										if(auditPwdForPasswords(sessionId, outcome, userDir)) {
											hasFinding=true;
										}
									}
								}
							}else {
								if(auditPwdForPasswords(sessionId, outcome, dir)) {
									hasFinding=true;
								}
							}
						}
					}
				}
				if(!hasFinding) {
					outcome.addMacroMessage("Unencrypted password Audit Complete, no findings");
				}else {
					outcome.addMacroMessage("Unencrypted Password Audit Complete");
				}
			} catch (WindowsToolOutputParseException ex) {
				outcome.addError("Unable to audit file system for improper password storage: " + ex.getMessage());
			}
			
			//The /d flag allows for change of drive letter, restores original working directory
			//System.out.println("Restoring: '" + originalDrive + "'");
			sendCommand("cd /d " + originalDrive + "", sessionId, outcome);
			String output = awaitResponse(sessionId, outcome);
			//System.out.println("Finish: " + output);
			if(!output.startsWith(originalDrive)) {
				outcome.addMacroMessage("Unable to restore original drive: " + originalDrive);
			}
		}catch(WindowsToolOutputParseException ex) {
			outcome.addError("Cannot query for current working directory, aborting password audit");
		}
		
		
		return outcome;
	}

}
