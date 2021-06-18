package c2.file;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileHelperTest {

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test() {
		try {
			String hexValue = FileHelper.getFileAsHex("test" + File.separator + "data" + File.separator + "shellcode" + File.separator + "HelloWorld.shc.exe");
			assertTrue(hexValue.startsWith("4D5A"));
			assertTrue(hexValue.charAt(hexValue.length() - 1) == '0');
			assertTrue(hexValue.charAt(hexValue.length() - 2) == '0');
		}catch (IOException ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}

}
