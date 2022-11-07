package util.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

public class TestCommons {

	public enum LANGUAGE {
		CPP, CSHARP, WINDOWS_NATIVE, PYTHON, JAVA
	};

	public static void cleanupExfil() {
		try {
			Path path = Paths.get("exfil");
			if (Files.exists(path)) {
				Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			}
		} catch (IOException e2) {
			fail("Cannot set up test and delete test log file");
		}
	}

	public static void cleanupKeylogger() {
		try {
			Files.deleteIfExists(Paths.get("test", InetAddress.getLocalHost().getHostName()));
		} catch (IOException e) {
			// e.printStackTrace();
		}
	}
	
	public static final Path HARVEST_LANDING_DIR = Paths.get("test", "fileReceiverTest");
	public static final String HARVEST_TEST_DIR = "harvest_test_source";
	
	public static void cleanFileHarvesterDir() {
		try (Stream<Path> walk = Files.walk(HARVEST_LANDING_DIR)) {
			walk.sorted(Comparator.reverseOrder()).forEach(TestCommons::deleteDirectory);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}

		try (Stream<Path> walk = Files.walk(Paths.get("test", HARVEST_TEST_DIR))) {
			walk.sorted(Comparator.reverseOrder()).forEach(TestCommons::deleteDirectory);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
	}
	
	static void deleteDirectory(Path path) {
		try {
			Files.delete(path);
		} catch (IOException e) {
			System.err.printf("Unable to delete this path : %s%n%s", path, e);
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

			if (Files.exists(Paths.get("test", "log"))) {
				Files.walk(Paths.get("test", "log")).sorted(Comparator.reverseOrder()).map(Path::toFile)
						.forEach(File::delete);
			}

		} catch (Exception ex) {
			// Don't worry about it
			// ex.printStackTrace();
		}

		cleanupKeylogger();
	}
}
