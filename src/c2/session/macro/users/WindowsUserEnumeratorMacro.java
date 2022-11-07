package c2.session.macro.users;

import java.util.ArrayList;
import java.util.List;

import c2.WindowsConstants;
import c2.session.IOManager;
import c2.session.macro.MacroOutcome;
import c2.session.macro.users.Group.GROUP_TYPE;
import c2.session.macro.users.User.USER_TYPE;

public class WindowsUserEnumeratorMacro {

	private IOManager io;

	public static String LIST_LOCAL_USERS_COMMAND = "net user";
	public static String LIST_DOMAIN_USERS_COMMAND = "net user /domain";
	public static String LIST_LOCAL_GROUPS_COMMAND = "net localgroup";
	public static String LIST_DOMAIN_GROUPS_COMMAND = "net group";
	public static String ENUM_LOCAL_USER_COMMAND = "net user $USER$";

	public WindowsUserEnumeratorMacro(IOManager io) {
		this.io = io;
	}

	private void pullUsersFromResponseStr(String response, USER_TYPE type, List<User> users) {
		String elements[] = response.split(System.lineSeparator());
		// Four lines are at the start of the command
		for (int idx = 4; idx < elements.length; idx++) {
			if (elements[idx].equals(WindowsConstants.WINDOWS_SYSTEM_COMMAND_COMPLETE_MSG)) {
				break;
			} else {
				String usersStrs[] = elements[idx].split(" ");
				for (String userStr : usersStrs) {
					if (!userStr.isEmpty()) {
						users.add(new User(userStr, USER_TYPE.LOCAL));
					}
				}
			}
		}
	}

	public List<User> getUserList(int sessionId, MacroOutcome outcome) throws Exception {
		List<User> users = new ArrayList<>();
		io.sendCommand(sessionId, LIST_LOCAL_USERS_COMMAND);
		outcome.addSentCommand(LIST_LOCAL_USERS_COMMAND);
		String response = io.awaitMultilineCommands(sessionId);
		if (!response.isEmpty() && response.contains(WindowsConstants.WINDOWS_SYSTEM_COMMAND_COMPLETE_MSG)) {
			outcome.addResponseIo(response);
			pullUsersFromResponseStr(response, USER_TYPE.LOCAL, users);
		} else {
			throw new Exception("Cannot list local users");
		}

		io.sendCommand(sessionId, LIST_DOMAIN_USERS_COMMAND);
		outcome.addSentCommand(LIST_DOMAIN_USERS_COMMAND);
		response = io.awaitMultilineCommands(sessionId);
		outcome.addResponseIo(response);
		if (!response.isEmpty() && response.contains(WindowsConstants.WINDOWS_SYSTEM_COMMAND_COMPLETE_MSG)) {
			pullUsersFromResponseStr(response, USER_TYPE.DOMAIN, users);
		} else {
			outcome.addMacroMessage("No domain available");
		}

		return users;
	}

	public List<Group> getGroupList(int sessionId, MacroOutcome outcome) throws Exception {
		List<Group> groups = new ArrayList<>();

		io.sendCommand(sessionId, LIST_LOCAL_GROUPS_COMMAND);
		outcome.addSentCommand(LIST_LOCAL_GROUPS_COMMAND);
		String response = io.awaitMultilineCommands(sessionId);
		if (!response.isEmpty() && response.contains(WindowsConstants.WINDOWS_SYSTEM_COMMAND_COMPLETE_MSG)) {
			outcome.addResponseIo(response);
			String elements[] = response.split(System.lineSeparator());
			// Four lines are at the start of the command
			for (int idx = 4; idx < elements.length; idx++) {
				if (!elements[idx].equals(WindowsConstants.WINDOWS_SYSTEM_COMMAND_COMPLETE_MSG)) {
					String groupName = elements[idx].substring(1);
					groups.add(new Group(groupName, GROUP_TYPE.LOCAL));
				}
			}
		} else {
			throw new Exception("Cannot list local groups");
		}

		io.sendCommand(sessionId, LIST_DOMAIN_GROUPS_COMMAND);
		outcome.addSentCommand(LIST_DOMAIN_GROUPS_COMMAND);
		response = io.awaitMultilineCommands(sessionId);
		outcome.addResponseIo(response);
		if (!response.isEmpty() && response.contains(WindowsConstants.WINDOWS_SYSTEM_COMMAND_COMPLETE_MSG)) {
			// TODO: Implement ME!!!!
		} else if (response.contains(WindowsConstants.WINDOWS_DOMAIN_CONTROLLER_RESTRICTED_COMMAND_MSG)) {
			outcome.addMacroMessage("Must be on a domain controller to list groups");
		} else {
			outcome.addMacroMessage("Cannot list local groups");
		}

		return groups;
	}

	private Group findGroup(String name, List<Group> groups, Group.GROUP_TYPE gType) {
		for (Group group : groups) {
			if (group.groupname.equals(name) && group.type == gType) {
				return group;
			}
		}
		return null;
	}

	public SystemAccountProfile getSystemAccountProfile(int sessionId, MacroOutcome outcome) throws Exception {
		SystemAccountProfile profile = new SystemAccountProfile();

		try {
			List<Group> groups = getGroupList(sessionId, outcome);

			List<User> users = getUserList(sessionId, outcome);
			profile.addUserList(users);
			for (User user : users) {
				String command = ENUM_LOCAL_USER_COMMAND.replace("$USER$", user.username);
				io.sendCommand(sessionId, command);
				outcome.addSentCommand(command);
				String response = io.awaitMultilineCommands(sessionId);
				outcome.addResponseIo(response);
				if (!response.isEmpty() && response.contains(WindowsConstants.WINDOWS_SYSTEM_COMMAND_COMPLETE_MSG)) {
					String elements[] = response.split(System.lineSeparator());
					for (String element : elements) {
						if (element.startsWith(WindowsConstants.NET_USER_LOCAL_GROUP_MEMBERSHIPS)) {
							element = element.substring(WindowsConstants.NET_USER_LOCAL_GROUP_MEMBERSHIPS.length());
							String groupNames[] = element.split("\\*");
							for (int idx = 1; idx < groupNames.length; idx++) {
								String groupName = groupNames[idx].trim();
								if (!groupName.isEmpty()) {
									Group tGroup = findGroup(groupName, groups, GROUP_TYPE.LOCAL);
									if (tGroup == null) {
										tGroup = new Group(groupName, GROUP_TYPE.LOCAL);
										groups.add(tGroup);
									}
									user.addGroup(tGroup);
								}
							}
						} else if (element.startsWith(WindowsConstants.NET_USER_GLOBAL_GROUP_MEMBERSHIPS)
								&& !element.contains(" *None")) {
							element = element.substring(WindowsConstants.NET_USER_GLOBAL_GROUP_MEMBERSHIPS.length());
							String groupNames[] = element.split("\\*");
							for (int idx = 1; idx < groupNames.length; idx++) {
								String groupName = groupNames[idx].trim(); 
								if (!groupName.isEmpty()) {
									Group tGroup = findGroup(groupName, groups, GROUP_TYPE.DOMAIN);
									if (tGroup == null) {
										tGroup = new Group(groupName, GROUP_TYPE.DOMAIN);
										groups.add(tGroup);
									}
									user.addGroup(tGroup);
								}
							}
						}
					}
				}
			}
			profile.addUserList(users);
			profile.addGroupList(groups);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new Exception("Cannot build account profile: " + ex.getMessage());
		}

		outcome.addMacroMessage("Account profile complete");
		return profile;
	}

}
