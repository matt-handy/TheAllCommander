package c2.win.rdp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;

import util.test.RDPTest;
import util.test.TestConstants;

class PythonWindowsRDPTest extends RDPTest{

	@Test
	public static void test() {
		System.out.println("RDP Integration not automatically tested, future update pending");
		/*
		initiateServer();
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "httpsAgent.py\"";
		spawnClient(clientCmd);

		System.out.println("Transmitting commands");

		RDPTestRunner();
	*/
	}

}
