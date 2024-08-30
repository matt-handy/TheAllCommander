package c2.session;

public class SessionAttributeDescriptor {

	public final String hostname;
	public final String username;
	public final String protocol;
	public final Boolean isElevated;
	
	public SessionAttributeDescriptor(String hostname, String username, String protocol, Boolean isElevated) {
		this.hostname = hostname;
		this.username = username;
		this.protocol = protocol;
		this.isElevated = isElevated;
	}
}
