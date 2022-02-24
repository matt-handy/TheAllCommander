package util.test.socks5;

public interface TargetEmulator {
	public void awaitSocketClose();
	
	public boolean hasConnection();
}
