package c2.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

public class LinuxSocketReader extends SocketReader{

	private boolean usesDollarSigns;
	
	//Some shells include $ either before or after their output returns, we want to know 
	//if we need to sanitize these out
	public LinuxSocketReader(Socket socket, BufferedReader br, boolean usesDollarSigns) {
		super(socket, br, false);
		this.usesDollarSigns = usesDollarSigns;
	}
	
	public String readUnknownLinesFromSocket() throws IOException{
		String content = super.readUnknownLinesFromSocket();
		if(usesDollarSigns) {
			//Clear leading $
			while(content.startsWith("$ ")) {
				content = content.replace("$ ", "");
			}
			//Clear trailing with line sep
			String endString = "$ " + System.lineSeparator();
			while(content.endsWith(endString)) {
				content = content.substring(0, content.length() - endString.length());
				content += System.lineSeparator();
			}
			
			//Clear trailing without line sep
			while(content.endsWith("$ ")) {
				content = content.substring(0, content.length() - "$ ".length());
			}
		}
		return content;
	}
}
