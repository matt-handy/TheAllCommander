package c2.win.services;

public class WindowsServiceInfo {

	public final String serviceName;
	public final String pathname;
	public final String executable;

	public WindowsServiceInfo(String serviceName, String pathname, String executable) {
		this.serviceName = serviceName;
		this.pathname = pathname;
		this.executable = executable;
	}
}
