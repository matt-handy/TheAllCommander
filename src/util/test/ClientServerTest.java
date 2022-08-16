package util.test;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Time;
import util.test.TestConfiguration.OS;

public class ClientServerTest {

	protected static ExecutorService service;
	protected static ServerRunner runner;
	
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
	
	protected static void spawnClient(String clientStartArgs) {
		Runnable runner2 = new Runnable() {
			@Override
			public void run() {
				try {
					Process process = Runtime.getRuntime()
							.exec(clientStartArgs);
					process.waitFor();
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}

			}
		};

		service.submit(runner2);
		//Give time for the daemons to make contact, especially important for SMB
		Time.sleepWrapped(3000);
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
