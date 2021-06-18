package c2.remote;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import util.Time;

class RemoteTestExecutorTest {

	public static final int TEST_PORT = 1005;

	@Test
	void test() {
		ExecutorService service = Executors.newFixedThreadPool(4);

		Runnable runner2 = new Runnable() {
			@Override
			public void run() {
				RemoteTestExecutor exec = new RemoteTestExecutor();
				exec.runTestServer("localhost", TEST_PORT);
			}
		};
		service.execute(runner2);
		
		Time.sleepWrapped(1000);
		
		RemoteTestExecutor exec = new RemoteTestExecutor();
		exec.startTestProgram(TEST_PORT, "calc.exe");
	}

}
