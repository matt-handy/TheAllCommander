package c2.win;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WindowsSystemInfoParser {

	private Integer buildNumber = null;
	private List<String> hotfixes = new ArrayList<>();
	
	public WindowsSystemInfoParser(String info) throws Exception{
		String lines[] = info.split("\r\n");
		for(String line : lines) {
			if(line.startsWith("OS Version:")) {
				line = line.stripTrailing();
				String elements[] = line.split(" ");
				String buildNumberStr = elements[elements.length - 1];
				try {
					buildNumber = Integer.parseInt(buildNumberStr);
				}catch(NumberFormatException ex) {
					throw new Exception("Invalid systeminfo - windows build number cannot be parsed");
				}
			}
		}
		if(buildNumber == null) {
			throw new Exception("Invalid systeminfo - OS Version data not found");
		}
		
		Pattern p = Pattern.compile("KB[0-9]{7}", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(info);
        while(m.find()) {
        	hotfixes.add(m.group());
        }
	}
	
	private WindowsSystemInfoParser() {}

	public Integer getBuildNumber() {
		return buildNumber;
	}

	public List<String> getHotfixes() {
		return new ArrayList<>(hotfixes);
	}

	
	
}
