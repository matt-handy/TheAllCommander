package c2.session.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import c2.session.Session;

public class IOLogger {
	public static final SimpleDateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
	private Path logRoot;
	private Map<Session, File> logs = new HashMap<>();
	
	public IOLogger(Path logRoot) {
		this.logRoot = logRoot;
		if (Files.notExists(logRoot)) {
			try {
				Files.createDirectories(logRoot);
			} catch (IOException e) {
				System.out.println("Unable to create logging root");
				e.printStackTrace();
			}
		}
	}
	
	public void writeReceivedIO(String io, Session session) throws Exception {
		String message = "Received IO: '" + io + "'";
		writeLog(message, session);
	}

	public void writeSendCommand(String cmd, Session session) throws Exception {
		String message = "Send command: '" + cmd + "'";
		writeLog(message, session);
	}

	public void writeLog(String message, Session session) throws Exception {
		File logFile = logs.get(session);
		if (logFile == null) {
			Path fileRelativePath = Paths.get(session.hostname, session.protocol + session.id);
			Path logFilePath = logRoot.resolve(fileRelativePath);
			if (Files.exists(logFilePath)) {
				logFile = logFilePath.toFile();
			} else {
				Files.createDirectories(logFilePath.getParent());
				logFile = Files.createFile(logFilePath).toFile();
			}
		}

		FileWriter fw = new FileWriter(logFile, true);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(ISO8601.format(new Date()));
		bw.write(" ");
		bw.write(message);
		bw.write(System.lineSeparator());
		bw.close();
	}
	
}
