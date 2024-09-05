package c2.win;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

public class WindowsQuotedServiceCheckerTest {

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
			List<String> services = WindowsQuotedServiceChecker.parseServicesQueryOutput(ALL_QUOTED_SVCS);
			assertEquals(0, services.size());
		}catch(WindowsToolOutputParseException ex) {
			fail(ex.getMessage());
		}
	}
	
	@Test
	void testMisQuoted() {
		try {
			List<String> services = WindowsQuotedServiceChecker.parseServicesQueryOutput(MISSING_QUOTED_SVCS);
			assertEquals(1, services.size());
			assertTrue(services.get(0).contains("DellClientManagementService"));
		}catch(WindowsToolOutputParseException ex) {
			fail(ex.getMessage());
		}
	}

}
