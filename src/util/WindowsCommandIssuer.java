package util;

import c2.Constants;
import c2.session.IOManager;

public class WindowsCommandIssuer {
	
	public static void commandIssuer(int sessionId, IOManager io, String command, String taskDesc, int milliTimeout) throws Exception{
		io.sendCommand(sessionId, command);
		for(int i = 0; i < milliTimeout / Constants.getConstants().getRepollForResponseInterval(); i++) {
			String response = io.pollIO(sessionId);
			if(response != null) {
				if(response.contains("The command completed successfully.") ||
						response.contains("The operation completed successfully.")) {
					return;
				}else {
					throw new Exception("Could not " + taskDesc + ": " + response);
				}
			}
			Time.sleepWrapped(Constants.getConstants().getRepollForResponseInterval());
		}
		throw new Exception("Could not " + taskDesc + ": No response");
	}
}
