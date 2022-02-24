package c2.portforward.socks;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import c2.session.CommandLoader;
import c2.session.IOManager;
import util.test.socks5.SocksClientEmulator;
import util.test.socks5.TargetDaemonEmulator;
import util.test.socks5.TargetServerEmulator;

class LocalSocksListenerTest {

	@Test
	void testHostnameSocksToDaemon() {
		//System.out.println("testHostnameSocksToDaemon");
		IOManager io = new IOManager(Paths.get("test", "log"),
				new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>()));
		io.addSession("fake", "fake", "fake");
		ExecutorService service = Executors.newCachedThreadPool();

		TargetDaemonEmulator targetDaemon = new TargetDaemonEmulator(io, 2, 1, false, false);
		service.submit(targetDaemon);

		LocalSocksListener localSocks = new LocalSocksListener(io, 9000, 2, false);
		service.submit(localSocks);
		localSocks.awaitStartup();

		SocksClientEmulator clientEmulator = new SocksClientEmulator(9000, false, targetDaemon, false);
		service.submit(clientEmulator);
		//System.out.println("Awaiting client end");
		assertTrue(clientEmulator.isComplete());
		//System.out.println("Client ended");
		localSocks.kill();
		//System.out.println("LocalSocksListener shut down");
	}

	@Test
	void testDomainNameSocks() {
		//System.out.println("testDomainNameSocks");
		IOManager io = new IOManager(Paths.get("test", "log"),
				new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>()));
		LocalSocksListener localSocks = new LocalSocksListener(io, 9000, 0, true);
		ExecutorService service = Executors.newCachedThreadPool();
		service.submit(localSocks);
		localSocks.awaitStartup();
		
		TargetServerEmulator targetService = new TargetServerEmulator(4096, false);
		service.submit(targetService);
		targetService.awaitStartup();

		SocksClientEmulator clientEmulator = new SocksClientEmulator(9000, false, targetService, false);
		service.submit(clientEmulator);
		//System.out.println("Awaiting client end");
		assertTrue(clientEmulator.isComplete());
		//System.out.println("Client ended");
		localSocks.kill();
		//System.out.println("LocalSocksListener shut down");
	}

	@Test
	void testStandardIpv4Socks() {
		//System.out.println("testStandardIpv4Socks");
		IOManager io = new IOManager(Paths.get("test", "log"),
				new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>()));
		ExecutorService service = Executors.newCachedThreadPool();

		TargetServerEmulator targetService = new TargetServerEmulator(4096, false);
		service.submit(targetService);
		targetService.awaitStartup();

		LocalSocksListener localSocks = new LocalSocksListener(io, 9000, 0, true);
		service.submit(localSocks);
		localSocks.awaitStartup();
		
		SocksClientEmulator clientEmulator = new SocksClientEmulator(9000, true, targetService, false);
		service.submit(clientEmulator);
		//System.out.println("Awaiting client end");
		assertTrue(clientEmulator.isComplete());
		//System.out.println("Client ended");
		localSocks.kill();
		//System.out.println("LocalSocksListener shut down");
	}

	@Test
	void testStandardIpv4SocksToDaemon() {
		//System.out.println("testStandardIpv4SocksToDaemon");
		IOManager io = new IOManager(Paths.get("test", "log"),
				new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>()));
		io.addSession("fake", "fake", "fake");
		ExecutorService service = Executors.newCachedThreadPool();

		TargetDaemonEmulator targetDaemon = new TargetDaemonEmulator(io, 2, 1, true, false);
		service.submit(targetDaemon);

		LocalSocksListener localSocks = new LocalSocksListener(io, 9000, 2, false);
		service.submit(localSocks);
		localSocks.awaitStartup();
		
		SocksClientEmulator clientEmulator = new SocksClientEmulator(9000, true, targetDaemon, false);
		service.submit(clientEmulator);
		//System.out.println("Awaiting client end");
		assertTrue(clientEmulator.isComplete());
		//System.out.println("Client ended");
		localSocks.kill();
		//System.out.println("LocalSocksListener shut down");
	}

	@Test
	void testLocalSocksConnectionBrokenWithServer() {
		//System.out.println("testLocalSocksConnectionBrokenWithServer");
		IOManager io = new IOManager(Paths.get("test", "log"),
				new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>()));
		ExecutorService service = Executors.newCachedThreadPool();

		TargetServerEmulator targetService = new TargetServerEmulator(4096, true);
		service.submit(targetService);
		targetService.awaitStartup();

		LocalSocksListener localSocks = new LocalSocksListener(io, 9000, 0, true);
		service.submit(localSocks);
		localSocks.awaitStartup();
		
		SocksClientEmulator clientEmulator = new SocksClientEmulator(9000, true, targetService, true);
		service.submit(clientEmulator);
		System.out.println("Awaiting client end");
		assertTrue(clientEmulator.isComplete());
		System.out.println("Client ended");
		localSocks.kill();
		System.out.println("LocalSocksListener shut down");
	}

	@Test
	void testDaemonConnectionBrokenWithServer() {
		//System.out.println("testDaemonConnectionBrokenWithServer");
		IOManager io = new IOManager(Paths.get("test", "log"),
				new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>()));
		io.addSession("fake", "fake", "fake");
		ExecutorService service = Executors.newCachedThreadPool();

		TargetDaemonEmulator targetDaemon = new TargetDaemonEmulator(io, 2, 1, true, true);
		service.submit(targetDaemon);

		LocalSocksListener localSocks = new LocalSocksListener(io, 9000, 2, false);
		service.submit(localSocks);
		localSocks.awaitStartup();
		
		SocksClientEmulator clientEmulator = new SocksClientEmulator(9000, true, targetDaemon, true);
		service.submit(clientEmulator);
		System.out.println("Awaiting client end");
		assertTrue(clientEmulator.isComplete());
		System.out.println("Client ended");
		localSocks.kill();
		System.out.println("LocalSocksListener shut down");
	}

}
