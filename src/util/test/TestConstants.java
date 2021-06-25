package util.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TestConstants {
	private static final String I_USERNAME_LINUX = "username.linux";
	private static final String I_HOSTNAME_LINUX = "hostname.linux";
	private static final String I_EXECUTIONROOT_LINUX = "executionroot.linux";
	
	private static final String I_TMP_DIR = "tmp.dir";
	private static final String I_TMP_CHROME_COOKIES = "tmp.chrome.cookies";
	private static final String I_TMP_FIREFOX_COOKIES = "tmp.firefox.cookies";
	private static final String I_TMP_EDGE_COOKIES = "tmp.edge.cookies";
	private static final String I_TMP_GENERIC = "tmp.generic";
	
	private static final String I_TEST_EXECUTION_ROOT = "test.executionroot";
	
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
	
	private static final String I_PYTHON_EXE = "python.exe";
	
	public static String USERNAME_LINUX;
	public static String HOSTNAME_LINUX;
	public static String EXECUTIONROOT_LINUX;
	
	public static String TMP_DIR;
	public static String TMP_CHROME_COOKIES;
	public static String TMP_FIREFOX_COOKIES;
	public static String TMP_EDGE_COOKIES;
	public static String TMP_GENERIC;
	
	public static String TEST_EXECUTION_ROOT;
	
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
	static {
		try (InputStream input = new FileInputStream("test" + File.separator + "test_config.properties")) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);
			
			USERNAME_LINUX = prop.getProperty(I_USERNAME_LINUX);
			HOSTNAME_LINUX = prop.getProperty(I_HOSTNAME_LINUX);
			EXECUTIONROOT_LINUX = prop.getProperty(I_EXECUTIONROOT_LINUX);
			
			TMP_DIR = prop.getProperty(I_TMP_DIR);
			TMP_CHROME_COOKIES = prop.getProperty(I_TMP_CHROME_COOKIES);
			TMP_FIREFOX_COOKIES = prop.getProperty(I_TMP_FIREFOX_COOKIES);
			TMP_EDGE_COOKIES = prop.getProperty(I_TMP_EDGE_COOKIES);
			TMP_GENERIC =  prop.getProperty(I_TMP_GENERIC);

			TEST_EXECUTION_ROOT = prop.getProperty(I_TEST_EXECUTION_ROOT);
			
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
		} catch (IOException ex) {
			System.out.println("Unable to load config file");
			fail(ex.getMessage());
		}
	}
}
