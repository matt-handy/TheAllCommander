package c2.win.services;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import c2.win.WindowsToolOutputParseException;
import java.util.regex.Matcher;
public class WindowsServiceParser {

	private WindowsServiceParser() {
	}

	public static final String SERVICE_PATH_QUERY = "powershell -c \"Get-WmiObject win32_service | select Name, PathName | Format-Table -AutoSize\"";
	public static final String SERVICE_PATH_QUERY_CSV = "powershell -c \"Get-WmiObject win32_service | select Name, PathName | Export-CSV -Path C:\\Windows\\Tasks\\services.csv\"";
	public static final String CAT_CSV = "type C:\\Windows\\Tasks\\services.csv";
	public static final String DEL_CSV = "del C:\\Windows\\Tasks\\services.csv";
	public static final String PATHNAME = "PathName";

	private List<String> unquotedServices = new ArrayList<>();
	private List<WindowsServiceInfo> services = new ArrayList<>();

	public List<String> getUnquotedServicesList() {
		return new ArrayList<>(unquotedServices);
	}

	public List<WindowsServiceInfo> getServices() {
		return services;
	}

	private static String trimQuotes(String str) {
		if(str.startsWith("\"\"") && str.endsWith("\"\"")) {
			return str.substring(2, str.length() - 2);
		}else if(str.startsWith("\"\"") && str.endsWith("\"")) {
			return str.substring(2, str.length() - 1);
		}else if(str.startsWith("\"") && str.endsWith("\"\"")) {
			return str.substring(1, str.length() - 2);
		}else if(str.startsWith("\"") && str.endsWith("\"")) {
			return str.substring(1, str.length() - 1);
		}else {
			return str;
		}
	}
	public static WindowsServiceParser parseServicesCSVQueryOutput(String output) throws WindowsToolOutputParseException {
		try {
			WindowsServiceParser serviceParser = new WindowsServiceParser();
			String lines[] = output.split("\r\n");
			for (int idx = 2; idx < lines.length; idx++) {
				String line = lines[idx].trim();
				String elements[] = line.split(",");
				if (elements.length == 2) {
					String name = trimQuotes(elements[0]);
					String rawPathname = trimQuotes(elements[1]);
					Pattern regex = Pattern.compile("[^\\s\"']+|\"[^\"]*\"|'[^']*'");
					List<String> args = new ArrayList<>();
					String pathname = rawPathname;
					if (pathname.contains(".exe")) {
						pathname = pathname.substring(0, pathname.indexOf(".exe") + 4);
						if(pathname.startsWith("\"")) {
							pathname = pathname.substring(1);
						}else {
							if(pathname.contains(" ")) {
								serviceParser.unquotedServices.add(line);
							}
						}
						if(rawPathname.indexOf(".exe") + 4 < rawPathname.length()) {
						Matcher regexMatcher = regex.matcher(rawPathname.substring(rawPathname.indexOf(".exe") + 4));
							while(regexMatcher.find()) {
								args.add(regexMatcher.group());
							}
						}
					} 
					serviceParser.services.add(new WindowsServiceInfo(name, rawPathname, pathname, args));
				}
			}
			return serviceParser;
		} catch (Exception ex) {
			throw new WindowsToolOutputParseException(ex.getMessage());
		}
	}
	
	//Deprecated
	public static WindowsServiceParser parseServicesQueryOutput(String output) throws WindowsToolOutputParseException {
		try {
			WindowsServiceParser serviceParser = new WindowsServiceParser();
			String lines[] = output.split("\r\n");
			int pathStartIdx = lines[1].indexOf(PATHNAME);
			// Skip the ---- line
			for (int idx = 3; idx < lines.length; idx++) {
				String line = lines[idx].trim();
				if (pathStartIdx < line.length()) {
					String name = line.substring(0, pathStartIdx).trim();
					String pathname = line.substring(pathStartIdx);
					if (pathname.contains(".exe")) {
						String fullArgument = pathname;
						pathname = pathname.substring(0, pathname.indexOf(".exe") + 4);
						if(pathname.startsWith("\"")) {
							pathname = pathname.substring(1);
						}else {
							if(pathname.contains(" ")) {
								serviceParser.unquotedServices.add(line);
							}
						}
						serviceParser.services.add(new WindowsServiceInfo(name, fullArgument, pathname, new ArrayList<>()));
					} else {
						serviceParser.services.add(new WindowsServiceInfo(name, pathname, pathname, new ArrayList<>()));
					}
					
				}
			}
			return serviceParser;
		} catch (Exception ex) {
			throw new WindowsToolOutputParseException(ex.getMessage());
		}
	}
}
