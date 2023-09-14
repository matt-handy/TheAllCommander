package c2.win;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;

class WindowsProcessHelperTest {

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test() {
		if (TestConfiguration.getThisSystemOS() == OS.WINDOWS) {
			List<String> processes = WindowsCmdLineHelper.listRunningProcesses();
			for (String process : processes) {
				//TODO: Wow this is a shitty test. Need to write a better one
				assertTrue(process.length() != 0);
			}
		}
	}

}
