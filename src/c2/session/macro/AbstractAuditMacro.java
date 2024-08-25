package c2.session.macro;

public abstract class AbstractAuditMacro extends AbstractCommandMacro {

	
	protected void sendCommand(String cmd, int sessionId, MacroOutcome outcome) {
		io.sendCommand(sessionId, cmd);
	}
	
	protected String awaitResponse(int sessionId, MacroOutcome outcome) {
		String response = io.awaitMultilineCommands(sessionId);
		return response;
	}
	
	/**
	* Implementations of AbstractCommandMacro should log all commands and responses in the MacroOutcome object. 
	* This method wraps reception of output from the IOManager via the awaitMultilineCommands method. 
	*
	* @param  sessionId  The ID which can be used to send IO to and from the IOManager
	* @param  sessionStr The MacroOutcome object for logging 
	* @param  timeout The number of milliseconds which should be waited by awaitMultilineCommands
	* @return returns a String object containing any output received
	*/
	protected String awaitResponse(int sessionId, MacroOutcome outcome, int timeout) {
		String response = io.awaitMultilineCommands(sessionId, timeout);
		return response;
	}
}
