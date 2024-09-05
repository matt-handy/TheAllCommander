package c2.session.macro;

public abstract class AbstractAuditMacro extends AbstractCommandMacro {

	@Override
	protected void sendCommand(String cmd, int sessionId, MacroOutcome outcome) {
		io.sendCommand(sessionId, cmd);
	}
	
	@Override
	protected String awaitResponse(int sessionId, MacroOutcome outcome) {
		String response = io.awaitMultilineCommands(sessionId);
		return response;
	}
	
	@Override
	protected String awaitResponse(int sessionId, MacroOutcome outcome, int timeout) {
		String response = io.awaitMultilineCommands(sessionId, timeout);
		return response;
	}
}
