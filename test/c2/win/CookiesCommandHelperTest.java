package c2.win;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class CookiesCommandHelperTest {

	@Test
	void testChromeCookiesFileIsLegit() {
		if (System.getProperty("os.name").contains("Windows")) {
			assertTrue(Files.exists(Paths.get(CookiesCommandHelper.getChromeCookiesFilename())));
		}
	}

	@Test
	void testFirefoxCookiesFileIsLegit() {
		if (System.getProperty("os.name").contains("Windows")) {
			assertTrue(Files.exists(Paths.get(CookiesCommandHelper.getFirefoxCookiesFilename())));
		}
	}

	@Test
	void testEdgeCookiesFileIsLegit() {
		if (System.getProperty("os.name").contains("Windows")) {
			assertTrue(Files.exists(Paths.get(CookiesCommandHelper.getEdgeCookiesFilename())));
		}
	}

}
