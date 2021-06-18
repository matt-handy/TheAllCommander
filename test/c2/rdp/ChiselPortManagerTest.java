package c2.rdp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChiselPortManagerTest {

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test() {
		try {
			ChiselPortManager manager = ChiselPortManager.loadFromConfig(Paths.get("test", "test_rdp_persist"));
			assertEquals(manager.getInfo().size(), 1);
			RDPSessionInfo info = manager.getInfo().iterator().next();
			assertEquals(info.sessionId, "Session:larry:UDP");
			assertEquals(info.localForwardPort, 125);
			assertEquals(info.localClientIncomingPort, 126);
			
			//Test that we can add a new manager to the file, then load fresh and see that the persistence worked
			RDPSessionInfo newInfo = new RDPSessionInfo("barf", 127, 128);
			manager.addRDPSessionInfo(newInfo);
			ChiselPortManager managerReloaded = ChiselPortManager.loadFromConfig(Paths.get("test", "test_rdp_persist"));
			assertEquals(managerReloaded.getInfo().size(), 2);
			for(RDPSessionInfo newInfos : manager.getInfo()) {
				if(newInfos.sessionId.equals("Session:larry:UDP")) {
					assertEquals(newInfos.localClientIncomingPort, 126);
					assertEquals(newInfos.localForwardPort, 125);
				}else if(newInfos.sessionId.equals("barf")) {
					assertEquals(newInfos.localClientIncomingPort, 128);
					assertEquals(newInfos.localForwardPort, 127);
				}else {
					fail("Loaded incorrect information");
				}
			}
			
			//Delete the new info, then load peristence again and validate we're back to scratch
			manager.removeRDPSessionInfo("barf");
			managerReloaded = ChiselPortManager.loadFromConfig(Paths.get("test", "test_rdp_persist"));
			assertEquals(managerReloaded.getInfo().size(), 1);
			for(RDPSessionInfo newInfos : manager.getInfo()) {
				if(newInfos.sessionId.equals("Session:larry:UDP")) {
					assertEquals(newInfos.localClientIncomingPort, 126);
					assertEquals(newInfos.localForwardPort, 125);
				}else if(newInfos.sessionId.equals("barf")) {
					assertEquals(newInfos.localClientIncomingPort, 128);
					assertEquals(newInfos.localForwardPort, 127);
				}else {
					fail("Loaded incorrect information");
				}
			}
			
		}catch(Exception ex) {
			fail(ex.getMessage());
		}
	}
	
	@Test
	void testBadLoad() {
		try {
			ChiselPortManager manager = ChiselPortManager.loadFromConfig(Paths.get("nonexistent_file"));
			fail("You shouldn't have this object: " + manager.toString());
		}catch(Exception ex) {
			assertEquals(ex.getMessage(), "Give me a real file path to load chisel configs");
		}
	}

}
