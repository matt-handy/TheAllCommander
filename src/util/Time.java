package util;

public class Time {
	public static void sleepWrapped(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			
		}
	}
}
