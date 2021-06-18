package c2.smtp;

public class SimpleEmail {
	public final String sender;
	public final String subject;
	public final String body;
	
	public SimpleEmail(String sender, String subject, String body) {
		this.sender = sender;
		this.subject = subject;
		this.body = body;
	}
}
