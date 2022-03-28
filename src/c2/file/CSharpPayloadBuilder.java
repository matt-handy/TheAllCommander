package c2.file;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CSharpPayloadBuilder {
	//TODO: Make configurable
	private static final Path CSHARP_PAYLOAD_PATH = Paths.get("config", "csharp_staging", "csharp_payload");
	
	public static String buildConfigurablePayload(Path masterImport, String commaListOfSrcFiles) throws IOException {
		String args[] = commaListOfSrcFiles.split(",");
		StringBuilder sb = new StringBuilder();
		
		BufferedReader br = new BufferedReader(new FileReader(CSHARP_PAYLOAD_PATH.resolve(masterImport).toFile()));
		String line;
		while((line = br.readLine()) != null) {
			sb.append(line);
			sb.append(System.lineSeparator());
		}
		br.close();
		
		for(String arg : args) {
			br = new BufferedReader(new FileReader(CSHARP_PAYLOAD_PATH.resolve(Paths.get(arg)).toFile()));
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
