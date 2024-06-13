package util;

import java.security.SecureRandom;

public class RandomHelper {

	public static String generateRandomLowercaseString() {
		SecureRandom rnd = new SecureRandom();
		int leftLimit = 97; // letter 'a'
	    int rightLimit = 122; // letter 'z'
	    int targetStringLength = 3 + rnd.nextInt(10);

	    String newString = rnd.ints(leftLimit, rightLimit + 1)
	  	      .limit(targetStringLength)
		      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
		      .toString(); 
	    return newString;
	}
}
