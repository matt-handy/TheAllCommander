package util.test.uac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import c2.Commands;
import c2.admin.LocalConnection;
import c2.session.macro.uac.CmstpUacBypasserMacro;
import util.Time;
import util.test.ClientServerTest;
import util.test.OutputStreamWriterHelper;
import util.test.RunnerTestGeneric;
import util.test.TestConstants;

public class CtsmpTestHelper extends ClientServerTest {

	public void testCmstpEndToEndBody(String invoke, String daemonSpawnInvoke) {
		initiateServer();
		spawnClient(daemonSpawnInvoke);
		try {
			// Connect to server and order the macro to fire.
			Socket remote = LocalConnection.getSocket("127.0.0.1", 8012,
					ClientServerTest.getDefaultSystemTestProperties());
			OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));
			RunnerTestGeneric.connectionSetupGeneric(remote, bw, br, false, false);
			String invocation = CmstpUacBypasserMacro.CMD;
			if (invoke != null) {
				invocation += " " + invoke;
			}
			OutputStreamWriterHelper.writeAndSend(bw, invocation);

			// Iterate over outcome comments
			if (invoke == null) {
				assertEquals("Sent Command: '" + Commands.CLIENT_CMD_GET_EXE + "'", br.readLine());
				assertTrue(br.readLine().startsWith("Received response: '"));
				assertEquals("", br.readLine());
				assertEquals("'", br.readLine());
			}
			String line = br.readLine();
			assertTrue(line.startsWith("Sent Command: '<control> download C:\\Windows\\Tasks\\"));
			assertTrue(br.readLine().startsWith("Received response: 'File written: C:\\Windows\\Tasks\\"));
			assertEquals("", br.readLine());
			assertEquals("'", br.readLine());
			assertTrue(br.readLine().startsWith("Sent Command: '<control> download C:\\Windows\\Tasks\\"));
			assertTrue(br.readLine().startsWith("Received response: 'File written: C:\\Windows\\Tasks\\"));
			assertEquals("", br.readLine());
			assertEquals("'", br.readLine());
			assertTrue(br.readLine().startsWith("Sent Command: 'powershell C:\\Windows\\Tasks\\"));
			assertEquals("Macro Executor: 'New elevated session available: 3'", br.readLine());
			line = br.readLine();
			assertTrue(line.startsWith("Sent Command: 'del C:\\Windows\\Tasks\\"));
			line = br.readLine();
			assertTrue(line.startsWith("Sent Command: 'del C:\\Windows\\Tasks\\"));
			assertEquals("Received response: ''", br.readLine());
			OutputStreamWriterHelper.writeAndSend(bw, Commands.CLIENT_CMD_SHUTDOWN_DAEMON);

			// Instruct second daemon to die77
			OutputStreamWriterHelper.writeAndSend(bw, "quit_session");
			OutputStreamWriterHelper.writeAndSend(bw, 3 + "");
			OutputStreamWriterHelper.writeAndSend(bw, Commands.CLIENT_CMD_SHUTDOWN_DAEMON);
			// We should clean up product files
			for (Path taskFile : listWindowsTasksDir()) {
				assertFalse(taskFile.toString().endsWith(".ps1"));
				assertFalse(taskFile.toString().endsWith(".inf"));
			}
			
			//Give time for the clients to get the death order
			Time.sleepWrapped(1000);

		} catch (Exception ex) {
			fail(ex.getMessage());
		}
	}

	private Set<Path> listWindowsTasksDir() throws IOException {
		try (Stream<Path> stream = Files.list(Paths.get("C:", "Windows", "Tasks"))) {
			return stream.filter(file -> !Files.isDirectory(file)).map(Path::getFileName).collect(Collectors.toSet());
		}
	}
}
