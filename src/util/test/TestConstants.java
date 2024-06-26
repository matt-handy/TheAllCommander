package util.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TestConstants {
	private static final String I_EXECUTIONROOT_REMOTE = "executionroot.remote";
	
	private static final String I_TMP_DIR = "tmp.dir";
	private static final String I_TMP_CHROME_COOKIES = "tmp.chrome.cookies";
	private static final String I_TMP_FIREFOX_COOKIES = "tmp.firefox.cookies";
	private static final String I_TMP_EDGE_COOKIES = "tmp.edge.cookies";
	private static final String I_TMP_GENERIC = "tmp.generic";
	
	//We want this to be visible to read in warning messages
	public static final String I_OUTLOOKHARVEST_LIVE_ENABLE = "outlookharvest.live.enable";
	public static final String I_LARGE_HARVEST_TEST_ENABLE = "largeharvest.test.enable";
	public static final String I_KEYLOGGER_TEST_ENABLE = "keylogger.test.enable";
	public static final String I_WINLOGON_REG_LIVE_DENIAL_ENABLE = "winlogon.enable";
	public static final String I_UAC_CMSTP_TEST_ENABLE = "uac.cmstp.test.enable";
	
	private static final String I_PROCESSHOLLOWER_TEST_EXE = "processhollower.testexe";
	private static final String I_SIMPLESTAGER_TEST_EXE = "simplestager.testexe";
	private static final String I_CPPHTTPDAEMON_TEST_EXE = "cpphttpdaemon.testexe";
	private static final String I_CPPDNSDAEMON_TEST_EXE = "cppdnsdaemon.testexe";
	private static final String I_CPPSMTPDAEMON_TEST_EXE = "cppsmtpdaemon.testexe";
	private static final String I_CSHARPHTTPDAEMON_TEST_EXE = "csharphttpdaemon.testexe";
	private static final String I_CSHARPDNSDAEMON_TEST_EXE = "csharpdnsdaemon.testexe";
	private static final String I_CSHARPSMTPDAEMON_TEST_EXE = "csharpsmtpdaemon.testexe";
	private static final String I_WINDOWSNATIVE_TEST_EXE = "windowsnative.textexe";
	private static final String I_SMBSERVER_CPPDNSDAEMON_TEST_EXE = "smbserver.cppdnsdaemon.testexe";
	private static final String I_SMBCLIENT_CPPDNSDAEMON_TEST_EXE = "smbclient.cppdnsdaemon.testexe";
	private static final String I_SMBSERVER_CPPHTTPDAEMON_TEST_EXE = "smbserver.cpphttpdaemon.testexe";
	private static final String I_SMBCLIENT_CPPHTTPDAEMON_TEST_EXE = "smbclient.cpphttpdaemon.testexe";
	private static final String I_SMBSERVER_CSHARPHTTPDAEMON_TEST_EXE = "smbserver.csharphttpdaemon.testexe";
	private static final String I_SMBCLIENT_CSHARPHTTPDAEMON_TEST_EXE = "smbclient.csharphttpdaemon.testexe";
	private static final String I_SMBSERVER_CSHARPDNSDAEMON_TEST_EXE = "smbserver.csharpdnsdaemon.testexe";
	private static final String I_SMBCLIENT_CSHARPDNSDAEMON_TEST_EXE = "smbclient.csharpdnsdaemon.testexe";
	private static final String I_PYTHON_HTTPSDAEMON_TEST_EXE = "python.httpsdaemon.textexe";
	private static final String I_PYTHON_DNSDAEMON_TEST_EXE = "python.dnsdaemon.textexe";
	private static final String I_PYTHON_SMTPDAEMON_TEST_EXE = "python.smtpdaemon.textexe";
	private static final String I_PYTHON_TLMSIM_TEST_EXE = "python.tlmsim.textexe";
	
	private static final String I_PYTHON_EXE = "python.exe";
	
	private static final String I_PORT_FORWARD_TEST_IP_LOCAL="portforward.testip.local";
	private static final String I_PORT_FORWARD_TEST_IP_LINUX="portforward.testip.linux";
	
	public static String EXECUTIONROOT_REMOTE;
	
	public static String TMP_DIR;
	public static String TMP_CHROME_COOKIES;
	public static String TMP_FIREFOX_COOKIES;
	public static String TMP_EDGE_COOKIES;
	public static String TMP_GENERIC;
	
	public static boolean WINLOGON_REG_LIVE_DENIAL_ENABLE;
	public static boolean OUTLOOKHARVEST_LIVE_ENABLE;
	public static boolean LARGE_HARVEST_TEST_ENABLE;
	public static boolean KEYLOGGER_TEST_ENABLE;
	public static boolean UAC_CMSTP_TEST_ENABLE;
	
	public static String PROCESSHOLLOWER_TEST_EXE;
	public static String SIMPLESTAGER_TEST_EXE;
	public static String CPPHTTPDAEMON_TEST_EXE;
	public static String CPPDNSDAEMON_TEST_EXE;
	public static String CPPSMTPDAEMON_TEST_EXE;
	public static String CSHARPHTTPDAEMON_TEST_EXE;
	public static String CSHARPDNSDAEMON_TEST_EXE;
	public static String CSHARPSMTPDAEMON_TEST_EXE;
	public static String WINDOWSNATIVE_TEST_EXE;
	public static String SMBSERVER_CPPDNSDAEMON_TEST_EXE;
	public static String SMBCLIENT_CPPDNSDAEMON_TEST_EXE;
	public static String SMBSERVER_CPPHTTPDAEMON_TEST_EXE;
	public static String SMBCLIENT_CPPHTTPDAEMON_TEST_EXE;
	public static String SMBSERVER_CSHARPDNSDAEMON_TEST_EXE;
	public static String SMBCLIENT_CSHARPDNSDAEMON_TEST_EXE;
	public static String SMBSERVER_CSHARPHTTPDAEMON_TEST_EXE;
	public static String SMBCLIENT_CSHARPHTTPDAEMON_TEST_EXE;
	
	public static String PYTHON_EXE;
	
	public static String PORT_FORWARD_TEST_IP_LOCAL;
	public static String PORT_FORWARD_TEST_IP_LINUX;
	
	public static String PYTHON_HTTPSDAEMON_TEST_EXE;
	public static String PYTHON_DNSDAEMON_TEST_EXE;
	public static String PYTHON_SMTPDAEMON_TEST_EXE;
	public static String PYTHON_TLMSIM_TEST_EXE;
	
	static {
		init();
	}
	
	public static void init() {
		try (InputStream input = new FileInputStream("test" + File.separator + "test_config.properties")) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);
			
			EXECUTIONROOT_REMOTE = prop.getProperty(I_EXECUTIONROOT_REMOTE);
			
			TMP_DIR = prop.getProperty(I_TMP_DIR);
			TMP_CHROME_COOKIES = prop.getProperty(I_TMP_CHROME_COOKIES);
			TMP_FIREFOX_COOKIES = prop.getProperty(I_TMP_FIREFOX_COOKIES);
			TMP_EDGE_COOKIES = prop.getProperty(I_TMP_EDGE_COOKIES);
			TMP_GENERIC =  prop.getProperty(I_TMP_GENERIC);

			SIMPLESTAGER_TEST_EXE = prop.getProperty(I_SIMPLESTAGER_TEST_EXE);
			PROCESSHOLLOWER_TEST_EXE = prop.getProperty(I_PROCESSHOLLOWER_TEST_EXE);
			CPPHTTPDAEMON_TEST_EXE = prop.getProperty(I_CPPHTTPDAEMON_TEST_EXE);
			CPPDNSDAEMON_TEST_EXE = prop.getProperty(I_CPPDNSDAEMON_TEST_EXE);
			CPPSMTPDAEMON_TEST_EXE = prop.getProperty(I_CPPSMTPDAEMON_TEST_EXE);
			CSHARPHTTPDAEMON_TEST_EXE = prop.getProperty(I_CSHARPHTTPDAEMON_TEST_EXE);
			CSHARPDNSDAEMON_TEST_EXE = prop.getProperty(I_CSHARPDNSDAEMON_TEST_EXE);
			CSHARPSMTPDAEMON_TEST_EXE = prop.getProperty(I_CSHARPSMTPDAEMON_TEST_EXE);
			WINDOWSNATIVE_TEST_EXE = prop.getProperty(I_WINDOWSNATIVE_TEST_EXE);
			
			SMBSERVER_CPPDNSDAEMON_TEST_EXE = prop.getProperty(I_SMBSERVER_CPPDNSDAEMON_TEST_EXE);
			SMBCLIENT_CPPDNSDAEMON_TEST_EXE = prop.getProperty(I_SMBCLIENT_CPPDNSDAEMON_TEST_EXE);
			
			SMBSERVER_CPPHTTPDAEMON_TEST_EXE = prop.getProperty(I_SMBSERVER_CPPHTTPDAEMON_TEST_EXE);
			SMBCLIENT_CPPHTTPDAEMON_TEST_EXE = prop.getProperty(I_SMBCLIENT_CPPHTTPDAEMON_TEST_EXE);
			
			SMBSERVER_CSHARPHTTPDAEMON_TEST_EXE = prop.getProperty(I_SMBSERVER_CSHARPHTTPDAEMON_TEST_EXE);
			SMBCLIENT_CSHARPHTTPDAEMON_TEST_EXE = prop.getProperty(I_SMBCLIENT_CSHARPHTTPDAEMON_TEST_EXE);
			
			SMBSERVER_CSHARPDNSDAEMON_TEST_EXE = prop.getProperty(I_SMBSERVER_CSHARPDNSDAEMON_TEST_EXE);
			SMBCLIENT_CSHARPDNSDAEMON_TEST_EXE = prop.getProperty(I_SMBCLIENT_CSHARPDNSDAEMON_TEST_EXE);
			
			PYTHON_EXE = prop.getProperty(I_PYTHON_EXE);
			
			PORT_FORWARD_TEST_IP_LOCAL = prop.getProperty(I_PORT_FORWARD_TEST_IP_LOCAL);
			PORT_FORWARD_TEST_IP_LINUX = prop.getProperty(I_PORT_FORWARD_TEST_IP_LINUX);
			
			PYTHON_HTTPSDAEMON_TEST_EXE = prop.getProperty(I_PYTHON_HTTPSDAEMON_TEST_EXE);
			PYTHON_DNSDAEMON_TEST_EXE = prop.getProperty(I_PYTHON_DNSDAEMON_TEST_EXE);
			PYTHON_SMTPDAEMON_TEST_EXE = prop.getProperty(I_PYTHON_SMTPDAEMON_TEST_EXE);
			PYTHON_TLMSIM_TEST_EXE = prop.getProperty(I_PYTHON_TLMSIM_TEST_EXE);
			
			OUTLOOKHARVEST_LIVE_ENABLE = prop.getProperty(I_OUTLOOKHARVEST_LIVE_ENABLE, "false").equalsIgnoreCase("true");
			LARGE_HARVEST_TEST_ENABLE = prop.getProperty(I_LARGE_HARVEST_TEST_ENABLE, "false").equalsIgnoreCase("true");
			KEYLOGGER_TEST_ENABLE = prop.getProperty(I_KEYLOGGER_TEST_ENABLE, "false").equalsIgnoreCase("true");
			WINLOGON_REG_LIVE_DENIAL_ENABLE = prop.getProperty(I_WINLOGON_REG_LIVE_DENIAL_ENABLE, "false").equalsIgnoreCase("true");
			UAC_CMSTP_TEST_ENABLE = prop.getProperty(I_UAC_CMSTP_TEST_ENABLE, "false").equalsIgnoreCase("true");
		} catch (IOException ex) {
			System.out.println("Unable to load test config file");
			ex.printStackTrace();
		}
	}
}
