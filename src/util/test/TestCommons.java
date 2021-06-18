package util.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class TestCommons {

	public enum LANGUAGE {CPP, CSHARP, WINDOWS_NATIVE};
	
	public static void cleanupExfil() {
		try {
			Path path = Paths.get("exfil");
			if(Files.exists(path)) {
				Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			}
			System.out.println("PYTHON EXFIL TEST: Deleted!");
		} catch (IOException e2) {
			fail("Cannot set up test and delete test log file");
		}
	}
	
	public static void cleanupKeylogger() {
		try {
			Files.deleteIfExists(Paths.get("test", InetAddress.getLocalHost().getHostName()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void pretestCleanup() {
		cleanupExfil();

		// C# shutdown cleanup
		try {
			Files.deleteIfExists(Paths.get("System.Net.Sockets.SocketException"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		// purge logging directory
		try {

			for (File file : Paths.get("test", "log").toFile().listFiles()) {
				if (!file.isDirectory()) {
					file.delete();
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		cleanupKeylogger();
	}
}
