package c2.win.services;

import java.util.ArrayList;
import java.util.List;

public class WindowsServiceInfo {

	public final String serviceName;
	public final String pathname;
	public final String executable;
	private List<String> serviceArgs;

	public WindowsServiceInfo(String serviceName, String pathname, String executable, List<String> serviceArgs) {
		this.serviceName = serviceName;
		this.pathname = pathname;
		this.executable = executable;
		this.serviceArgs = new ArrayList<>();
		this.serviceArgs.addAll(serviceArgs);
	}

	public List<String> getServiceArgs() {
		return serviceArgs;
	}
}
