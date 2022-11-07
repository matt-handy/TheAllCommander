package c2.session.macro.users;

import java.util.ArrayList;
import java.util.List;

public class User {

	public enum USER_TYPE {LOCAL, DOMAIN};
	public final String username;
	public final USER_TYPE type;
	
	private List<Group> groups = new ArrayList<>();
	
	public User(String username, USER_TYPE type) {
		this.username = username;
		this.type = type;
	}
	
	public void addGroup(Group group) {
		groups.add(group);
	}
	
	public List<Group> getGroups(){
		return new ArrayList<>(groups);
	}
}
