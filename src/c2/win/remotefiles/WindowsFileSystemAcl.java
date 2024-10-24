package c2.win.remotefiles;

import c2.win.WindowsToolOutputParseException;

public class WindowsFileSystemAcl {
	
	public enum AccessControlType {ALLOW, DENY;
		public static AccessControlType parseStr(String str) throws WindowsToolOutputParseException{
			if(str.equalsIgnoreCase("Allow")) {
				return ALLOW;
			}else if(str.equalsIgnoreCase("Deny")) {
				return DENY;
			}else {
				throw new WindowsToolOutputParseException("Unknown AccessControlType: " + str);
			}
		}
	};
	//For the non-standard entries not included in the Microsoft enum, please see: 
	//https://stackoverflow.com/questions/9694834/encountering-a-filesystemrights-value-that-isnt-defined-in-enumeration
	public enum FileSystemRights {APPEND_DATA, CHANGE_PERMISSIONS, CREATE_DIRECTORIES, CREATE_FILES, DELETE, DELETE_SUBDIRECTORIES_AND_FILES, 
		EXECUTE_FILE, FULL_CONTROL, LIST_DIRECTORY, MODIFY, READ, READ_AND_EXECUTE, READ_ATTRIBUTES, READ_DATA, READ_EXTENDED_ATTRIBUTES,
		READ_PERMISSIONS, SYNCHRONIZE, TAKE_OWNERSHIP, TRAVERSE, WRITE, WRITE_ATTRIBUTE, WRITE_DATA, WRITE_EXTENDED_ATTRIBUTES,
		MODIFY_SYNCHRONIZE, READ_AND_EXECUTE_SYNCHRONIZE;
	
		public static FileSystemRights parseStr(String str) throws WindowsToolOutputParseException{
			if(str.equalsIgnoreCase("AppendData")) {
				return APPEND_DATA;
			}else if(str.equalsIgnoreCase("ChangePermissions")) {
				return CHANGE_PERMISSIONS;
			}else if(str.equalsIgnoreCase("CreateDirectories")) {
				return CREATE_DIRECTORIES;
			}else if(str.equalsIgnoreCase("CreateFiles")) {
				return CREATE_FILES;
			}else if(str.equalsIgnoreCase("Delete")) {
				return DELETE;
			}else if(str.equalsIgnoreCase("DeleteSubdirectoriesAndFiles")) {
				return DELETE_SUBDIRECTORIES_AND_FILES;
			}else if(str.equalsIgnoreCase("ExecuteFile")) {
				return EXECUTE_FILE;
			}else if(str.equalsIgnoreCase("FullControl") || str.equals("268435456")) {
				return FULL_CONTROL;
			}else if(str.equalsIgnoreCase("ListDirectory")) {
				return LIST_DIRECTORY;
			}else if(str.equalsIgnoreCase("Modify")) {
				return MODIFY;
			}else if(str.equalsIgnoreCase("Read")) {
				return READ;
			}else if(str.equalsIgnoreCase("ReadAndExecute")) {
				return READ_AND_EXECUTE;
			}else if(str.equalsIgnoreCase("ReadAttributes")) {
				return READ_ATTRIBUTES;
			}
			else if(str.equalsIgnoreCase("ReadData")) {
				return READ_DATA;
			}
			else if(str.equalsIgnoreCase("ReadExtendedAttributes")) {
				return READ_EXTENDED_ATTRIBUTES;
			}
			else if(str.equalsIgnoreCase("ReadPermissions")) {
				return READ_PERMISSIONS;
			}
			else if(str.equalsIgnoreCase("Synchronize")) {
				return SYNCHRONIZE;
			}
			else if(str.equalsIgnoreCase("TakeOwnership")) {
				return TAKE_OWNERSHIP;
			}
			else if(str.equalsIgnoreCase("Traverse")) {
				return TRAVERSE;
			}
			else if(str.equalsIgnoreCase("Write")) {
				return WRITE;
			}
			else if(str.equalsIgnoreCase("WriteAttributes")) {
				return WRITE_ATTRIBUTE;
			}
			else if(str.equalsIgnoreCase("WriteData")) {
				return WRITE_DATA;
			}
			else if(str.equalsIgnoreCase("WriteExtendedAttributes")) {
				return WRITE_EXTENDED_ATTRIBUTES;
			}else if(str.equalsIgnoreCase("Modify, Synchronize") || str.equalsIgnoreCase("-536805376")) {
				return MODIFY_SYNCHRONIZE;
			}else if(str.equalsIgnoreCase("ReadAndExecute, Synchronize") || str.equalsIgnoreCase("-1610612736")) {
				return READ_AND_EXECUTE_SYNCHRONIZE;
			}else {
				throw new WindowsToolOutputParseException("Unable to parse FileSystemRights: " + str);
			}
		}
	};
	
	
	public final FileSystemRights fileSystemRights;
	public final AccessControlType accessControlType;
	public final String identityReference;
	public final String objectName;
	
	public WindowsFileSystemAcl(String objectName, String identityReference, FileSystemRights fileSystemRights, AccessControlType accessControlType) {
		this.objectName = objectName;
		this.identityReference = identityReference;
		this.fileSystemRights = fileSystemRights;
		this.accessControlType = accessControlType;
	}
}
