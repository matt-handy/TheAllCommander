package c2.session;

public class CommandPreprocessorOutcome {

	public final String message;
	public final boolean outcome;
	public final boolean sendingCmdToClient;
	
	public CommandPreprocessorOutcome(String message, boolean outcome) {
		this.message = message;
		this.outcome = outcome;
		sendingCmdToClient = true;
	}
	
	public CommandPreprocessorOutcome(String message, boolean outcome, boolean sendingCmdToClient) {
		this.message = message;
		this.outcome = outcome;
		this.sendingCmdToClient = sendingCmdToClient;
	}
	
	public CommandPreprocessorOutcome(boolean outcome) {
		this.message = "Success";
		this.outcome = outcome;
		sendingCmdToClient = true;
	}
	
	
}
