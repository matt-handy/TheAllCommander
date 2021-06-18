package c2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.junit.jupiter.api.Test;

import c2.session.SessionInitiator;
import util.Time;
import util.test.ClientServerTest;
import util.test.TestConstants;

class CommanderInterfaceTest extends ClientServerTest {

	@Test
	void testLocal() {
		test();
	}
	
	public static void test() {
		initiateServer();
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "httpsAgent.py\"";
		spawnClient(clientCmd);
		System.out.println("Transmitting commands");
		
		Time.sleepWrapped(5000);
		
		try {
			System.out.println("Connecting test commander...");
			Socket remote = new Socket("localhost", 8111);
			System.out.println("Locking test commander streams...");
			OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));
			
			RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, false, false);
			
			bw.write("quit_session" + System.lineSeparator());
			bw.flush();
			
			RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, false, false);
			
			bw.write("list_sessions" + System.lineSeparator());
			bw.flush();
			RunnerTestGeneric.validateTwoSessionBanner(remote, bw, br, false, 2, false);
			
			//Test killing a session. First the session will disconnect, see that it isn't there, then 
			//reconnect and tell it to "die"
			bw.write("kill_session 2" + System.lineSeparator());
			bw.flush();
			
			String output = br.readLine();
			assertEquals(output, SessionInitiator.AVAILABLE_SESSION_BANNER);
			output = br.readLine();
			assertEquals(output, "1:default");
			bw.write("2" + System.lineSeparator());
			bw.flush();
			output = br.readLine();
			assertEquals(output, "Invalid Session id, will continue with prior session id.");
			bw.close();
			br.close();
			remote.close();
			
			//Wait long enough for a reconnect
			Time.sleepWrapped(2000);
			System.out.println("Connecting test commander...");
			remote = new Socket("localhost", 8111);
			System.out.println("Locking test commander streams...");
			bw = new OutputStreamWriter(remote.getOutputStream());
			br = new BufferedReader(new InputStreamReader(remote.getInputStream()));
			
			RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, false, 3, false);
			bw.write("list_sessions" + System.lineSeparator());
			bw.flush();
			RunnerTestGeneric.validateTwoSessionBanner(remote, bw, br, false, 3, false);
			
			bw.write("die" + System.lineSeparator());
			bw.flush();
			
			Time.sleepWrapped(3000);
			
			bw.close();
			br.close();
			remote.close();
			
			
		}catch(Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
		
		Time.sleepWrapped(500);
		
		teardown();
	}

}
