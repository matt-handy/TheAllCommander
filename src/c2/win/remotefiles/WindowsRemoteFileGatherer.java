package c2.win.remotefiles;

import c2.session.IOManager;

public class WindowsRemoteFileGatherer {

	public static WindowsRemoteFileInfo gather(String filename, int sessionId, IOManager io) throws Exception {
		try {
			io.sendCommand(sessionId, "powershell -c \"(Get-Item " + filename + ").VersionInfo.FileBuildPart\"");
			String fileBuildPartStr = io.awaitDiscreteCommandResponse(sessionId).replace("\n", "").replace("\r", "").trim();
			int fileBuildPart = Integer.parseInt(fileBuildPartStr);
			
			io.sendCommand(sessionId, "powershell -c \"(Get-Item " + filename + ").VersionInfo.FilePrivatePart\"");
			String filePrivatePartStr = io.awaitDiscreteCommandResponse(sessionId).replace("\n", "").replace("\r", "").trim();
			int filePrivatePart = Integer.parseInt(filePrivatePartStr);
			return new WindowsRemoteFileInfo(fileBuildPart, filePrivatePart);
		}catch(NumberFormatException ex) {
			throw new Exception("Cannot parse return file part information: " + ex.getMessage());
		}catch(NullPointerException ex) {
			throw new Exception("Client did not respond to request for information for file: " + filename);
		}
	}
}
