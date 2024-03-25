package util.test;

import java.nio.file.Path;

public class PythonHostingTestFramework {

	private Process httpServerProcess;
	
	public void init(Path serverLocation) throws Exception {
		//Stand up HTTP server
		String pythonServerArgs[] = {"python", "-m", "http.server", "8011", "-d", serverLocation.toString()};
		httpServerProcess = Runtime.getRuntime().exec(pythonServerArgs);
		System.out.println("Python server started");
	}
	
	public void teardownTest(){
		httpServerProcess.destroyForcibly();
	}
	
}
