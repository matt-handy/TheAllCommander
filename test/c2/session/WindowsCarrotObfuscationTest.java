package c2.session;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import c2.session.log.IOLogger;
import util.Time;

class WindowsCarrotObfuscationTest {

	private class CommandClientToggleEmulator implements Runnable {
		@Override
		public void run() {
			Time.sleepWrapped(1000);
			try {
				Socket sock = new Socket("127.0.0.1", 40445);
				BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				br.readLine();//Flush banner and ensure ready to read.
				PrintWriter pw = new PrintWriter(sock.getOutputStream());
				pw.println("net user");
				pw.println("<start esc>");
				pw.println("net user");
				pw.println("net user");
				pw.println("<end esc>");
				pw.println("net user");
				pw.println("qSession");
				pw.flush();
				pw.close();
				sock.close();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private class CommandClientCmdEmulator implements Runnable {

		@Override
		public void run() {
			Time.sleepWrapped(1000);
			try {
				Socket sock = new Socket("127.0.0.1", 40444);
				BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				br.readLine();//Flush banner and ensure ready to read.
				PrintWriter pw = new PrintWriter(sock.getOutputStream());
				pw.println("<esc> net user");
				pw.println("qSession");
				pw.flush();
				pw.close();
				sock.close();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	@Test
	void testBasicEscInsertedStandalone() {
		for(int i = 0; i < 1000; i++) {
			String product = WindowsCarrotObfuscation.obfuscate("This is command");
			assertTrue(product.contains("^"));
			assertEquals("This is command", product.replace("^", ""));
		}
	}
	
	@Test
	void testBasicEscInsertedWithSession() {
		IOManager io = new IOManager(new IOLogger(Paths.get("test", "log")), new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>()));
		int id = io.addSession("fake", "fake", "fake");
		
		ExecutorService executor = Executors.newCachedThreadPool();
		CommandClientCmdEmulator em = new CommandClientCmdEmulator();
		executor.submit(em);
		
		try {
		ServerSocket ss = new ServerSocket(40444);
		Socket sock = ss.accept();
		
		CommandMacroManager cmm = new CommandMacroManager(null, io, "test");//test dir should not be populated with any harvest
		SessionHandler handler = new SessionHandler(io, sock, id, "Irrelevant", cmm);
		executor.submit(handler);
		
		String command = io.pollCommand(id);
		while(command == null) {
			command = io.pollCommand(id);
		}
		assertTrue(command.contains("^"));
		assertEquals("net user", command.replace("^", ""));
		
		ss.close();
		}catch(Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
		
	}
	
	@Test
	void testStartAndStopObfuscation() {
		IOManager io = new IOManager(new IOLogger(Paths.get("test", "log")), new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>()));
		int id = io.addSession("fake", "fake", "fake");
		
		ExecutorService executor = Executors.newCachedThreadPool();
		CommandClientToggleEmulator em = new CommandClientToggleEmulator();
		executor.submit(em);
		
		try {
		ServerSocket ss = new ServerSocket(40445);
		Socket sock = ss.accept();
		
		CommandMacroManager cmm = new CommandMacroManager(null, io, "test");//test dir should not be populated with any harvest
		SessionHandler handler = new SessionHandler(io, sock, id, "Irrelevant", cmm);
		executor.submit(handler);
		
		String command = io.pollCommand(id);
		while(command == null) {
			command = io.pollCommand(id);
		}
		assertFalse(command.contains("^"));
		assertEquals("net user", command.replace("^", ""));
		
		command = io.pollCommand(id);
		while(command == null) {
			command = io.pollCommand(id);
		}
		assertTrue(command.contains("^"));
		assertEquals("net user", command.replace("^", ""));
		
		command = io.pollCommand(id);
		while(command == null) {
			command = io.pollCommand(id);
		}
		assertTrue(command.contains("^"));
		assertEquals("net user", command.replace("^", ""));
		
		command = io.pollCommand(id);
		while(command == null) {
			command = io.pollCommand(id);
		}
		assertFalse(command.contains("^"));
		assertEquals("net user", command.replace("^", ""));
		
		ss.close();
		}catch(Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}

}
