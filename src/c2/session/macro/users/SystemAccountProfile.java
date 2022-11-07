package c2.session.macro.users;

import java.util.ArrayList;
import java.util.List;

public class SystemAccountProfile {
	
	private List<User> users;
	private List<Group> groups;
	
	public void addUserList(List<User> users) {
		this.users = new ArrayList<>();
		this.users.addAll(users);
	}
	
	public void addGroupList(List<Group> groups) {
		this.groups = new ArrayList<>();
		this.groups.addAll(groups);
	}
	
	public List<User> getUserList(){
		return new ArrayList<>(users);
	}
	
	public List<Group> getGroupList(){
		return new ArrayList<>(groups);
	}
}
