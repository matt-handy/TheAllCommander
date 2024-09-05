package c2.win;

import java.util.ArrayList;
import java.util.List;

public class WindowsQuotedServiceChecker {

	public static final String SERVICE_PATH_QUERY= "powershell -c \"Get-WmiObject win32_service | select Name, PathName\"";
	
	public static final String PATHNAME = "PathName";
	
	public static List<String> parseServicesQueryOutput(String output) throws WindowsToolOutputParseException{
		try {
			List<String> unquotedServices = new ArrayList<>();
			String lines[] = output.split("\r\n");
			int pathStartIdx = lines[1].indexOf(PATHNAME);
			//Skip the ---- line
			for(int idx = 3; idx < lines.length; idx++) {
				String line = lines[idx].trim();
				if(pathStartIdx < line.length()) {
					String pathname = line.substring(pathStartIdx);
					if(pathname.contains(".exe")) {
						pathname = pathname.substring(0, pathname.indexOf(".exe"));
					}
					if(pathname.contains(" ") && !pathname.contains("\"")) {
						//We have an unquoted path!
						unquotedServices.add(line);
					}
;				}
			}
			return unquotedServices;
		}catch(Exception ex) {
			throw new WindowsToolOutputParseException(ex.getMessage());
		}
	}
}
