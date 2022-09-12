package util.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Properties;

import c2.smtp.EmailHandler;
import c2.smtp.SimpleEmail;

public class EmailHelper {
	public static Properties setup() {
		return ClientServerTest.getDefaultSystemTestProperties();
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
