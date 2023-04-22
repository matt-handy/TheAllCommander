package c2.session;

public class PowershellWrapperObfuscator {
	public static String process(String cmd) {
		return "echo " + cmd + " | powershell";
	}
}
