package util.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import c2.smtp.EmailHandler;
import c2.smtp.SimpleEmail;

public class EmailHelper {
	public static Properties setup() {
		try (InputStream input = new FileInputStream("config" + File.separator + "test.properties")) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			return prop;
		} catch (IOException ex) {
			System.out.println("Unable to load config file");
			fail(ex.getMessage());
			return null;
		}
	}
	
	public static void flushC2Emails() {
		EmailHandler receiveHandler = new EmailHandler();
		try {
			receiveHandler.initialize(null, setup(), null, null);
		} catch (Exception ex) {
			fail(ex.getMessage());
		}
		
		SimpleEmail email = receiveHandler.getNextMessage();
		while(email != null) {
			email = receiveHandler.getNextMessage();
		}
	}
}
