package c2.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Date;

import c2.http.HTTPSManager;

public class ScreenshotHelper {
	public static boolean saveScreenshot(String b64Screenshot, String srcHostname, String srcUsername, String lz) {
		try {
			byte[] data = Base64.getDecoder().decode(b64Screenshot);
			String localPath = srcHostname + "-screen" + File.separator + srcUsername;
			Files.createDirectories(Paths.get(lz + File.separator + localPath));
			try (OutputStream stream = new FileOutputStream(lz + File.separator + localPath + File.separator
					+ HTTPSManager.ISO8601_WIN.format(new Date()) + ".png")) {
				stream.write(data);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}
}
