package c2.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

import c2.Constants;
import util.Time;

public class WindowsSocketReader extends SocketReader {

	// This flag is set if the first line of the response is a echo of the command
	// which needs to be stripped
	private boolean removeEchoedCommand = true;

	public WindowsSocketReader(Socket socket, BufferedReader br) {
		super(socket, br, true);
	}
	
	public String readUnknownLinesFromSocket() throws IOException{
		String command = super.readUnknownLinesFromSocket();
		//System.out.println("Read: -" + command + "-");
		if(removeEchoedCommand) {
			String elements[] = command.split(System.lineSeparator());
			//Sometimes ncat and others will flush a string of chars to remind you where you are
			//flush the buffer to get clean output
			if(isResponseAPromptFlush(elements)) {
				return "";
			}
			//ncat will echo a command immediately, but you might need to wait on the response for the command.
			//with the command echo flushed, wait for the new IO to arrive
			if(elements.length == 1 && elements[0].length() != 0) {
				command = super.readUnknownLinesFromSocket();
				int retryCounter = 0;
				while(command.length() == 0 && retryCounter < Constants.getConstants().getMaxResponseWait()) {
					command = super.readUnknownLinesFromSocket();
					retryCounter += Constants.getConstants().getRepollForResponseInterval();
					Time.sleepWrapped(Constants.getConstants().getRepollForResponseInterval());
				}
				//System.out.println("Returning: -" +command  +"-");
				command = stripTrailingPrompt(command);
				//System.out.println("ReturningStripped: -" +command  +"-");
				return command;
			}
		}
		if(removeEchoedCommand && command != null && command.length() != 0) {
			int firstLineBreak = command.indexOf(System.lineSeparator());
			command = command.substring(firstLineBreak + System.lineSeparator().length());
		}
		//System.out.println("Returning: -" +command  +"-");
		command = stripTrailingPrompt(command);
		//System.out.println("ReturningStripped: -" +command  +"-");
		return command;
	}
	
	private static boolean isResponseAPromptFlush(String response[]) {
		if(response.length == 1) {
			if(response[0].startsWith("C:\\") && response[0].endsWith(">")) {
				return true;
			}
		}else if(response.length == 2) {
			if((response[0].equals("") && (response[1].startsWith("C:\\") && response[1].endsWith(">"))) ||
					(response[1].equals("") && (response[0].startsWith("C:\\") && response[0].endsWith(">")))){
				return true;
			}
		}
		return false;
	}
	
	private static String stripTrailingPrompt(String message) {
		String elements[] = message.split(System.lineSeparator());
		if(elements.length == 1 && message.length() == 0) {
			return message;
		}
		boolean dropLastLine = false;
		boolean dropPenultimateLine = false;
		if((elements[elements.length - 1].startsWith("C:\\") && elements[elements.length - 1].endsWith(">"))) {
			dropLastLine = true;
			if(elements[elements.length - 2].equals("")) {
				dropPenultimateLine = true;
			}
		}
		StringBuilder sb = new StringBuilder();
		for(int idx = 0; idx < elements.length; idx++) {
			if(idx == elements.length - 1) {
				if(!dropLastLine) {
					sb.append(elements[idx]);
					sb.append(System.lineSeparator());
				}
			}else if(idx == elements.length - 2) {
				if(!dropPenultimateLine) {
					sb.append(elements[idx]);
					sb.append(System.lineSeparator());
				}
			}else {
				sb.append(elements[idx]);
				sb.append(System.lineSeparator());
			}
		}
		return sb.toString();
	}

}
