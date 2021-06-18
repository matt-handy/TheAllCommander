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
	
	public static String resolveAppData(IOManager io, int sessionId) throws Exception{
		String queryDirCmd = "dir %APPDATA%";
		io.sendCommand(sessionId, queryDirCmd);
		Time.sleepWrapped(2000);
		String dirResponse = io.readAllMultilineCommands(sessionId);
		
		String lines[] = dirResponse.split(System.lineSeparator());
		if(lines.length < 4) {
			throw new Exception("Cannot get APPDATA directory");
		}
		return lines[3].substring(" Directory of ".length());
	}
}
