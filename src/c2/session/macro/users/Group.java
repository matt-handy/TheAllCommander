package c2.session.macro.users;

public class Group {
	public enum GROUP_TYPE {LOCAL, DOMAIN};
	public final String groupname;
	public final GROUP_TYPE type;
	
	public Group(String groupname, GROUP_TYPE type) {
		this.groupname = groupname;
		this.type = type;
	}
}
