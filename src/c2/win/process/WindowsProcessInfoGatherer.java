package c2.win.process;

import java.util.ArrayList;
import java.util.List;

import c2.WindowsConstants;
import c2.session.IOManager;
import c2.win.WindowsToolOutputParseException;

public class WindowsProcessInfoGatherer {
	
	public static final String TASKLIST_CMD = "tasklist /FO csv /v";
	public static final String GET_MODULES_CMD = "powershell -c \"(Get-Process -Id $PROCESS_ID$).Modules\"";
	public static final String TAC_COMPLETE_ECHO = "echo TAC_COMPLETE_SCAN";
	
	public static List<WindowsProcessInfo> gatherWindowsProcessInfo(IOManager io, int sessionId, boolean skipCurrentUserAndSystemProcessDlls) throws WindowsToolOutputParseException{
		String sessionUsername = io.getSessionDescriptor(sessionId).username;
		String sessionHostname = io.getSessionDescriptor(sessionId).hostname;
		List<WindowsProcessInfo> processes = new ArrayList<>();
		
		io.sendCommand(sessionId, TASKLIST_CMD);
		io.sendCommand(sessionId, TAC_COMPLETE_ECHO);
		
		String output = io.awaitMultilineCommands(sessionId);
		while(output == null || !output.contains("TAC_COMPLETE_SCAN")) {
			String testOutput = io.awaitMultilineCommands(sessionId);
			if(testOutput != null) {
				output = output + WindowsConstants.WINDOWS_LINE_SEP + testOutput;
			}
		}
		String lines[] = output.split("\r\n");
		int startIdx = -1;
		String headerLine = null;
		for(int idx = 0; idx < lines.length; idx++) {
			headerLine = lines[idx];
			if(headerLine.startsWith("\"Image Name\"")) {
				startIdx = idx + 1;
				break;
			}
		}
		
		if(startIdx == -1) {
			throw new WindowsToolOutputParseException("Unable to parse tasklist CSV header, no header found in response");
		}
		
		String headerElements[] = headerLine.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
		//System.out.println("Header elements: " + headerLine);
		int nameIdx = -1;
		int pidIdx = -1;
		int usernameIdx = -1;
		for(int idx = 0; idx < headerElements.length; idx++) {
			String element = headerElements[idx];
			if(element.equalsIgnoreCase("\"Image Name\"")) {
				nameIdx = idx;
			}else if(element.equalsIgnoreCase("\"PID\"")) {
				pidIdx = idx;
			}else if(element.equalsIgnoreCase("\"User Name\"")) {
				usernameIdx = idx;
			}
		}
		
		if(nameIdx == -1 || pidIdx == -1 || usernameIdx == -1) {
			//System.out.println("output: " + output);
			throw new WindowsToolOutputParseException("Unable to parse tasklist CSV header, improperly formatted response");
		}
		//System.out.println("Have a good index list for tasklist");
		for(int idx = startIdx; idx < lines.length; idx++) {
			String line = lines[idx].trim();
			//System.out.println("Considering line: " + line);
			if(!line.equals("") && !line.contains("TAC_COMPLETE_SCAN")) {
				//System.out.println("Valid Line");
				String elements[] = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
				//System.out.println(elements.length);
				if(nameIdx > elements.length || pidIdx > elements.length || usernameIdx > elements.length) {
					System.out.println("Output bad!");
					throw new WindowsToolOutputParseException("Unable to parse tasklist CSV entries");
				}
				String name = elements[nameIdx].replace("\"", ""); 
				int pid = Integer.parseInt(elements[pidIdx].replace("\"", ""));
				String username = elements[usernameIdx].replace("\"", "");
				//System.out.println("Name; "+ name);
				//Username may be either blank username or qualified with hostname
				//TODO: account for domain users
				if(username.equals("N/A") || (username.equalsIgnoreCase(sessionUsername) || username.equalsIgnoreCase(new String(sessionHostname + "\\" + sessionUsername))) || pid == 0) {
					//System.out.println("Adding process no mods");
					processes.add(new WindowsProcessInfo(name, pid, username, new ArrayList<>()));
				}else {
					//System.out.println("Need modules");
					List<WindowsModule> modules = new ArrayList<>();
					String commandForModules = GET_MODULES_CMD.replace("$PROCESS_ID$", pid + "");
					//System.out.println("Getting modules for process: " + name + " for user " + username);
					io.sendCommand(sessionId, commandForModules);
					
					String modulesRaw = io.awaitMultilineCommands(sessionId);
					//System.out.println("Modules: " + modulesRaw);
					if(modulesRaw.contains("Get-Process : Cannot find a process with the process") ||
							modulesRaw.replace("\r", "").replace("\n", "").trim().equals("")) {
						continue; //This process died since the tasklist query if there is the Get-Process error, or had no modules if blank return
					}
					String modulesLines[] = modulesRaw.split("\r\n");
					int startParseIdx = -1;
					int moduleNameStartIdx = -1;
					int pathStartIdx = -1;
					for(int jdx = 0; jdx < modulesLines.length; jdx++) {
						String moduleLine = modulesLines[jdx];
						if(moduleLine.contains("ModuleName") && moduleLine.contains("FileName")) {
							startParseIdx = jdx + 2;
							moduleNameStartIdx = moduleLine.indexOf("ModuleName");
							pathStartIdx = moduleLine.indexOf("FileName");
							break;
						}
					}
					if(startParseIdx == -1) {
						//System.out.println("Raw module return: " + modulesRaw);
						throw new WindowsToolOutputParseException("Unable to parse process module entries: " + pid);
					}
					for(int jdx = startParseIdx; jdx < modulesLines.length; jdx++) {
						String moduleLine = modulesLines[jdx];
						//System.out.println("Examining: " + moduleLine);
						if(moduleNameStartIdx < moduleLine.length() && pathStartIdx < moduleLine.length()) {
							String moduleStr = moduleLine.substring(moduleNameStartIdx, pathStartIdx).trim();
							String pathStr = moduleLine.substring(pathStartIdx).trim();
							System.out.println("Mod: " + moduleStr);
							modules.add(new WindowsModule(moduleStr, pathStr));
						}//else - its not a module line
					}
					processes.add(new WindowsProcessInfo(name, pid, username, modules));
				}
			}
		}
		//System.out.println("Done with tasklist scan");
		return processes;
	}
}
