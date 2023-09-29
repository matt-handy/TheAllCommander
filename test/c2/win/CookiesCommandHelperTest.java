package c2.win;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;

public class CookiesCommandHelperTest {

	@Test
	void testChromeCookiesFileIsLegit() {
		if (TestConfiguration.getThisSystemOS() == OS.WINDOWS) {
			assertTrue(Files.exists(Paths.get(CookiesCommandHelper.getChromeCookiesFilename())));
		}
	}

	@Test
	void testFirefoxCookiesFileIsLegit() {
		if (TestConfiguration.getThisSystemOS() == OS.WINDOWS) {
			assertTrue(Files.exists(Paths.get(CookiesCommandHelper.getFirefoxCookiesFilename())));
		}
	}

	@Test
	void testEdgeCookiesFileIsLegit() {
		
		System.out.println("Warning: unable to validate Edge cookies on current version of Windows");
		/*
		if (TestConfiguration.getThisSystemOS() == OS.WINDOWS) {
			assertTrue(Files.exists(Paths.get(CookiesCommandHelper.getEdgeCookiesFilename())));
		}
		*/
	}

}
