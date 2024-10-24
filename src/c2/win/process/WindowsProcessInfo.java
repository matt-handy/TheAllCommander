package c2.win.process;

import java.util.ArrayList;
import java.util.List;

public class WindowsProcessInfo {

	public final String name;
	public final int pid;
	public final String username;
	public List<WindowsModule> modules = new ArrayList<>();
	
	public WindowsProcessInfo(String name, int pid, String username, List<WindowsModule> modules) {
		this.name = name;
		this.pid = pid;
		this.username = username;
		this.modules.addAll(modules);
	}

	public List<WindowsModule> getModules() {
		return new ArrayList<>(modules);
	}
	
}
