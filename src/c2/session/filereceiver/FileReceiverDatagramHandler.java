package c2.session.filereceiver;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class FileReceiverDatagramHandler {

	private Map<String, SessionStateInformation> sessions = new HashMap<>();
	
	private Path contentDir;
	
	public FileReceiverDatagramHandler(Path contentDir) {
		this.contentDir = contentDir;
	}
	
	public boolean hasSessionCurrently(int clientSessionId, int uploadSessionId) {
		String sessionLookupID = clientSessionId + "-" + uploadSessionId;
		return sessions.containsKey(sessionLookupID);
	}
	
	public void registerNewSession(int clientSessionId, int uploadSessionId, String hostname) {
		String sessionLookupID = clientSessionId + "-" + uploadSessionId;
		SessionStateInformation state = new SessionStateInformation(hostname, contentDir);
		//System.out.println("Registering: " + sessionLookupID);
		sessions.put(sessionLookupID, state);
	}
	
	public void processIncoming(int clientSessionId, int uploadSessionId, byte[] content) {
		String sessionLookupID = clientSessionId + "-" + uploadSessionId;
		//System.out.println("Polling: " + sessionLookupID + " " + sessions);
		SessionStateInformation state = sessions.get(sessionLookupID);
		//System.out.println("Polled: " + sessionLookupID + " " + state);
		//Start of datagram must always be either continuation of existing file or the start
		//of a new file. New files only start at the beginning of the datagram
		Path localFilePath = null;
		int fileStartIdx = 0;
		int bytesToRead = 0;
		OutputStream output = null;
		boolean finishingFile = false;
		
		//System.out.println("Remaining: " + state.getBytesRemainingInCurrentFile());
		if(state.getBytesRemainingInCurrentFile() == 0) {
			ByteBuffer nameLenBuffer = ByteBuffer.wrap(content, 0, 4);
			int fileNameLength = nameLenBuffer.getInt();
			//System.out.println("Filename len: " + fileNameLength);
			String filename = new String(content, 4, fileNameLength);
			//System.out.println("Filename: " + filename);
			if (filename.equals("End of transmission")) {
				// System.out.println(counter + ": Shutting Down Receiver");
				sessions.remove(sessionLookupID);
				return;
			}
			
			// System.out.println(counter + ": Read FCon");
			ByteBuffer fileContentLengthBuffer = ByteBuffer.wrap(content, 4 + fileNameLength, 8);
			long fileContentLength = fileContentLengthBuffer.getLong();
			fileStartIdx = 4 + fileNameLength + 8;
			
			//System.out.println("File content len: " + fileContentLength);
			//Complete file is contained in the transmission
			if(fileContentLength + fileStartIdx == content.length) {
				state.setBytesRemainingInCurrentFile(0);
				bytesToRead = (int) fileContentLength;
				finishingFile = true;
				//Partial transmission	
			}else {
				bytesToRead = content.length - (fileStartIdx);
				state.setBytesRemainingInCurrentFile(fileContentLength - bytesToRead);
			}
			
			//System.out.println("Bytes to read: " + bytesToRead);
			//System.out.println("Filename: " + filename);
			Path filenamePath = Paths.get(filename);
			//System.out.println("Filename: " + filenamePath.toString());
			
			//Path remoteRootPath = filenamePath.toAbsolutePath().getRoot();
			/*
			if (remoteRootPath == null) {
				// We're running on a non-Windows system
				System.out.println("Dewindows-ize");
				String manuallyDeWindowsedFilename = filename.substring(3);
				manuallyDeWindowsedFilename = manuallyDeWindowsedFilename.replace("\\", "/");
				filenamePath = Paths.get(manuallyDeWindowsedFilename);
				localFilePath = Paths.get(state.getRootPath().toString(), filenamePath.toString());
			} else {
			*/
				//Replace leading drive letter colon for windows hosts
				filenamePath = Paths.get(filenamePath.toString().replaceAll(":", ""));
				//If running windows and receive Linux xmission, change delimiter
				if(filenamePath.toString().startsWith("/") && !System.lineSeparator().equals("/")) {
					filenamePath = Paths.get(filenamePath.toString().replaceAll("/", System.lineSeparator()));
				}
				//If receive linux xmission, replace leading /
				if(filenamePath.toString().startsWith("/")) {
					filenamePath = Paths.get(filenamePath.toString().substring(1));
				}
				
				localFilePath = Paths.get(state.getRootPath().toString(), filenamePath.toString());
				//System.out.println("Local path: " + localFilePath.toString());
			//}
			try {
				//System.out.println(localFilePath.toAbsolutePath().toString());
				Files.createDirectories(localFilePath.getParent());
				output = Files.newOutputStream(localFilePath);
				state.setOs(output);
			}catch(IOException ex) {
				ex.printStackTrace();
				//TODO Handle
			}
		//Continue prior file from earlier
		}else {
			if(state.getBytesRemainingInCurrentFile() == content.length) {
				//System.out.println("Last segment of file");
				bytesToRead = (int) state.getBytesRemainingInCurrentFile();
				state.setBytesRemainingInCurrentFile(0);
				finishingFile = true;
			}else {
				//System.out.println("Additional segment of file");
				bytesToRead = content.length - (fileStartIdx);
				state.setBytesRemainingInCurrentFile(state.getBytesRemainingInCurrentFile() - bytesToRead);
			}
			output = state.getOs();
		}
		
		try {
			//System.out.println("Start: " + fileStartIdx);
			//System.out.println("Reading: " + bytesToRead);
			output.write(content, fileStartIdx, bytesToRead);
			output.flush();
			if(finishingFile) {
				output.close();
			}
		}catch(IOException ex) {
			//TODO handle
			ex.printStackTrace();
		}
	}
}
