package c2.win;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class CookiesCommandHelperTest {

	
	
	@Test
	void testChromeCookiesFileIsLegit() {
		assertTrue(Files.exists(Paths.get(CookiesCommandHelper.getChromeCookiesFilename())));
	}
	
	@Test
	void testFirefoxCookiesFileIsLegit() {
		assertTrue(Files.exists(Paths.get(CookiesCommandHelper.getFirefoxCookiesFilename())));
	}
	
	@Test
	void testEdgeCookiesFileIsLegit() {
		assertTrue(Files.exists(Paths.get(CookiesCommandHelper.getEdgeCookiesFilename())));
	}

}
