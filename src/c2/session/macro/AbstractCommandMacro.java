package c2.session.macro;

import c2.HarvestProcessor;
import c2.session.IOManager;

public abstract class AbstractCommandMacro {

	public abstract boolean isCommandMatch(String cmd);
	public abstract void initialize(IOManager io, HarvestProcessor harvestProcessor);
	public abstract MacroOutcome processCmd(String cmd, int sessionId, String sessionStr);

}
