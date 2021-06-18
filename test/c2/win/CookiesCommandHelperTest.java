package c2.win;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class CookiesCommandHelperTest {

	private static String getFirefoxCookieFilenameWithAppdata() {
		String firefoxProfileRoot = stripQuotesAndReplaceAppdata(CookiesCommandHelper.FIREFOX_COOKIES_ROOT);
		File[] directories = new File(firefoxProfileRoot).listFiles(File::isDirectory);
		assertEquals(directories.length, 1);
		return CookiesCommandHelper.FIREFOX_COOKIES_ROOT + "\\" + directories[0].getName() + "\\" + CookiesCommandHelper.FIREFOX_COOKIES_FILENAME;
	}
	
	private static String stripQuotesAndReplaceAppdata(String target) {
		String targetFilename = target.replaceAll("\"", "");
		String appData = System.getenv().get("APPDATA");
		targetFilename = targetFilename.replace("%APPDATA%", appData);
		return targetFilename;
	}
	
	public static String getChromeCookiesFilename() {
		return stripQuotesAndReplaceAppdata(CookiesCommandHelper.CHROME_COOKIES_FILENAME);
	}
	
	public static String getFirefoxCookiesFilename() {
		return stripQuotesAndReplaceAppdata(getFirefoxCookieFilenameWithAppdata());
	}
	
	public static String getEdgeCookiesFilename() {
		return stripQuotesAndReplaceAppdata(CookiesCommandHelper.EDGE_CHROMIUM_FILENAME);
	}
	
	@Test
	void testChromeCookiesFileIsLegit() {
		assertTrue(Files.exists(Paths.get(getChromeCookiesFilename())));
	}
	
	@Test
	void testFirefoxCookiesFileIsLegit() {
		assertTrue(Files.exists(Paths.get(getFirefoxCookiesFilename())));
	}
	
	@Test
	void testEdgeCookiesFileIsLegit() {
		assertTrue(Files.exists(Paths.get(getEdgeCookiesFilename())));
	}

}
