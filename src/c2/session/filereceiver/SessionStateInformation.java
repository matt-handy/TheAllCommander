package c2.session.filereceiver;

import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class SessionStateInformation {

	public final String hostname;
	public final Path rootPath;
	
	private long bytesRemainingInCurrentFile;
	private OutputStream os; 
	
	public SessionStateInformation(String hostname, Path contentDir) {
		this.hostname = hostname;
		
		Date dt = new Date();
		String timeStamp = dt.getTime() + "";

		rootPath = Paths.get(contentDir.toString(), hostname, timeStamp);
	}
	
	public long getBytesRemainingInCurrentFile() {
		return bytesRemainingInCurrentFile;
	}
	
	public void setBytesRemainingInCurrentFile(long bytesRemainingInCurrentFile) {
		this.bytesRemainingInCurrentFile = bytesRemainingInCurrentFile;
	}

	public OutputStream getOs() {
		return os;
	}

	public void setOs(OutputStream os) {
		this.os = os;
	}

	public Path getRootPath() {
		return rootPath;
	}
	
	
	
}
