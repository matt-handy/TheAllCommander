package c2.session;

import java.security.SecureRandom;
import java.util.Random;

public class WindowsCarrotObfuscation {

	public static String obfuscate(String command) {
		Random rnd = new SecureRandom();
		int numberOfRounds = 1 + rnd.nextInt(10);
		for(int idx = 0; idx < numberOfRounds; idx++) {
			int targetIdx = rnd.nextInt(command.length() - 1);
			command = command.substring(0, targetIdx) + "^" + command.substring(targetIdx);
		}
		return command;
	}
}
