package c2.session.macro;

import java.util.ArrayList;
import java.util.List;

public class MacroOutcome {
	
	private List<String> errors = new ArrayList<>();
	private List<String> outputLines = new ArrayList<>();
	
	public boolean hasErrors() {
		return !errors.isEmpty();
	}
	
	public List<String> getErrors(){
		return errors;
	}
	
	public List<String> getOutput(){
		return outputLines;
	}

	public void addError(String error) {
		errors.add(error);
	}
	
	public void addSentCommand(String command) {
		outputLines.add("Sent Command: '" + command + "'");
	}
	
	public void addResponseIo(String response) {
		outputLines.add("Received response: '" + response + "'");
	}
	
	public void addMacroMessage(String message) {
		outputLines.add("Macro Executor: '" + message + "'");
	}
}
