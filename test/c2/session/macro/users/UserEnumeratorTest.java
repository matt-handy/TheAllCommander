package c2.session.macro.users;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import c2.Commands;
import c2.Constants;
import c2.session.CommandLoader;
import c2.session.IOManager;
import c2.session.log.IOLogger;
import c2.session.macro.MacroOutcome;
import c2.session.macro.OutlookHarvesterMacro;
import c2.session.macro.users.Group.GROUP_TYPE;
import util.Time;
import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestCommons;

class UserEnumeratorTest {

	public static String GROUP_ROOT= "root : root kaboxer";
	public static String GROUP_KALI= "kali : kali cdrom floppy sudo audio dip video plugdev netdev bluetooth scanner kaboxer";
	
	public static String ETC_PASSWD = "root:x:0:0:root:/root:/usr/bin/zsh\r\n"
			+ "kali:x:1000:1000:Kali,,,:/home/kali:/usr/bin/zsh";
	
	public static String MACRO_LINUX_RESPONSE = "Macro Executor: 'User Profile: \r\n"
			+ "User: root type LOCAL\r\n"
			+ "Group Membership: root, Type: LOCAL\r\n"
			+ "Group Membership: kaboxer, Type: LOCAL\r\n"
			+ "User: kali type LOCAL\r\n"
			+ "Group Membership: kali, Type: LOCAL\r\n"
			+ "Group Membership: cdrom, Type: LOCAL\r\n"
			+ "Group Membership: floppy, Type: LOCAL\r\n"
			+ "Group Membership: sudo, Type: LOCAL\r\n"
			+ "Group Membership: audio, Type: LOCAL\r\n"
			+ "Group Membership: dip, Type: LOCAL\r\n"
			+ "Group Membership: video, Type: LOCAL\r\n"
			+ "Group Membership: plugdev, Type: LOCAL\r\n"
			+ "Group Membership: netdev, Type: LOCAL\r\n"
			+ "Group Membership: bluetooth, Type: LOCAL\r\n"
			+ "Group Membership: scanner, Type: LOCAL\r\n"
			+ "Group Membership: kaboxer, Type: LOCAL\r\n"
			+ "'";
	
	public static String ETC_GROUP = "root:x:0:\r\n"
			+ "daemon:x:1:\r\n"
			+ "bin:x:2:\r\n"
			+ "sys:x:3:\r\n"
			+ "adm:x:4:\r\n"
			+ "tty:x:5:\r\n"
			+ "disk:x:6:\r\n"
			+ "lp:x:7:\r\n"
			+ "mail:x:8:\r\n"
			+ "news:x:9:\r\n"
			+ "uucp:x:10:\r\n"
			+ "man:x:12:\r\n"
			+ "proxy:x:13:\r\n"
			+ "kmem:x:15:\r\n"
			+ "dialout:x:20:\r\n"
			+ "fax:x:21:\r\n"
			+ "voice:x:22:\r\n"
			+ "cdrom:x:24:kali\r\n"
			+ "floppy:x:25:kali\r\n"
			+ "tape:x:26:\r\n"
			+ "sudo:x:27:kali\r\n"
			+ "audio:x:29:pulse,kali\r\n"
			+ "dip:x:30:kali\r\n"
			+ "www-data:x:33:\r\n"
			+ "backup:x:34:\r\n"
			+ "operator:x:37:\r\n"
			+ "list:x:38:\r\n"
			+ "irc:x:39:\r\n"
			+ "src:x:40:\r\n"
			+ "gnats:x:41:\r\n"
			+ "shadow:x:42:\r\n"
			+ "utmp:x:43:\r\n"
			+ "video:x:44:kali\r\n"
			+ "sasl:x:45:\r\n"
			+ "plugdev:x:46:kali\r\n"
			+ "staff:x:50:\r\n"
			+ "games:x:60:\r\n"
			+ "users:x:100:\r\n"
			+ "nogroup:x:65534:\r\n"
			+ "systemd-timesync:x:101:\r\n"
			+ "systemd-journal:x:102:\r\n"
			+ "systemd-network:x:103:\r\n"
			+ "systemd-resolve:x:104:\r\n"
			+ "input:x:105:\r\n"
			+ "kvm:x:106:\r\n"
			+ "render:x:107:\r\n"
			+ "crontab:x:108:\r\n"
			+ "netdev:x:109:kali\r\n"
			+ "mysql:x:110:\r\n"
			+ "tss:x:111:\r\n"
			+ "ssl-cert:x:112:postgres\r\n"
			+ "ntp:x:113:\r\n"
			+ "messagebus:x:114:\r\n"
			+ "redsocks:x:115:\r\n"
			+ "mlocate:x:116:\r\n"
			+ "kismet:x:117:\r\n"
			+ "bluetooth:x:119:kali\r\n"
			+ "tcpdump:x:120:\r\n"
			+ "rtkit:x:121:\r\n"
			+ "kali-trusted:x:122:\r\n"
			+ "postgres:x:123:\r\n"
			+ "i2c:x:124:\r\n"
			+ "avahi:x:125:\r\n"
			+ "stunnel4:x:126:\r\n"
			+ "Debian-snmp:x:127:\r\n"
			+ "sslh:x:128:\r\n"
			+ "nm-openvpn:x:129:\r\n"
			+ "nm-openconnect:x:130:\r\n"
			+ "pulse:x:131:\r\n"
			+ "pulse-access:x:132:\r\n"
			+ "scanner:x:133:saned,kali\r\n"
			+ "saned:x:134:\r\n"
			+ "sambashare:x:135:\r\n"
			+ "inetsim:x:136:\r\n"
			+ "colord:x:137:\r\n"
			+ "geoclue:x:138:\r\n"
			+ "lightdm:x:139:\r\n"
			+ "kpadmins:x:140:\r\n"
			+ "kali:x:1000:\r\n"
			+ "vboxsf:x:141:\r\n"
			+ "kaboxer:x:142:kali,root\r\n"
			+ "systemd-coredump:x:999:\r\n"
			+ "_ssh:x:118:";
	
	public static String NO_DOMAIN_STR = "The request will be processed at a domain controller for domain WORKGROUP.\r\n"
			+ "\r\n"
			+ "System error 1355 has occurred.\r\n"
			+ "\r\n"
			+ "The specified domain either does not exist or could not be contacted.\r\n"
			+ "\r\n"
			+ "";
	
	public static String EXAMPLE_USERS_STR = "\r\n"
			+ "User accounts for \\\\HOST\r\n"
			+ "\r\n"
			+ "-------------------------------------------------------------------------------\r\n"
			+ "Administrator            DefaultAccount           Guest\r\n"
			+ "bob\r\n"
			+ "The command completed successfully.\r\n"
			+ "\r\n"
			+ "";
	
	public static String EXAMPLE_USERS_WITH_ERROR_STR = "\r\n"
			+ "User accounts for \\\\HOST\r\n"
			+ "\r\n"
			+ "-------------------------------------------------------------------------------\r\n"
			+ "Administrator            DefaultAccount           Guest\r\n"
			+ "bob_\r\n"
			+ "The command completed with one or more errors.\r\n"
			+ "\r\n"
			+ "";
	
	public static String EXAMPLE_USERS_PROFILE_OUTPUT = "Macro Executor: 'User Profile: " + System.lineSeparator()
			+ "User: Administrator type LOCAL"  + System.lineSeparator()
			+ "Group Membership: Administrators, Type: LOCAL"  + System.lineSeparator()
			+ "User: DefaultAccount type LOCAL" + System.lineSeparator()
			+ "Group Membership: System Managed Account, Type: LOCAL" + System.lineSeparator()
			+ "User: Guest type LOCAL" + System.lineSeparator()
			+ "Group Membership: Guests, Type: LOCAL" + System.lineSeparator()
			+ "User: bob type LOCAL" + System.lineSeparator()
			+ "Group Membership: Administrators, Type: LOCAL" + System.lineSeparator()
			+ "Group Membership: Users, Type: LOCAL" + System.lineSeparator()
			+ "'";
	
	public static String EXAMPLE_GROUPS_STR = "\r\n"
			+ "Aliases for \\\\HOST\r\n"
			+ "\r\n"
			+ "-------------------------------------------------------------------------------\r\n"
			+ "*Administrators\r\n"
			+ "*Guests\r\n"
			+ "*Users\r\n"
			+ "The command completed successfully.\r\n"
			+ "\r\n"
			+ "";
	
	public static String EXAMPLE_ADMINISTRATOR_STR = "User name                    Administrator\r\n"
			+ "Full Name\r\n"
			+ "Comment                      Built-in account for administering the computer/domain\r\n"
			+ "User's comment\r\n"
			+ "Country/region code          000 (System Default)\r\n"
			+ "Account active               No\r\n"
			+ "Account expires              Never\r\n"
			+ "\r\n"
			+ "Password last set            10/28/2022 11:20:55 AM\r\n"
			+ "Password expires             Never\r\n"
			+ "Password changeable          10/28/2022 11:20:55 AM\r\n"
			+ "Password required            Yes\r\n"
			+ "User may change password     Yes\r\n"
			+ "\r\n"
			+ "Workstations allowed         All\r\n"
			+ "Logon script\r\n"
			+ "User profile\r\n"
			+ "Home directory\r\n"
			+ "Last logon                   3/8/2019 6:47:36 AM\r\n"
			+ "\r\n"
			+ "Logon hours allowed          All\r\n"
			+ "\r\n"
			+ "Local Group Memberships      *Administrators\r\n"
			+ "Global Group memberships     *None\r\n"
			+ "The command completed successfully.\r\n"
			+ "\r\n"
			+ "";
	
	public static String EXAMPLE_GUEST_STR = "User name                    Guest\r\n"
			+ "Full Name\r\n"
			+ "Comment                      Built-in account for guest access to the computer/domain\r\n"
			+ "User's comment\r\n"
			+ "Country/region code          000 (System Default)\r\n"
			+ "Account active               No\r\n"
			+ "Account expires              Never\r\n"
			+ "\r\n"
			+ "Password last set            10/28/2022 11:22:08 AM\r\n"
			+ "Password expires             Never\r\n"
			+ "Password changeable          10/28/2022 11:22:08 AM\r\n"
			+ "Password required            No\r\n"
			+ "User may change password     No\r\n"
			+ "\r\n"
			+ "Workstations allowed         All\r\n"
			+ "Logon script\r\n"
			+ "User profile\r\n"
			+ "Home directory\r\n"
			+ "Last logon                   Never\r\n"
			+ "\r\n"
			+ "Logon hours allowed          All\r\n"
			+ "\r\n"
			+ "Local Group Memberships      *Guests\r\n"
			+ "Global Group memberships     *None\r\n"
			+ "The command completed successfully.\r\n"
			+ "\r\n"
			+ "";
	
	public static String EXAMPLE_DEFAULT_ACCOUNT_STR = "User name                    DefaultAccount\r\n"
			+ "Full Name\r\n"
			+ "Comment                      A user account managed by the system.\r\n"
			+ "User's comment\r\n"
			+ "Country/region code          000 (System Default)\r\n"
			+ "Account active               No\r\n"
			+ "Account expires              Never\r\n"
			+ "\r\n"
			+ "Password last set            10/28/2022 11:23:19 AM\r\n"
			+ "Password expires             Never\r\n"
			+ "Password changeable          10/28/2022 11:23:19 AM\r\n"
			+ "Password required            No\r\n"
			+ "User may change password     Yes\r\n"
			+ "\r\n"
			+ "Workstations allowed         All\r\n"
			+ "Logon script\r\n"
			+ "User profile\r\n"
			+ "Home directory\r\n"
			+ "Last logon                   Never\r\n"
			+ "\r\n"
			+ "Logon hours allowed          All\r\n"
			+ "\r\n"
			+ "Local Group Memberships      *System Managed Account\r\n"
			+ "Global Group memberships     *None\r\n"
			+ "The command completed successfully.\r\n"
			+ "\r\n"
			+ "";
	
	public static String EXAMPLE_BOB_STR = "User name                    bob\r\n"
			+ "Full Name                    Bob SonOfBob\r\n"
			+ "Comment\r\n"
			+ "User's comment\r\n"
			+ "Country/region code          000 (System Default)\r\n"
			+ "Account active               Yes\r\n"
			+ "Account expires              Never\r\n"
			+ "\r\n"
			+ "Password last set            3/11/2019 9:13:43 PM\r\n"
			+ "Password expires             Never\r\n"
			+ "Password changeable          3/11/2019 9:13:43 PM\r\n"
			+ "Password required            Yes\r\n"
			+ "User may change password     Yes\r\n"
			+ "\r\n"
			+ "Workstations allowed         All\r\n"
			+ "Logon script\r\n"
			+ "User profile\r\n"
			+ "Home directory\r\n"
			+ "Last logon                   3/8/2021 1:38:09 PM\r\n"
			+ "\r\n"
			+ "Logon hours allowed          All\r\n"
			+ "\r\n"
			+ "Local Group Memberships      *Administrators       *Users\r\n"
			+ "Global Group memberships     *None\r\n"
			+ "The command completed successfully.\r\n"
			+ "\r\n"
			+ "";
	
	public static String NET_GROUP_NO_DC_RESPONSE = "This command can be used only on a Windows Domain Controller.\r\n"
			+ "\r\n"
			+ "More help is available by typing NET HELPMSG 3515.\r\n"
			+ "\r\n"
			+ "";
	
	private Properties properties = ClientServerTest.getDefaultSystemTestProperties();
	private IOManager io;
	private int id;
	
	@BeforeEach
	void setUp() {
		CommandLoader cl = new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>());
		io = new IOManager(new IOLogger(Paths.get(properties.getProperty(Constants.HUBLOGGINGPATH))), cl);
		TestCommons.cleanFileHarvesterDir();
		id = io.addSession("user", "host", "protocol");

		// Flush the command buffer
		String cmd = io.pollCommand(id);
		while (cmd != null) {
			cmd = io.pollCommand(id);
		}
	}
	
	@AfterEach
	void cleanup() {
		RunnerTestGeneric.cleanLogs();
		
		TestCommons.cleanFileHarvesterDir();
	}
	
	private class ClientResponseEmulator implements Runnable {

		private IOManager io;
		private int id;

		private boolean alive = true;
		
		private String pretendOS;

		private boolean errorOnUserPoll = false;
		
		public ClientResponseEmulator(IOManager io, int id, String pretendOS) {
			this.io = io;
			this.id = id;
			this.pretendOS = pretendOS;
		}

		public void setErrorOnUserPoll(boolean errorOnUserPoll) {
			this.errorOnUserPoll = errorOnUserPoll;
		}
		
		public void kill() {
			alive = false;
		}

		@Override
		public void run() {
			while (alive) {
				String command = io.pollCommand(id);
				if (command == null) {
					// continue
					Time.sleepWrapped(10);
				}else if(command.equals(LinuxUserEnumeratorMacro.LIST_LOCAL_USERS_COMMAND)) {
					io.sendIO(id, ETC_PASSWD);
				}else if(command.equals(LinuxUserEnumeratorMacro.LIST_LOCAL_GROUPS_COMMAND)) {
					io.sendIO(id, ETC_GROUP);
				}else if(command.equals("groups root")) {
					io.sendIO(id, GROUP_ROOT);
				}else if(command.equals("groups kali")) {
					io.sendIO(id, GROUP_KALI);
				} else if (command.equals(WindowsUserEnumeratorMacro.LIST_LOCAL_USERS_COMMAND)) {
					if(errorOnUserPoll) {
						io.sendIO(id, EXAMPLE_USERS_WITH_ERROR_STR);
					}else {
						io.sendIO(id, EXAMPLE_USERS_STR);
					}
				} else if (command.equals(WindowsUserEnumeratorMacro.LIST_DOMAIN_USERS_COMMAND)) {
					io.sendIO(id, NO_DOMAIN_STR);
				} else if (command.equals(WindowsUserEnumeratorMacro.LIST_LOCAL_GROUPS_COMMAND)) {
					io.sendIO(id, EXAMPLE_GROUPS_STR);
				}else if(command.equals(WindowsUserEnumeratorMacro.LIST_DOMAIN_GROUPS_COMMAND)) {
					io.sendIO(id, NET_GROUP_NO_DC_RESPONSE);
				}else if(command.equals("net user Administrator")) {
					io.sendIO(id, EXAMPLE_ADMINISTRATOR_STR);
				}else if(command.equals("net user Guest")) {
					io.sendIO(id, EXAMPLE_GUEST_STR);
				}else if(command.equals("net user bob")) {
					io.sendIO(id, EXAMPLE_BOB_STR);
				}else if(command.equals("net user DefaultAccount")) {
					io.sendIO(id, EXAMPLE_DEFAULT_ACCOUNT_STR);
				}else if(command.equals(Commands.OS_HERITAGE)) {
					if(pretendOS.equals("Windows")) {
						io.sendIO(id, Commands.OS_HERITAGE_RESPONSE_WINDOWS);
					}else if(pretendOS.equals("Linux")) {
						io.sendIO(id, Commands.OS_HERITAGE_RESPONSE_LINUX);
					}else if(pretendOS.equals("Mac")) {
						io.sendIO(id, Commands.OS_HERITAGE_RESPONSE_MAC);
					}else {
						System.out.println("Cannot emulate unknown OS: " + pretendOS);
					}
				} else {
					System.out.println("Unknown command: " + command);
				}
			}
		}

	}
	
	@Test
	void testListUsersWindowsNoDomain() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientResponseEmulator em = new ClientResponseEmulator(io, id, "Windows");
		exec.submit(em);
		
		MacroOutcome outcome = new MacroOutcome();
		WindowsUserEnumeratorMacro macro = new WindowsUserEnumeratorMacro(io);
		try {
			List<User> users = macro.getUserList(id, outcome);
			boolean foundAdministrator = false;
			boolean foundDefaultAccount = false;
			boolean foundGuest = false;
			boolean foundbob = false;
			for(User user : users) {
				if(user.username.equals("Administrator")) {
					foundAdministrator = true;
				}else if(user.username.equals("DefaultAccount")) {
					foundDefaultAccount = true;
				}else if(user.username.equals("Guest")) {
					foundGuest = true;
				}else if(user.username.equals("bob")) {
					foundbob = true;
				}else {
					fail("Unknown user: " + user.username);
				}
			}
			assertEquals(4, users.size());
			assertTrue(foundAdministrator, "Could not find Administrator");
			assertTrue(foundDefaultAccount, "Could not find DefaultAccount");
			assertTrue(foundGuest, "Could not find Guest");
			assertTrue(foundbob, "Could not find bob");
		}catch(Exception ex) {
			fail(ex.getMessage());
		}
		
		assertEquals("Sent Command: 'net user'", outcome.getOutput().get(0));
		//The extra line separator is added as part of the command flushing process through the IO processor
		assertEquals("Received response: '" + EXAMPLE_USERS_STR.replace("\r\n", System.lineSeparator()) + System.lineSeparator() + "'", outcome.getOutput().get(1));
		assertEquals("Sent Command: 'net user /domain'", outcome.getOutput().get(2));
		assertEquals("Received response: '" + NO_DOMAIN_STR.replace("\r\n", System.lineSeparator()) + System.lineSeparator() + "'", outcome.getOutput().get(3));
		assertEquals("Macro Executor: 'No domain available'", outcome.getOutput().get(4));
	}
	
	@Test
	void testListUsersThrowsExceptionIfError() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientResponseEmulator em = new ClientResponseEmulator(io, id, "Windows");
		em.setErrorOnUserPoll(true);
		exec.submit(em);
		
		MacroOutcome outcome = new MacroOutcome();
		WindowsUserEnumeratorMacro macro = new WindowsUserEnumeratorMacro(io);
		
		boolean foundError = false;
		try {
			macro.getUserList(id, outcome);
		}catch(Exception ex) {
			assertEquals("Cannot list local users", ex.getMessage());
			foundError = true;
		}
		assertTrue(foundError);
	}
	
	
	
	@Test
	void testMacroDiscoversCorrectOSWindows() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientResponseEmulator em = new ClientResponseEmulator(io, id, "Windows");
		exec.submit(em);
		
		UserEnumeratorMacro macro = new UserEnumeratorMacro();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(UserEnumeratorMacro.COMMAND_STR, id, "unused");
		
		assertEquals("Sent Command: 'os_heritage'", outcome.getOutput().get(0));
		assertEquals("Received response: 'Windows" + System.lineSeparator()
				+ "'", outcome.getOutput().get(1));
		assertEquals("Macro Executor: 'Proceeding with Windows enumeration'", outcome.getOutput().get(2));
	}
	
	@Test
	void testMacroDiscoversCorrectOSLinux() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientResponseEmulator em = new ClientResponseEmulator(io, id, "Linux");
		exec.submit(em);
		
		UserEnumeratorMacro macro = new UserEnumeratorMacro();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(UserEnumeratorMacro.COMMAND_STR, id, "unused");
		
		assertEquals("Sent Command: 'os_heritage'", outcome.getOutput().get(0));
		assertEquals("Received response: 'Linux" + System.lineSeparator()
				+ "'", outcome.getOutput().get(1));
		assertEquals("Macro Executor: 'Proceeding with Linux enumeration'", outcome.getOutput().get(2));
		assertEquals("Sent Command: 'cat /etc/group'", outcome.getOutput().get(3));
		assertEquals("Received response: '" + ETC_GROUP + System.lineSeparator() + "'", outcome.getOutput().get(4));
		assertEquals("Sent Command: 'cat /etc/passwd'", outcome.getOutput().get(5));
		assertEquals("Received response: '" + ETC_PASSWD + System.lineSeparator() + "'", outcome.getOutput().get(6));
		assertEquals("Sent Command: 'groups root'", outcome.getOutput().get(7));
		assertEquals("Received response: 'root : root kaboxer" + System.lineSeparator() + "'", outcome.getOutput().get(8));
		assertEquals("Sent Command: 'groups kali'", outcome.getOutput().get(9));
		assertEquals("Received response: 'kali : kali cdrom floppy sudo audio dip video plugdev netdev bluetooth scanner kaboxer" + System.lineSeparator() + "'", outcome.getOutput().get(10));
		assertEquals("Macro Executor: 'Account profile complete'", outcome.getOutput().get(11));
		assertEquals("Macro Executor: 'User enumeration not supported for Linux at this time'", outcome.getOutput().get(12));
		String expected = MACRO_LINUX_RESPONSE.replace("\r\n", System.lineSeparator());
		assertEquals(expected, outcome.getOutput().get(13));
		
		assertEquals(14, outcome.getOutput().size());
	}
	
	@Test
	void testMacroWritesCredsForUserUse() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientResponseEmulator em = new ClientResponseEmulator(io, id, "Windows");
		exec.submit(em);
		
		UserEnumeratorMacro macro = new UserEnumeratorMacro();
		macro.initialize(io, null);
		MacroOutcome outcome = macro.processCmd(UserEnumeratorMacro.COMMAND_STR, id, "unused");
		
		assertEquals("Sent Command: 'os_heritage'", outcome.getOutput().get(0));
		assertEquals("Received response: 'Windows" + System.lineSeparator()
				+ "'", outcome.getOutput().get(1));
		assertEquals("Macro Executor: 'Proceeding with Windows enumeration'", outcome.getOutput().get(2));
		
		//Skip over enumeration steps, those are tested elsewhere
		int startIdx = 22;
		assertEquals(EXAMPLE_USERS_PROFILE_OUTPUT, outcome.getOutput().get(startIdx));
	}

	@Test
	void testListGroupsWindowsNotDC() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientResponseEmulator em = new ClientResponseEmulator(io, id, "Windows");
		exec.submit(em);
		
		MacroOutcome outcome = new MacroOutcome();
		WindowsUserEnumeratorMacro macro = new WindowsUserEnumeratorMacro(io);
		try {
			List<Group> groups = macro.getGroupList(id, outcome);
			boolean foundAdministrators = false;
			boolean foundGuests = false;
			boolean foundUsers = false;
			for(Group g : groups) {
				if(g.groupname.equals("Administrators")) {
					foundAdministrators = true;
				}else if(g.groupname.equals("Guests")) {
					foundGuests = true;
				}else if(g.groupname.equals("Users")) {
					foundUsers = true;
				}else {
					fail("Unknown group: " + g.groupname);
				}
				assertEquals(GROUP_TYPE.LOCAL, g.type);
			}
			assertEquals(3, groups.size());
			assertTrue(foundAdministrators);
			assertTrue(foundGuests);
			assertTrue(foundUsers);
		}catch(Exception ex) {
			fail(ex.getMessage());
		}
		
		assertEquals("Sent Command: 'net localgroup'", outcome.getOutput().get(0));
		assertEquals("Received response: '" + EXAMPLE_GROUPS_STR.replace("\r\n", System.lineSeparator()) + System.lineSeparator() + "'", outcome.getOutput().get(1));
		assertEquals("Sent Command: 'net group'", outcome.getOutput().get(2));
		assertEquals("Received response: '" + NET_GROUP_NO_DC_RESPONSE.replace("\r\n", System.lineSeparator()) + System.lineSeparator() + "'", outcome.getOutput().get(3));
		assertEquals("Macro Executor: 'Must be on a domain controller to list groups'", outcome.getOutput().get(4));
		
	}
	
	/*
	@Test
	void testUserGroupAssociationsDiscoveredWithLocalAndGlobalGroups() {
		fail("Not yet implemented");
	}
	
	
	@Test
	void testListGroupsWindowsOnDC() {
		fail("Not yet implemented");
	}
	
	@Test
	void testListUsersWindowsWithDomain() {
		fail("Not yet implemented");
	}
	
	*/
	
	private void validateSystemAccountProfileNoDomain(SystemAccountProfile profile) {
		boolean foundAdministrator = false;
		boolean foundDefaultAccount = false;
		boolean foundGuest = false;
		boolean foundbob = false;
		for(User user : profile.getUserList()) {
			if(user.username.equals("Administrator")) {
				foundAdministrator = true;
				assertEquals(1, user.getGroups().size());
				assertEquals("Administrators", user.getGroups().get(0).groupname);
				assertEquals(GROUP_TYPE.LOCAL, user.getGroups().get(0).type);
			}else if(user.username.equals("DefaultAccount")) {
				foundDefaultAccount = true;
				assertEquals(1, user.getGroups().size());
				assertEquals("System Managed Account", user.getGroups().get(0).groupname);
				assertEquals(GROUP_TYPE.LOCAL, user.getGroups().get(0).type);
			}else if(user.username.equals("Guest")) {
				foundGuest = true;
				assertEquals(1, user.getGroups().size());
				assertEquals("Guests", user.getGroups().get(0).groupname);
				assertEquals(GROUP_TYPE.LOCAL, user.getGroups().get(0).type);
			}else if(user.username.equals("bob")) {
				foundbob = true;
				assertEquals(2, user.getGroups().size());
				assertEquals("Administrators", user.getGroups().get(0).groupname);
				assertEquals(GROUP_TYPE.LOCAL, user.getGroups().get(0).type);
				assertEquals("Users", user.getGroups().get(1).groupname);
				assertEquals(GROUP_TYPE.LOCAL, user.getGroups().get(1).type);
			}else {
				fail("Unknown user: " + user.username);
			}
		}
		assertEquals(4, profile.getUserList().size());
		assertTrue(foundAdministrator, "Could not find Administrator");
		assertTrue(foundDefaultAccount, "Could not find DefaultAccount");
		assertTrue(foundGuest, "Could not find Guest");
		assertTrue(foundbob, "Could not find bob");
		
		boolean foundAdministrators = false;
		boolean foundGuests = false;
		boolean foundUsers = false;
		boolean foundSystemManagedAccount = false;
		for(Group g : profile.getGroupList()) {
			if(g.groupname.equals("Administrators")) {
				foundAdministrators = true;
			}else if(g.groupname.equals("Guests")) {
				foundGuests = true;
			}else if(g.groupname.equals("Users")) {
				foundUsers = true;
			}else if(g.groupname.equals("System Managed Account")) {
				foundSystemManagedAccount = true;
			}else {
				fail("Unknown group: " + g.groupname);
			}
			assertEquals(GROUP_TYPE.LOCAL, g.type);
		}
		assertEquals(4, profile.getGroupList().size());
		assertTrue(foundAdministrators);
		assertTrue(foundGuests);
		assertTrue(foundUsers);
		assertTrue(foundSystemManagedAccount);
	}
	
	@Test
	void testUserGroupAssociationsDiscoveredNoDomain() {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		ClientResponseEmulator em = new ClientResponseEmulator(io, id, "Windows");
		exec.submit(em);
		
		MacroOutcome outcome = new MacroOutcome();
		WindowsUserEnumeratorMacro macro = new WindowsUserEnumeratorMacro(io);
		try {
			SystemAccountProfile profile = macro.getSystemAccountProfile(id, outcome);
			
			validateSystemAccountProfileNoDomain(profile);
			
			assertEquals("Sent Command: 'net localgroup'", outcome.getOutput().get(0));
			assertEquals("Received response: '" + EXAMPLE_GROUPS_STR.replace("\r\n", System.lineSeparator()) + System.lineSeparator() + "'", outcome.getOutput().get(1));
			assertEquals("Sent Command: 'net group'", outcome.getOutput().get(2));
			assertEquals("Received response: '" + NET_GROUP_NO_DC_RESPONSE.replace("\r\n", System.lineSeparator()) + System.lineSeparator() + "'", outcome.getOutput().get(3));
			assertEquals("Macro Executor: 'Must be on a domain controller to list groups'", outcome.getOutput().get(4));
			assertEquals("Sent Command: 'net user'", outcome.getOutput().get(5));
			assertEquals("Received response: '" + EXAMPLE_USERS_STR.replace("\r\n", System.lineSeparator()) + System.lineSeparator() + "'", outcome.getOutput().get(6));
			assertEquals("Sent Command: 'net user /domain'", outcome.getOutput().get(7));
			assertEquals("Received response: '" + NO_DOMAIN_STR.replace("\r\n", System.lineSeparator()) + System.lineSeparator() + "'", outcome.getOutput().get(8));
			assertEquals("Macro Executor: 'No domain available'", outcome.getOutput().get(9));
			assertEquals("Sent Command: 'net user Administrator'", outcome.getOutput().get(10));
			assertEquals("Received response: '" + EXAMPLE_ADMINISTRATOR_STR.replace("\r\n", System.lineSeparator()) + System.lineSeparator() + "'", outcome.getOutput().get(11));
			assertEquals("Sent Command: 'net user DefaultAccount'", outcome.getOutput().get(12));
			assertEquals("Received response: '" + EXAMPLE_DEFAULT_ACCOUNT_STR.replace("\r\n", System.lineSeparator()) + System.lineSeparator() + "'", outcome.getOutput().get(13));
			assertEquals("Sent Command: 'net user Guest'", outcome.getOutput().get(14));
			assertEquals("Received response: '" + EXAMPLE_GUEST_STR.replace("\r\n", System.lineSeparator()) + System.lineSeparator() + "'", outcome.getOutput().get(15));
			assertEquals("Sent Command: 'net user bob'", outcome.getOutput().get(16));
			assertEquals("Received response: '" + EXAMPLE_BOB_STR.replace("\r\n", System.lineSeparator()) + System.lineSeparator() + "'", outcome.getOutput().get(17));
			assertEquals("Macro Executor: 'Account profile complete'", outcome.getOutput().get(18));
		}catch(Exception ex) {
			System.out.println(ex.getMessage());
			fail(ex.getMessage());
		}
	}
	
	@Test
	void testMacroDetectsCommand() {
		UserEnumeratorMacro macro = new UserEnumeratorMacro();
		assertTrue(macro.isCommandMatch(UserEnumeratorMacro.COMMAND_STR));
		assertFalse(macro.isCommandMatch("barf"));
	}
		
}
