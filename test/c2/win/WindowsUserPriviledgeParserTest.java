package c2.win;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class WindowsUserPriviledgeParserTest {

	public static final String NO_PRIV = "\r\n"
			+ "PRIVILEGES INFORMATION\r\n"
			+ "----------------------\r\n"
			+ "\r\n"
			+ "Privilege Name                Description                          State\r\n"
			+ "============================= ==================================== ========\r\n"
			+ "SeShutdownPrivilege           Shut down the system                 Disabled\r\n"
			+ "SeChangeNotifyPrivilege       Bypass traverse checking             Enabled\r\n"
			+ "SeUndockPrivilege             Remove computer from docking station Disabled\r\n"
			+ "SeIncreaseWorkingSetPrivilege Increase a process working set       Disabled\r\n"
			+ "SeTimeZonePrivilege           Change the time zone                 Disabled\\r\\n";
	
	public static final String WITH_PRIV = "\r\n"
			+ "PRIVILEGES INFORMATION\r\n"
			+ "----------------------\r\n"
			+ "\r\n"
			+ "Privilege Name                Description                          State\r\n"
			+ "============================= ==================================== ========\r\n"
			+ "SeShutdownPrivilege           Shut down the system                 Disabled\r\n"
			+ "SeChangeNotifyPrivilege       Bypass traverse checking             Enabled\r\n"
			+ "SeImpersonatePrivilege        Impersonate a client after authentication Enabled\r\n"
			+ "SeUndockPrivilege             Remove computer from docking station Disabled\r\n"
			+ "SeIncreaseWorkingSetPrivilege Increase a process working set       Disabled\r\n"
			+ "SeTimeZonePrivilege           Change the time zone                 Disabled\\r\\n";
	
	
	
	@Test
	void testNoPriv() {
		try {
			WindowsUserPriviledgeParser privs = new WindowsUserPriviledgeParser(NO_PRIV);
			assertFalse(privs.isHasSeImpersonate());
		}catch(WindowsToolOutputParseException ex) {
			fail("Did not parse privs");
		}
	}
	
	@Test
	void testWithPriv() {
		try {
			WindowsUserPriviledgeParser privs = new WindowsUserPriviledgeParser(WITH_PRIV);
			assertTrue(privs.isHasSeImpersonate());
		}catch(WindowsToolOutputParseException ex) {
			fail("Did not parse privs");
		}
	}

}
