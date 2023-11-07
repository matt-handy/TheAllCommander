package c2.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.List;

import c2.session.IOManager;
import util.Time;

public abstract class SocketReader {

	protected Socket socket;
	protected BufferedReader br;
	protected boolean appendFinalLineSep;
	protected boolean usesDollarSigns;

	public SocketReader(Socket socket, BufferedReader br, boolean appendFinalLineSep) {
		this.socket = socket;
		this.br = br;
		this.appendFinalLineSep = appendFinalLineSep;
	}

	public String readUnknownLinesFromSocketWithTimeout(long timeout) throws IOException {
		return readUnknownLinesFromSocket(br, socket, appendFinalLineSep, timeout);
	}

	public String readUnknownLinesFromSocket() throws IOException {
		return GenericTCPInitiator.readUnknownLinesFromSocket(br, socket, appendFinalLineSep);
	}

	public abstract void executeCd(String cd, IOManager ioManager, OutputStreamWriter bw, int sessionId)
			throws IOException;

	public abstract String getCurrentDirectoryNativeFormat(IOManager ioManager, OutputStreamWriter bw, int sessionId)
			throws IOException;

	public abstract List<String> getCurrentDirectoriesFromNativeFormatDump(String parentDirectory, String dir);

	public abstract List<String> getCurrentFilesFromNativeFormatDump(String parentDirectory, String dir);

	public abstract String uplinkFileBase64(String filename, OutputStreamWriter bw, int sessionId) throws Exception;

	public abstract String getPwd(OutputStreamWriter bw, int sessionId);

	public String readUnknownLinesFromSocket(BufferedReader br, Socket socket, boolean appendFinalLineSeparator,
			long millisToWait) throws IOException {
		if (usesDollarSigns) {
			Time.sleepWrapped(millisToWait);
		} else {
			long target = System.currentTimeMillis() + millisToWait;
			while (System.currentTimeMillis() < target && !br.ready()) {
				Time.sleepWrapped(50);
			}
		}
		return GenericTCPInitiator.readUnknownLinesFromSocket(br, socket, appendFinalLineSeparator);
	}

}
