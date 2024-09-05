package c2.win;

public class WindowsUserPriviledgeParser {
	
	public static final String QUERY = "whoami /priv";
	
	public static final String SE_IMPERSONATE = "SeImpersonatePrivilege";
	
	public static final String ENABLED = "Enabled";
	
	private boolean hasSeImpersonate = false;
	
	public WindowsUserPriviledgeParser(String privRaw) throws WindowsToolOutputParseException{
		String[] permissions = privRaw.split("\r\n");
		for(String perm : permissions) {
			if(perm.contains(SE_IMPERSONATE) && perm.contains(ENABLED)) {
				hasSeImpersonate = true;
			}
		}
	}
	
	private WindowsUserPriviledgeParser() {}

	public boolean isHasSeImpersonate() {
		return hasSeImpersonate;
	}

	public void setHasSeImpersonate(boolean hasSeImpersonate) {
		this.hasSeImpersonate = hasSeImpersonate;
	}
	
	
}
