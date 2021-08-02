package c2.session;

public class CommandPreprocessorOutcome {

	public final String message;
	public final boolean outcome;
	
	public CommandPreprocessorOutcome(String message, boolean outcome) {
		this.message = message;
		this.outcome = outcome;
	}
	
	public CommandPreprocessorOutcome(boolean outcome) {
		this.message = "Success";
		this.outcome = outcome;
	}
}
