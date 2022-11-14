package util.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Time;
import util.test.TestConfiguration.OS;

public class ClientServerTest {

	protected static ExecutorService service;
	protected static ServerRunner runner;
	protected static List<ChildManager> childManagers = new ArrayList<>();
	//protected static ChildManager childManager;
	
	public static final String DEFAULT_SERVER_CONFIG = "test.properties";
	
	public static Properties getDefaultSystemTestProperties() {
		String configName = "config" + File.separator;
		if(System.getProperty("os.name").contains("Windows")) {
			configName += "test.properties";
		}else {
			configName += "test_linux.properties";
		}
		
		try (InputStream input = new FileInputStream(configName)) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			return prop;
		} catch (IOException ex) {
			System.out.println("Unable to load config file");
			fail(ex.getMessage());
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
		private Process process = null;
			
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
		service.shutdownNow();
		runner.main.awaitFullShutdown();
		RunnerTestGeneric.cleanLogs();
	}
	
	protected static void executeStandardTest(String daemonLaunchArg, TestConfiguration config) {
		initiateServer();
		spawnClient(daemonLaunchArg);
		RunnerTestGeneric.test(config);
		awaitClient();
		teardown();
	}
}
