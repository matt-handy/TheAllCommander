package c2.win.remotefiles;

import java.util.ArrayList;
import java.util.List;

import c2.WindowsConstants;
import c2.session.IOManager;
import c2.win.WindowsToolOutputParseException;
import c2.win.remotefiles.WindowsFileSystemAcl.AccessControlType;
import c2.win.remotefiles.WindowsFileSystemAcl.FileSystemRights;

public class WindowsRemoteFileGatherer {

	public static final String GET_FS_ACL_TEMPLATE= "powershell -c \"Get-Acl -Path '$FILENAME$' | Select-Object -ExpandProperty Access\"";
	
	public static final String GET_KNOWN_DLLS = "powershell -c \"Get-Item 'HKLM:\\System\\CurrentControlSet\\Control\\Session Manager\\KnownDLLs'\"";
	
	public static final String PROPERTY_STR = "Property";
	
	public static WindowsRemoteFileInfo gather(String filename, int sessionId, IOManager io) throws WindowsToolOutputParseException, WindowsRemoteFileNotFound {
		try {
			io.sendCommand(sessionId, "powershell -c \"(Get-Item " + filename + ").VersionInfo.FileBuildPart\"");
			String fileBuildPartStr = io.awaitDiscreteCommandResponse(sessionId).replace("\n", "").replace("\r", "").trim();
			if(fileBuildPartStr.contains("Get-Item : Cannot find path")) {
				throw new WindowsRemoteFileNotFound("Cannot find path: " + filename);
			}
			int fileBuildPart = Integer.parseInt(fileBuildPartStr);
			
			io.sendCommand(sessionId, "powershell -c \"(Get-Item " + filename + ").VersionInfo.FilePrivatePart\"");
			String filePrivatePartStr = io.awaitDiscreteCommandResponse(sessionId).replace("\n", "").replace("\r", "").trim();
			int filePrivatePart = Integer.parseInt(filePrivatePartStr);
			return new WindowsRemoteFileInfo(fileBuildPart, filePrivatePart);
		}catch(NumberFormatException ex) {
			throw new WindowsToolOutputParseException("Cannot parse return file part information: " + ex.getMessage());
		}catch(NullPointerException ex) {
			throw new WindowsToolOutputParseException("Client did not respond to request for information for file: " + filename);
		}
	}
	
	public static List<String> getKnownDlls(int sessionId, IOManager io) throws WindowsToolOutputParseException{
		List<String> knownDlls = new ArrayList<>();
		io.sendCommand(sessionId, GET_KNOWN_DLLS);
		String response = io.awaitDiscreteCommandResponse(sessionId);
		String[] lines = response.split("\r\n");
		int startDllIdx = 0;
		int propertyColumn = -1;
		for(String line : lines) {
			startDllIdx++;
			if(line.contains(PROPERTY_STR)) {
				startDllIdx++;
				propertyColumn = line.indexOf(PROPERTY_STR);
				break;
			}
		}
		
		if(propertyColumn == -1) {
			throw new WindowsToolOutputParseException("Unable to process KnownDlls, invalid registry response");
		}
		
		for(int idx = startDllIdx; idx < lines.length; idx++) {
			String line = lines[idx].trim();
			if(line.length() > propertyColumn) {
				String data = line.substring(propertyColumn);
				String elements[] = data.split(":");
				if(elements.length == 2) {
					data = elements[1].trim();
					if(data.endsWith(".dll")) {
						knownDlls.add(data);
					}
				}
			}
		}
		
		return knownDlls;
	}
	
	public static List<WindowsFileSystemAcl> getFileSystemAcl(String path, int sessionId, IOManager io) throws WindowsToolOutputParseException {
		List<WindowsFileSystemAcl> acl = new ArrayList<>();
		String command = GET_FS_ACL_TEMPLATE.replace("$FILENAME$", path);
		io.sendCommand(sessionId, command);
		String aclResponse = io.awaitMultilineCommands(sessionId);
		String lines[] = aclResponse.split(WindowsConstants.WINDOWS_LINE_SEP);
		WindowsFileSystemAcl.AccessControlType type = null;
		WindowsFileSystemAcl.FileSystemRights rights = null;
		String identityReference = null;
		boolean isInData = false;
		for(int idx = 0; idx < lines.length; idx++) {
			String line = lines[idx];
			if(isInData) {
				if(line.startsWith("AccessControlType")) {
					if(!line.contains(":") || line.indexOf(":") > line.length()) {
						throw new WindowsToolOutputParseException("Improper format received for File System ACL");
					}
					String typeStr = line.substring(line.indexOf(":") + 2).replace("\r", "").replace("\n", "");
					type = WindowsFileSystemAcl.AccessControlType.parseStr(typeStr);
				}else if(line.startsWith("IdentityReference")) {
					if(!line.contains(":") || line.indexOf(":") > line.length()) {
						throw new WindowsToolOutputParseException("Improper format received for File System ACL");
					}
					identityReference = line.substring(line.indexOf(":") + 2).replace("\r", "").replace("\n", "");
					if(type != null && rights != null) {
						acl.add(new WindowsFileSystemAcl(path, identityReference, rights, type));
						isInData = false;
						identityReference = null;
						type = null;
						rights = null;
					}else {
						throw new WindowsToolOutputParseException("Improper ACL format, not all elements provided");
					}
				}
			}else {
				if(line.startsWith("FileSystemRights")) {
					if(!line.contains(":") || line.indexOf(":") > line.length()) {
						throw new WindowsToolOutputParseException("Improper format received for File System ACL");
					}
					String rightsStr = line.substring(line.indexOf(":") + 2).replace("\r", "").replace("\n", "");
					rights = WindowsFileSystemAcl.FileSystemRights.parseStr(rightsStr);
					isInData = true;
				}
			}
		}
		return acl;
	}
	
	public static boolean canUserModifyFileSystemObject(String username, String path, int sessionId, IOManager io) throws WindowsToolOutputParseException{
		String sessionHostname = io.getSessionDescriptor(sessionId).hostname;
		List<WindowsFileSystemAcl> acl = getFileSystemAcl(path, sessionId, io);
		for(WindowsFileSystemAcl ac : acl) {
			if(ac.accessControlType == AccessControlType.ALLOW && (ac.identityReference.equalsIgnoreCase(sessionHostname + "\\" + username) || ac.identityReference.equalsIgnoreCase(username) ||
					ac.identityReference.equalsIgnoreCase("BUILTIN\\Users"))) {
				//TODO: Check if these can come back 'GenericWrite', 'GenericAll', 'MaximumAllowed', 'WriteOwner', 'WriteDAC', 'WriteData/AddFile', 'AppendData/AddSubdirectory')
				if(ac.fileSystemRights == FileSystemRights.FULL_CONTROL || 
						ac.fileSystemRights == FileSystemRights.MODIFY ||
						//ac.fileSystemRights == FileSystemRights.READ_AND_WRITE ||
						ac.fileSystemRights == FileSystemRights.WRITE || 
						ac.fileSystemRights == FileSystemRights.WRITE_DATA || 
						ac.fileSystemRights == FileSystemRights.TAKE_OWNERSHIP ||
						ac.fileSystemRights == FileSystemRights.APPEND_DATA) {
					return true;
				}
			}
		}
		return false;
	}
}
