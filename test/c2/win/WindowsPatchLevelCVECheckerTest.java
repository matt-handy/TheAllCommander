package c2.win;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.Test;

public class WindowsPatchLevelCVECheckerTest {
	public static final String SAMPLE_SYSTEMINFO_CVE_2019_0836 = "Host Name:                 MY_SYSTEM\r\n"
			+ "OS Name:                   Microsoft Windows 11 Home\r\n"
			+ "OS Version:                10.0.14393 N/A Build 14393\r\n"
			+ "OS Manufacturer:           Microsoft Corporation\r\n"
			+ "OS Configuration:          Standalone Workstation\r\n"
			+ "OS Build Type:             Multiprocessor Free\r\n"
			+ "Registered Owner:          someone@gmail.com\r\n"
			+ "Registered Organization:   N/A\r\n"
			+ "Product ID:                00342-21134-16801-AAOEM\r\n"
			+ "Original Install Date:     3/10/2024, 4:27:10 PM\r\n"
			+ "System Boot Time:          8/23/2024, 11:24:10 PM\r\n"
			+ "System Manufacturer:       Alienware\r\n"
			+ "System Model:              Alienware m18 R1 AMD\r\n"
			+ "System Type:               x64-based PC\r\n"
			+ "Processor(s):              1 Processor(s) Installed.\r\n"
			+ "                           [01]: AMD64 Family 25 Model 97 Stepping 2 AuthenticAMD ~2501 Mhz\r\n"
			+ "BIOS Version:              Alienware 1.13.1, 4/23/2024\r\n"
			+ "Windows Directory:         C:\\Windows\r\n"
			+ "System Directory:          C:\\Windows\\system32\r\n"
			+ "Boot Device:               \\Device\\HarddiskVolume1\r\n"
			+ "System Locale:             en-us;English (United States)\r\n"
			+ "Input Locale:              en-us;English (United States)\r\n"
			+ "Time Zone:                 (UTC-05:00) Eastern Time (US & Canada)\r\n"
			+ "Total Physical Memory:     31,937 MB\r\n"
			+ "Available Physical Memory: 17,944 MB\r\n"
			+ "Virtual Memory: Max Size:  37,057 MB\r\n"
			+ "Virtual Memory: Available: 19,975 MB\r\n"
			+ "Virtual Memory: In Use:    17,082 MB\r\n"
			+ "Page File Location(s):     C:\\pagefile.sys\r\n"
			+ "Domain:                    WORKGROUP\r\n"
			+ "Logon Server:              \\\\MY_SYSTEM\r\n"
			+ "Hotfix(s):                 1 Hotfix(s) Installed.\r\n"
			+ "                           [01]: KB4534307\r\n"
			+ "Network Card(s):           2 NIC(s) Installed.\r\n"
			+ "                           [01]: Realtek Gaming 2.5GbE Family Controller\r\n"
			+ "                                 Connection Name: Ethernet\r\n"
			+ "                                 Status:          Media disconnected\r\n"
			+ "                           [02]: VirtualBox Host-Only Ethernet Adapter\r\n"
			+ "                                 Connection Name: Ethernet 2\r\n"
			+ "                                 DHCP Enabled:    No\r\n"
			+ "                                 IP address(es)\r\n"
			+ "                                 [01]: 192.168.56.1\r\n"
			+ "                                 [02]: fe80::d5b9:7bc6:87ef:1271\r\n"
			+ "Hyper-V Requirements:      A hypervisor has been detected. Features required for Hyper-V will not be displayed.\r\n"
			+ "";
	
	private static final String SAMPLE_SYSTEMINFO_CVE_MULTI = "Host Name:                 MY_SYSTEM\r\n"
			+ "OS Name:                   Microsoft Windows 11 Home\r\n"
			+ "OS Version:                10.0.10240 N/A Build 10240\r\n"
			+ "OS Manufacturer:           Microsoft Corporation\r\n"
			+ "OS Configuration:          Standalone Workstation\r\n"
			+ "OS Build Type:             Multiprocessor Free\r\n"
			+ "Registered Owner:          someone@gmail.com\r\n"
			+ "Registered Organization:   N/A\r\n"
			+ "Product ID:                00342-21134-16801-AAOEM\r\n"
			+ "Original Install Date:     3/10/2024, 4:27:10 PM\r\n"
			+ "System Boot Time:          8/23/2024, 11:24:10 PM\r\n"
			+ "System Manufacturer:       Alienware\r\n"
			+ "System Model:              Alienware m18 R1 AMD\r\n"
			+ "System Type:               x64-based PC\r\n"
			+ "Processor(s):              1 Processor(s) Installed.\r\n"
			+ "                           [01]: AMD64 Family 25 Model 97 Stepping 2 AuthenticAMD ~2501 Mhz\r\n"
			+ "BIOS Version:              Alienware 1.13.1, 4/23/2024\r\n"
			+ "Windows Directory:         C:\\Windows\r\n"
			+ "System Directory:          C:\\Windows\\system32\r\n"
			+ "Boot Device:               \\Device\\HarddiskVolume1\r\n"
			+ "System Locale:             en-us;English (United States)\r\n"
			+ "Input Locale:              en-us;English (United States)\r\n"
			+ "Time Zone:                 (UTC-05:00) Eastern Time (US & Canada)\r\n"
			+ "Total Physical Memory:     31,937 MB\r\n"
			+ "Available Physical Memory: 17,944 MB\r\n"
			+ "Virtual Memory: Max Size:  37,057 MB\r\n"
			+ "Virtual Memory: Available: 19,975 MB\r\n"
			+ "Virtual Memory: In Use:    17,082 MB\r\n"
			+ "Page File Location(s):     C:\\pagefile.sys\r\n"
			+ "Domain:                    WORKGROUP\r\n"
			+ "Logon Server:              \\\\MY_SYSTEM\r\n"
			+ "Hotfix(s):                 1 Hotfix(s) Installed.\r\n"
			+ "                           [01]: KB4537776\r\n"
			+ "Network Card(s):           2 NIC(s) Installed.\r\n"
			+ "                           [01]: Realtek Gaming 2.5GbE Family Controller\r\n"
			+ "                                 Connection Name: Ethernet\r\n"
			+ "                                 Status:          Media disconnected\r\n"
			+ "                           [02]: VirtualBox Host-Only Ethernet Adapter\r\n"
			+ "                                 Connection Name: Ethernet 2\r\n"
			+ "                                 DHCP Enabled:    No\r\n"
			+ "                                 IP address(es)\r\n"
			+ "                                 [01]: 192.168.56.1\r\n"
			+ "                                 [02]: fe80::d5b9:7bc6:87ef:1271\r\n"
			+ "Hyper-V Requirements:      A hypervisor has been detected. Features required for Hyper-V will not be displayed.\r\n"
			+ "";
	
	@Test
	void testWindowsPatchLevelCVEChecker() {
		try {
			WindowsSystemInfoParser parser = new WindowsSystemInfoParser(SAMPLE_SYSTEMINFO_CVE_2019_0836);
			List<String> applicableCVEs = WindowsPatchLevelCVEChecker.getApplicableCVEs(parser);
			assertEquals(1, applicableCVEs.size());
			assertTrue(applicableCVEs.contains("CVE-2019-0836"));
		}catch(Exception ex) {
			fail(ex);
		}
	}
	
	@Test
	void testWindowsPatchLevelCVEChecker2() {
		try {
			WindowsSystemInfoParser parser = new WindowsSystemInfoParser(SAMPLE_SYSTEMINFO_CVE_MULTI);
			List<String> applicableCVEs = WindowsPatchLevelCVEChecker.getApplicableCVEs(parser);
			assertEquals(7, applicableCVEs.size());
			assertTrue(applicableCVEs.contains("CVE-2019-0836"));
			assertTrue(applicableCVEs.contains("CVE-2019-1130"));
			assertTrue(applicableCVEs.contains("CVE-2019-1315"));
			assertTrue(applicableCVEs.contains("CVE-2019-1388"));
			assertTrue(applicableCVEs.contains("CVE-2019-1405"));
			assertTrue(applicableCVEs.contains("CVE-2020-0668"));
			assertTrue(applicableCVEs.contains("CVE-2020-0683"));
		}catch(Exception ex) {
			fail(ex);
		}
	}
}
