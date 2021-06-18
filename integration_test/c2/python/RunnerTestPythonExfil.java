package c2.python;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import c2.Runner;
import util.test.TestCommons;

class RunnerTestPythonExfil {

	//TODO: Retire this test with project 5.1
	
	
	ExecutorService service = Executors.newFixedThreadPool(4);
	Properties prop;
	
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
	
	Process process;
	
	@BeforeEach
	void setUp() throws Exception {
		TestCommons.pretestCleanup();
		
		Runnable runner1 = new Runnable() {

			@Override
			public void run() {
				try (InputStream input = new FileInputStream("test" + File.separator + "test.properties")) {

					prop = new Properties();

					// load a properties file
					prop.load(input);

					Runner main = new Runner();
					main.engage(prop);

				} catch (IOException ex) {
					System.out.println("Unable to load config file");
					fail(ex.getMessage());
				}
			}

		};
		service.submit(runner1);
		
		Runnable runner2 = new Runnable() {
			@Override
			public void run() {
				try {
					process = Runtime.getRuntime()
							.exec("python localAgent" + File.separator + "python_exfiltrator" + File.separator
									+ "base64Sender.py");
					process.waitFor();
				} catch (IOException | InterruptedException e) {
					fail(e.getMessage());
				}

			}
		};

		service.submit(runner2);
	}

	@AfterEach
	void tearDown() throws Exception {
		service.shutdownNow();
		process.destroyForcibly();
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		cleanupExfil();
		
	}

	@Test
	void test() {
		
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			assertTrue(Files.exists(Paths.get("exfil", InetAddress.getLocalHost().getHostName() + System.getProperty("user.name"), System.getProperty("user.name"), "OneDrive", "Documents", "Archive Index - Legacy DVDs.txt")));
			assertTrue(Files.exists(Paths.get("exfil", InetAddress.getLocalHost().getHostName() + System.getProperty("user.name"), System.getProperty("user.name"), "OneDrive", "Documents", "Appliance Fixture and Device Manuals", "3DS XL Operations Manual.pdf")));
		} catch (UnknownHostException e) {
			fail(e.getMessage());
		}
	}

}
