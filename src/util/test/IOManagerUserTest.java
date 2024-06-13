package util.test;

import c2.session.IOManager;

public class IOManagerUserTest {
	protected IOManager io;
	protected int sessionId;
	
	protected void setUpManagerAndSession() throws Exception {
		io = ClientServerTest.setupDefaultIOManager();
		sessionId = io.determineAndGetCorrectSessionId("noone", "testHost", "protocol", false, null);
	}
}
