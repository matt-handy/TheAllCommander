package c2.win;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import c2.win.services.WindowsServiceInfo;
import c2.win.services.WindowsServiceParser;

public class WindowsQuotedServiceCheckerTest {
	public static final String ALL_QUOTED_SVCS_CSV ="#TYPE Selected.System.Management.ManagementObject\r\n"
			+ "\"Name\",\"PathName\"\r\n"
			+ "\"AJRouter\",\"C:\\Windows\\system32\\svchost.exe -k LocalServiceNetworkRestricted -p\"\r\n"
			+ "\"ALG\",\"C:\\Windows\\System32\\alg.exe\"\r\n"
			+ "\"Alienware Digital Delivery Services\",\"\"\"C:\\Program Files (x86)\\Alienware Digital Delivery Services\\Dell.D3.WinSvc.exe\"\"\"\r\n"
			+ "\"Alienware SupportAssist Remediation\",\"\"\"C:\\Program Files\\Dell\\SARemediation\\agent\\DellSupportAssistRemedationService.exe\"\"\"\r\n"
			+ "\"AMD Crash Defender Service\",\"C:\\Windows\\System32\\amdfendrsr.exe\"\r\n"
			+ "\"AMD External Events Utility\",\"C:\\Windows\\System32\\DriverStore\\FileRepository\\u0405029.inf_amd64_af0fbd4ef3690c3f\\B400324\\atiesrxx.exe\"\r\n"
			+ "\"CertPropSvc\",\"C:\\Windows\\system32\\svchost.exe -k netsvcs\"\r\n"
			+ "\"ClickToRunSvc\",\"\"\"C:\\Program Files\\Common Files\\Microsoft Shared\\ClickToRun\\OfficeClickToRun.exe\"\" /service\"\r\n"
			+ "\"ClipSVC\",\"C:\\Windows\\System32\\svchost.exe -k wsappx -p\"\r\n"
			+ "\"COMSysApp\",\"C:\\Windows\\system32\\dllhost.exe /Processid:{02D4B3F1-FD88-11D1-960D-00805FC79235}\"\r\n"
			+ "\"CoreMessagingRegistrar\",\"C:\\Windows\\system32\\svchost.exe -k LocalServiceNoNetwork -p\"\r\n"
			+ "\"CryptSvc\",\"C:\\Windows\\system32\\svchost.exe -k NetworkService -p\"\r\n"
			+ "\"DcomLaunch\",\"C:\\Windows\\system32\\svchost.exe -k DcomLaunch -p\"\r\n"
			+ "\"dcsvc\",\"C:\\Windows\\system32\\svchost.exe -k netsvcs -p\"\r\n"
			+ "\"DDVCollectorSvcApi\",\"\"\"C:\\Program Files\\Dell\\DellDataVault\\DDVCollectorSvcApi.exe\"\"\"\r\n"
			+ "\"DDVDataCollector\",\"\"\"C:\\Program Files\\Dell\\DellDataVault\\DDVDataCollector.exe\"\"\"\r\n";
	
	public static final String ALL_QUOTED_SVCS_CSV_ACCESSIBLE_ARG ="#TYPE Selected.System.Management.ManagementObject\r\n"
			+ "\"Name\",\"PathName\"\r\n"
			+ "\"AJRouter\",\"C:\\Windows\\system32\\svchost.exe -k LocalServiceNetworkRestricted -p\"\r\n"
			+ "\"ALG\",\"C:\\Windows\\System32\\alg.exe\"\r\n"
			+ "\"Alienware Digital Delivery Services\",\"\"\"C:\\Program Files (x86)\\Alienware Digital Delivery Services\\Dell.D3.WinSvc.exe\"\"\"\r\n"
			+ "\"Alienware SupportAssist Remediation\",\"\"\"C:\\Program Files\\Dell\\SARemediation\\agent\\DellSupportAssistRemedationService.exe\"\"\"\r\n"
			+ "\"AMD Crash Defender Service\",\"C:\\Windows\\System32\\amdfendrsr.exe\"\r\n"
			+ "\"AMD External Events Utility\",\"C:\\Windows\\System32\\DriverStore\\FileRepository\\u0405029.inf_amd64_af0fbd4ef3690c3f\\B400324\\atiesrxx.exe\"\r\n"
			+ "\"CertPropSvc\",\"C:\\Windows\\system32\\svchost.exe -k netsvcs\"\r\n"
			+ "\"ClickToRunSvc\",\"\"\"C:\\Program Files\\Common Files\\Microsoft Shared\\ClickToRun\\OfficeClickToRun.exe\"\" /service\"\r\n"
			+ "\"ClipSVC\",\"C:\\Windows\\System32\\svchost.exe -k wsappx -p\"\r\n"
			+ "\"COMSysApp\",\"C:\\Windows\\system32\\dllhost.exe /Processid:{02D4B3F1-FD88-11D1-960D-00805FC79235}\"\r\n"
			+ "\"CoreMessagingRegistrar\",\"C:\\Windows\\system32\\svchost.exe -k LocalServiceNoNetwork -p\"\r\n"
			+ "\"CryptSvc\",\"C:\\Windows\\system32\\svchost.exe -k NetworkService -p\"\r\n"
			+ "\"DcomLaunch\",\"C:\\Windows\\system32\\svchost.exe -k DcomLaunch -p\"\r\n"
			+ "\"FakeSvc\",\"C:\\Windows\\system32\\fake.exe C:\\Windows\\Tasks\\problem.xml\"\r\n"
			+ "\"DDVCollectorSvcApi\",\"\"\"C:\\Program Files\\Dell\\DellDataVault\\DDVCollectorSvcApi.exe\"\"\"\r\n"
			+ "\"DDVDataCollector\",\"\"\"C:\\Program Files\\Dell\\DellDataVault\\DDVDataCollector.exe\"\"\"\r\n";
	
	public static final String MISSING_QUOTED_SVCS_CSV ="#TYPE Selected.System.Management.ManagementObject\r\n"
			+ "\"Name\",\"PathName\"\r\n"
			+ "\"AJRouter\",\"C:\\Windows\\system32\\svchost.exe -k LocalServiceNetworkRestricted -p\"\r\n"
			+ "\"ALG\",\"C:\\Windows\\System32\\alg.exe\"\r\n"
			+ "\"Alienware Digital Delivery Services\",\"\"\"C:\\Program Files (x86)\\Alienware Digital Delivery Services\\Dell.D3.WinSvc.exe\"\"\"\r\n"
			+ "\"Alienware SupportAssist Remediation\",\"\"\"C:\\Program Files\\Dell\\SARemediation\\agent\\DellSupportAssistRemedationService.exe\"\"\"\r\n"
			+ "\"AMD Crash Defender Service\",\"C:\\Windows\\System32\\amdfendrsr.exe\"\r\n"
			+ "\"AMD External Events Utility\",\"C:\\Windows\\System32\\DriverStore\\FileRepository\\u0405029.inf_amd64_af0fbd4ef3690c3f\\B400324\\atiesrxx.exe\"\r\n"
			+ "\"CertPropSvc\",\"C:\\Windows\\system32\\svchost.exe -k netsvcs\"\r\n"
			+ "\"ClickToRunSvc\",\"\"\"C:\\Program Files\\Common Files\\Microsoft Shared\\ClickToRun\\OfficeClickToRun.exe\"\" /service\"\r\n"
			+ "\"ClipSVC\",\"C:\\Windows\\System32\\svchost.exe -k wsappx -p\"\r\n"
			+ "\"COMSysApp\",\"C:\\Windows\\system32\\dllhost.exe /Processid:{02D4B3F1-FD88-11D1-960D-00805FC79235}\"\r\n"
			+ "\"CoreMessagingRegistrar\",\"C:\\Windows\\system32\\svchost.exe -k LocalServiceNoNetwork -p\"\r\n"
			+ "\"CryptSvc\",\"C:\\Windows\\system32\\svchost.exe -k NetworkService -p\"\r\n"
			+ "\"DcomLaunch\",\"C:\\Windows\\system32\\svchost.exe -k DcomLaunch -p\"\r\n"
			+ "\"dcsvc\",\"C:\\Windows\\system32\\svchost.exe -k netsvcs -p\"\r\n"
			+ "\"DDVCollectorSvcApi\",\"C:\\Program Files\\Dell\\DellDataVault\\DDVCollectorSvcApi.exe\"\r\n"
			+ "\"DDVDataCollector\",\"\"\"C:\\Program Files\\Dell\\DellDataVault\\DDVDataCollector.exe\"\"\"\r\n";
	
	public static final String ALL_QUOTED_SVCS = "\r\n"
			+ "Name                                      PathName\r\n"
			+ "----                                      --------\r\n"
			+ "AJRouter                                  C:\\Windows\\system32\\svchost.exe -k LocalServiceNetworkRestricted -p\r\n"
			+ "ALG                                       C:\\Windows\\System32\\alg.exe\r\n"
			+ "CertPropSvc                               C:\\Windows\\system32\\svchost.exe -k netsvcs\r\n"
			+ "ClipSVC                                   C:\\Windows\\System32\\svchost.exe -k wsappx -p\r\n"
			+ "CoreMessagingRegistrar                    C:\\Windows\\system32\\svchost.exe -k LocalServiceNoNetwork -p\r\n"
			+ "CryptSvc                                  C:\\Windows\\system32\\svchost.exe -k NetworkService -p\r\n"
			+ "DcomLaunch                                C:\\Windows\\system32\\svchost.exe -k DcomLaunch -p\r\n"
			+ "dcsvc                                     C:\\Windows\\system32\\svchost.exe -k netsvcs -p\r\n"
			+ "DDVCollectorSvcApi                        \"C:\\Program Files\\Dell\\DellDataVault\\DDVCollectorSvcApi.exe\"\r\n"
			+ "DDVDataCollector                          \"C:\\Program Files\\Dell\\DellDataVault\\DDVDataCollector.exe\"\r\n"
			+ "DDVRulesProcessor                         \"C:\\Program Files\\Dell\\DellDataVault\\DDVRulesProcessor.exe\"\r\n"
			+ "defragsvc                                 C:\\Windows\\system32\\svchost.exe -k defragsvc\r\n"
			+ "DellClientManagementService               \"C:\\Program Files (x86)\\Dell\\UpdateService\\ServiceShell.exe\"\r\n"
			+ "\r\n";
	
	public static final String MISSING_QUOTED_SVCS = "\r\n"
			+ "Name                                      PathName\r\n"
			+ "----                                      --------\r\n"
			+ "AJRouter                                  C:\\Windows\\system32\\svchost.exe -k LocalServiceNetworkRestricted -p\r\n"
			+ "ALG                                       C:\\Windows\\System32\\alg.exe\r\n"
			+ "CertPropSvc                               C:\\Windows\\system32\\svchost.exe -k netsvcs\r\n"
			+ "ClipSVC                                   C:\\Windows\\System32\\svchost.exe -k wsappx -p\r\n"
			+ "CoreMessagingRegistrar                    C:\\Windows\\system32\\svchost.exe -k LocalServiceNoNetwork -p\r\n"
			+ "CryptSvc                                  C:\\Windows\\system32\\svchost.exe -k NetworkService -p\r\n"
			+ "DcomLaunch                                C:\\Windows\\system32\\svchost.exe -k DcomLaunch -p\r\n"
			+ "dcsvc                                     C:\\Windows\\system32\\svchost.exe -k netsvcs -p\r\n"
			+ "DDVCollectorSvcApi                        \"C:\\Program Files\\Dell\\DellDataVault\\DDVCollectorSvcApi.exe\"\r\n"
			+ "DDVDataCollector                          \"C:\\Program Files\\Dell\\DellDataVault\\DDVDataCollector.exe\"\r\n"
			+ "DDVRulesProcessor                         \"C:\\Program Files\\Dell\\DellDataVault\\DDVRulesProcessor.exe\"\r\n"
			+ "defragsvc                                 C:\\Windows\\system32\\svchost.exe -k defragsvc\r\n"
			+ "DellClientManagementService               C:\\Program Files (x86)\\Dell\\UpdateService\\ServiceShell.exe\r\n"
			+ "\r\n";
	
	@Test
	void testAllQuoted() {
		try {
			List<String> services = WindowsServiceParser.parseServicesQueryOutput(ALL_QUOTED_SVCS).getUnquotedServicesList();
			assertEquals(0, services.size());
		}catch(WindowsToolOutputParseException ex) {
			fail(ex.getMessage());
		}
	}
	
	@Test
	void testMisQuoted() {
		try {
			List<String> services = WindowsServiceParser.parseServicesQueryOutput(MISSING_QUOTED_SVCS).getUnquotedServicesList();
			assertEquals(1, services.size());
			assertTrue(services.get(0).contains("DellClientManagementService"));
		}catch(WindowsToolOutputParseException ex) {
			fail(ex.getMessage());
		}
	}
	
	@Test
	void testAllQuotedCsv() {
		try {
			List<String> services = WindowsServiceParser.parseServicesCSVQueryOutput(ALL_QUOTED_SVCS_CSV).getUnquotedServicesList();
			assertEquals(0, services.size());
		}catch(WindowsToolOutputParseException ex) {
			fail(ex.getMessage());
		}
	}
	
	@Test
	void testMisQuotedCsv() {
		try {
			List<String> services = WindowsServiceParser.parseServicesCSVQueryOutput(MISSING_QUOTED_SVCS_CSV).getUnquotedServicesList();
			assertEquals(1, services.size());
			assertTrue(services.get(0).contains("DDVCollectorSvcApi"));
		}catch(WindowsToolOutputParseException ex) {
			fail(ex.getMessage());
		}
	}
	
	@Test
	void testArgumentParser() {
		try {
			WindowsServiceParser parser = WindowsServiceParser.parseServicesCSVQueryOutput(ALL_QUOTED_SVCS_CSV_ACCESSIBLE_ARG);
			boolean foundFakeSvc = false;
			for(WindowsServiceInfo info : parser.getServices()) {
				if(info.serviceName.equals("FakeSvc")) {
					foundFakeSvc = true;
					assertEquals(1, info.getServiceArgs().size());
					assertEquals("C:\\Windows\\Tasks\\problem.xml", info.getServiceArgs().get(0));
				}
			}
			assertTrue(foundFakeSvc, "Unable to find expected service");
		}catch(WindowsToolOutputParseException ex) {
			fail(ex.getMessage());
		}
	}

}
