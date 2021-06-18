package c2.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

public class SocketReader {

	protected Socket socket;
	protected BufferedReader br;
	protected boolean appendFinalLineSep;
	
	public SocketReader(Socket socket, BufferedReader br, boolean appendFinalLineSep) {
		this.socket = socket;
		this.br = br;
		this.appendFinalLineSep = appendFinalLineSep;
	}
	
	public String readUnknownLinesFromSocket() throws IOException{
		return GenericTCPInitiator.readUnknownLinesFromSocket(br, socket, appendFinalLineSep);
	}
}
