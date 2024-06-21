package c2.session.macro.uac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;
import util.test.TestConstants;
import util.test.uac.CtsmpTestHelper;

class CmstpUacBypasserMacroTest extends CtsmpTestHelper {

	@AfterEach
	void clean() {
		teardown();
	}
	
	@Test
	void testDetectsCommand() {
		CmstpUacBypasserMacro macro = new CmstpUacBypasserMacro();
		assertTrue(macro.isCommandMatch(CmstpUacBypasserMacro.CMD));
		assertTrue(macro.isCommandMatch(CmstpUacBypasserMacro.CMD + " barf.exe"));
		assertFalse(macro.isCommandMatch("Something else"));
	}

	@Test
	void testEndToEndWithPythonHttpsExplicitPath(){
		if(TestConfiguration.getThisSystemOS() == OS.WINDOWS && TestConstants.UAC_CMSTP_TEST_ENABLE) {
			//Execute the command with an explicit python invocation command as an argument
			testCmstpEndToEndBody("python.exe " + Paths.get("agents", "python", "httpsAgent.py").toAbsolutePath(), TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);
		}
	}

	@Test
	void testEndToEndWithPythonAutoDetectedPath(){
		if(TestConfiguration.getThisSystemOS() == OS.WINDOWS && TestConstants.UAC_CMSTP_TEST_ENABLE) {
			//Execute the command without an explicit python invocation command as an argument
			testCmstpEndToEndBody(null, TestConstants.PYTHON_HTTPSDAEMON_TEST_EXE);
		}
	}

	

}