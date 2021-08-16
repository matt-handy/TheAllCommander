package c2;

public class Constants {
	public static final boolean DEBUG = false;
	
	public static final String COMMANDERPORT = "commander.port";
	
	public static final String DAEMONPORT = "daemon.port";
	public static final String DAEMONHTTPPORT = "daemon.http.port";
	public static final String DNSPORT = "daemon.dns.port";
	public static final String NATIVESHELLPORT = "daemon.nativeshell.port";
	
	public static final String DAEMON_EMAIL_PORT = "daemon.email.port";
	public static final String DAEMON_EMAIL_HOST = "daemon.email.host";
	public static final String DAEMON_EMAIL_USERNAME = "daemon.email.username";
	public static final String DAEMON_EMAIL_PASSWORD = "daemon.email.password";
	
	public static final String DAEMONCONTEXTCMD = "daemon.context.cmd";
	public static final String DAEMONCONTEXTEXFIL = "daemon.context.exfil";
	public static final String DAEMONCONTEXTLOGGER = "daemon.context.logger";
	public static final String DAEMONCONTEXTHARVEST = "daemon.context.harvest";
	public static final String DAEMONCONTEXTSCREENSHOT = "daemon.context.screenshot";
	public static final String DAEMONCONTEXTPAYLOAD = "daemon.context.payload";
	public static final String DAEMONCONTEXTHOLLOWERPAYLOAD = "daemon.context.hollower.payload";
	public static final String DAEMONCONTEXTCSHARPPAYLOAD = "daemon.context.csharp.payload";
	public static final String DAEMONCONTEXTGETSESSIONS = "daemon.context.getsessions";
	public static final String DAEMONCONTEXTCMDREST = "daemon.context.cmdrest";
	
	
	public static final String DAEMONLZLOGGER = "daemon.lz.logger";
	public static final String DAEMONLZEXFIL = "daemon.lz.exfil";
	public static final String DAEMONLZHARVEST = "daemon.lz.harvest";
			
	public static final String DAEMONPAYLOADDEFAULT = "daemon.payload.default";
	public static final String DAEMONPAYLOADHOLLOWERDEFAULT = "daemon.payload.hollower.default";
	public static final String DAEMONPAYLOADCSHARPDIR = "daemon.payload.csharp.dir";
	
	public static final String HUBLOGGINGPATH = "hub.logging.path";
	public static final String HUBCMDDEFAULTS = "hub.cmd.defaults";
	
	public static final String WIREENCRYPTTOGGLE = "wire.encrypt.toggle";
	public static final String WIREENCRYPTKEY = "wire.encrypt.key";
	
	public static final String COMMSERVICES = "commservices";
	public static final String MACROS = "macros";
	
	public static final String NEWLINE = "\n";
	
	public static final String PORT_FORWARD_NO_DATA = "<No Data>";
	
	private Constants() {
		
	}
	
	private static Constants theOnlyOne = null;
	
	public static Constants getConstants() {
		if(theOnlyOne == null) {
			theOnlyOne = new Constants();
		}
		return theOnlyOne;
	}
	
	public static final String DAEMONMAXRESPONSEWAIT = "daemon.maxresponsewait";
	public static final String DAEMONRESPONSEREPOLLINTERVAL = "daemon.responserepollinterval";
	public static final String DAEMONTEXTOVERTCPSTATICWAIT = "daemon.textovertcpstaticwait";
	
	public static final String EXPECTEDMAXCLIENTREPORTINGINTERVAL = "daemon.reportinginterval.expectedmaxclient";
	public static final String MULTIPLESEXPECTEDMAXCLIENTREPORTINGINTERVAL = "daemon.reportinginterval.multiplesexpectedmaxclient";
	
	//When looping to wait for response from client, wait no longer than...
	private int maxResponseWait = 10000;
	//When looping to wait for response from client, wait this long between polls
	private int repollForResponseInterval = 100;
	//When waiting a static interval for command action in synchronous text over
	//ip, wait this long...
	private int textOverTCPStaticWait = 500;

	private int expectedMaxClientReportingInterval = 2000;
	private int multiplesOfExpectedMaxClientReportingToWait = 5;
	
	public int getMaxResponseWait() {
		return maxResponseWait;
	}
	public int getRepollForResponseInterval() {
		return repollForResponseInterval;
	}
	public int getTextOverTCPStaticWait() {
		return textOverTCPStaticWait;
	}
	public void setMaxResponseWait(int maxResponseWait) {
		this.maxResponseWait = maxResponseWait;
	}
	public void setRepollForResponseInterval(int repollForResponseInterval) {
		this.repollForResponseInterval = repollForResponseInterval;
	}
	public void setTextOverTCPStaticWait(int textOverTCPStaticWait) {
		this.textOverTCPStaticWait = textOverTCPStaticWait;
	}
	public int getExpectedMaxClientReportingInterval() {
		return expectedMaxClientReportingInterval;
	}
	public void setExpectedMaxClientReportingInterval(int expectedMaxClientReportingInterval) {
		this.expectedMaxClientReportingInterval = expectedMaxClientReportingInterval;
	}
	public int getMultiplesOfExpectedMaxClientReportingToWait() {
		return multiplesOfExpectedMaxClientReportingToWait;
	}
	public void setMultiplesOfExpectedMaxClientReportingToWait(int multiplesOfExpectedMaxClientReportingToWait) {
		this.multiplesOfExpectedMaxClientReportingToWait = multiplesOfExpectedMaxClientReportingToWait;
	}
	
	
}
