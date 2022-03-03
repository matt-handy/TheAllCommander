package c2.remote;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import util.Time;

class RemoteTestExecutorTest {

	@Test
	void test() {
		//TODO: This was a stupid test, need to beef it up to be valuable.
		/*
		Random rnd = new Random();
		int testPort = 40000 + rnd.nextInt(1000);
		ExecutorService service = Executors.newFixedThreadPool(4);

		Runnable runner2 = new Runnable() {
			@Override
			public void run() {
				RemoteTestExecutor exec = new RemoteTestExecutor();
				exec.runTestServer("localhost", testPort);
			}
		};
		service.execute(runner2);
		
		Time.sleepWrapped(1000);
		
		RemoteTestExecutor exec = new RemoteTestExecutor();
		exec.startTestProgram(testPort, "calc.exe");
		*/
	}

}
