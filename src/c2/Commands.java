package c2;

public class Commands {
	public static String PWD = "pwd";
	public static String CD = "cd";
	
	public static String SHUTDOWN_DAEMON = "die";
	
	public static String HARVEST_CURRENT_DIRECTORY = "harvest_pwd";
	
	public static String OS_HERITAGE = "os_heritage";
	public static String OS_HERITAGE_RESPONSE_WINDOWS = "Windows";
	public static String OS_HERITAGE_RESPONSE_LINUX = "Linux";
	public static String OS_HERITAGE_RESPONSE_MAC = "Mac";
	
	public static final String CLIENT_GET_EXE_CMD = "get_daemon_start_cmd";
	
	public static final String SERVER_CMD_LIST_ALL_MACROS = "list_macros";
	
	public static final String SESSION_START_OBFUSCATE_WINDOWS_COMMAND = "<start esc>";
	public static final String SESSION_END_OBFUSCATE_WINDOWS_COMMAND = "<end esc>";
	public static final String SESSION_OBFUSCATE_WINDOWS_COMMAND_PREFIX = "<esc> ";
	
	public static final String SESSION_START_OBFUSCATED_POWERSHELL_MODE = "<ps-start>";
	public static final String SESSION_END_OBFUSCATED_POWERSHELL_MODE = "<ps-end>";
}
