package util.test;

public class TestConfiguration {

	public final OS os;
	public final String lang;
	public final String protocol;

	public TestConfiguration(OS os, String lang, String protocol) {
		this.os = os;
		this.lang = lang;
		this.protocol = protocol;
	}

	private boolean execInRoot = true;
	private boolean testSecondaryClient = false;
	private boolean testTwoClients = false;
	private String serverConfigFile = "test.properties";
	private boolean isRemote = false;

	public boolean isExecInRoot() {
		return execInRoot;
	}

	public void setExecInRoot(boolean execInRoot) {
		this.execInRoot = execInRoot;
	}

	public boolean isTestSecondaryClient() {
		return testSecondaryClient;
	}

	public void setTestSecondaryClient(boolean testSecondaryClient) {
		this.testSecondaryClient = testSecondaryClient;
	}

	public boolean isTestTwoClients() {
		return testTwoClients;
	}

	public void setTestTwoClients(boolean testTwoClients) {
		this.testTwoClients = testTwoClients;
	}

	public String getServerConfigFile() {
		return serverConfigFile;
	}

	public void setServerConfigFile(String serverConfigFile) {
		this.serverConfigFile = serverConfigFile;
	}

	public boolean isRemote() {
		return isRemote;
	}

	public void setRemote(boolean isRemote) {
		this.isRemote = isRemote;
	}

	public enum OS {
		WINDOWS, LINUX
	};
}
