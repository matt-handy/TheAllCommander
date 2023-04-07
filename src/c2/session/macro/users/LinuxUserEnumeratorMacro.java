package c2.session.macro.users;

import java.util.ArrayList;
import java.util.List;

import c2.session.IOManager;
import c2.session.macro.MacroOutcome;
import c2.session.macro.users.Group.GROUP_TYPE;
import c2.session.macro.users.User.USER_TYPE;

public class LinuxUserEnumeratorMacro {

	private IOManager io;
	
	public static final String LIST_LOCAL_GROUPS_COMMAND = "cat /etc/group";
	public static final String LIST_LOCAL_USERS_COMMAND = "cat /etc/passwd";
	
	public static final String LIST_LOCAL_GROUP_FOR_USER_COMMAND = "groups $USER$";
	
	public LinuxUserEnumeratorMacro(IOManager io) {
		this.io = io;
	}
	
	public List<Group> getGroupList(int sessionId, MacroOutcome outcome) throws Exception {
		List<Group> groups = new ArrayList<>();
		
		io.sendCommand(sessionId, LIST_LOCAL_GROUPS_COMMAND);
		outcome.addSentCommand(LIST_LOCAL_GROUPS_COMMAND);
		String response = io.awaitMultilineCommands(sessionId);
		outcome.addResponseIo(response);
		if(response.isEmpty()) {
			throw new Exception("Cannot list local groups");
		}
		for(String line : response.split(System.lineSeparator())) {
			//System.out.println(line);
			String elements[] = line.split(":");
			if(elements.length != 3 && elements.length != 4) {
				//System.out.println("Continue: " + line);
				continue;
			}
			groups.add(new Group(elements[0], GROUP_TYPE.LOCAL));
		}
		
		return groups;
	}
	
	public List<User> getUserList(int sessionId, MacroOutcome outcome) throws Exception {
		List<User> users = new ArrayList<>();
		io.sendCommand(sessionId, LIST_LOCAL_USERS_COMMAND);
		outcome.addSentCommand(LIST_LOCAL_USERS_COMMAND);
		String response = io.awaitMultilineCommands(sessionId);
		outcome.addResponseIo(response);
		if(!response.isEmpty()) {
			String[] lines = response.split(System.lineSeparator());
			for(String line : lines) {
				String[] elements = line.split(":");
				if(elements.length == 7) {
					users.add(new User(elements[0], USER_TYPE.LOCAL));
				}
			}
		}else {
			throw new Exception("Cannot list local users");
		}
		return users;
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
			
			for(User user : users) {
				String command = LIST_LOCAL_GROUP_FOR_USER_COMMAND.replace("$USER$", user.username);
				io.sendCommand(sessionId, command);
				outcome.addSentCommand(command);
				String response = io.awaitMultilineCommands(sessionId);
				outcome.addResponseIo(response);
				if (!response.isEmpty()) {
					String groupsStr = response.trim().split(" : ")[1];
					String[] groupStrs = groupsStr.split(" ");
					for(String group : groupStrs) {
						Group g = findGroup(group, groups, GROUP_TYPE.LOCAL);
						user.addGroup(g);
					}
				}
			}
			profile.addUserList(users);
			profile.addGroupList(groups);
		} catch (Exception ex) {
			throw new Exception("Cannot build account profile: " + ex.getMessage());
		}
		
		outcome.addMacroMessage("Account profile complete");
		return profile;
	}
}
