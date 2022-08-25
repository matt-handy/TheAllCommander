package util.test;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Time;
import util.test.TestConfiguration.OS;

public class ClientServerTest {

	protected static ExecutorService service;
	protected static ServerRunner runner;
	protected static ChildManager childManager;
	
	public static final String DEFAULT_SERVER_CONFIG = "test.properties";
	
	protected static void initiateServer() {
		initiateServer("test.properties");
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
		childManager = new ChildManager(clientStartArgs);
		
		service.submit(childManager);
		//Give time for the daemons to make contact, especially important for SMB
		Time.sleepWrapped(3000);
	}
	
	protected static void awaitClient() {
		childManager.awaitProcess();
	}
	
	protected static void teardown() {
		service.shutdownNow();
		runner.main.awaitFullShutdown();
		RunnerTestGeneric.cleanLogs();
	}
	
	protected static void executeStandardTest(String daemonLaunchArg, TestConfiguration config) {
		initiateServer();
		
		spawnClient(daemonLaunchArg);
		
		System.out.println("Transmitting commands");
		
		RunnerTestGeneric.test(config);
		
		teardown();
	}
}
