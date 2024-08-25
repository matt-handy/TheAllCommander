package c2.win.remotefiles;

public class WindowsRemoteFileInfo {
	public final int fileBuildPart;
	public final int filePrivatePart;
	
	public WindowsRemoteFileInfo(int fileBuildPart, int filePrivatePart) {
		this.fileBuildPart = fileBuildPart;
		this.filePrivatePart = filePrivatePart;
	}
}
