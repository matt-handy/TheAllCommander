package util.test.socks5;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Time;
import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;

public class TestProcessor extends ClientServerTest {

	public static void testDaemonConnection(String clientStartArg, boolean testBreak, boolean isEmailTest) {
		if (System.getProperty("os.name").contains("Windows")) {
			initiateServer();
		} else {
			initiateServer("test_linux.properties");
		}
		spawnClient(clientStartArg);
		Time.sleepWrapped(6000);
		ExecutorService service = Executors.newCachedThreadPool();
		
		try {			
			//System.out.println("Connecting test commander...");
			Socket remote = new Socket("localhost", 8111);
			System.out.println("Locking test commander streams...");
			OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));
			
			RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, false, false);
		
			Random rnd = new Random();
			int socksPort = 40000 + rnd.nextInt(1000);
			int targetServicePort = 40000 + rnd.nextInt(1000);
			
			bw.write("startSocks5 " + socksPort + System.lineSeparator());
			bw.flush();
			//br.readLine();//Proxy is started

			TargetServerEmulator targetService = new TargetServerEmulator(targetServicePort, testBreak);
			service.submit(targetService);
			targetService.awaitStartup();
			
			SocksClientEmulator clientEmulator = new SocksClientEmulator(socksPort, true, null, testBreak, isEmailTest, targetServicePort);
			service.submit(clientEmulator);
			//System.out.println("Awaiting client end");
			assertTrue(clientEmulator.isComplete());
			//System.out.println("Client ended");
			
			bw.write("killSocks5" + System.lineSeparator());
			bw.write("die" + System.lineSeparator());
			bw.flush();
			remote.close();
			awaitClient();
		}catch(Exception ex) {
			fail(ex.getMessage());
		}
		
		teardown();
	}
}
