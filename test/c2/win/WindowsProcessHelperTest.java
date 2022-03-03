package c2.win;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WindowsProcessHelperTest {

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test() {
		if (System.getProperty("os.name").contains("Windows")) {
			List<String> processes = WindowsCmdLineHelper.listRunningProcesses();
			for (String process : processes) {
				assertTrue(process.length() != 0);
			}
		}
	}

}
