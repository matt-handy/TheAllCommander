package c2.session.macro.enumeration;

import java.util.List;

import c2.session.macro.AbstractAuditMacro;
import c2.session.macro.MacroOutcome;

public class AuditAllTheThingsMacro extends AbstractAuditMacro{

	public static final String CMD = "audit_all_the_things";
	
	private List<AbstractAuditMacro> audits;
	
	public AuditAllTheThingsMacro(List<AbstractAuditMacro> audits) {
		this.audits = audits;
	}
	
	@Override
	public String getReadableName() {
		return "Audit All The Things";
	}

	@Override
	public String getInvocationCommandDescription() {
		return CMD;
	}

	@Override
	public String getBehaviorDescription() {
		return "This macro runs all of TheAllCommander's integrated audits in sequence";
	}

	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equalsIgnoreCase(CMD);
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		MacroOutcome outcome = new MacroOutcome();
				
		for(AbstractAuditMacro macro : audits) {
			//System.out.println("Firing : " + macro.getReadableName());
			MacroOutcome newOutcome = macro.processCmd(cmd, sessionId, sessionStr);
			//System.out.println("Errors: " + newOutcome.getErrors().size());
			outcome.appendMacro(newOutcome);
		}
		
		System.out.println("Main Errors: " + outcome.getErrors().size());
		return outcome;
	}

}
