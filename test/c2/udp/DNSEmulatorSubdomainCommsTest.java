package c2.udp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import c2.Constants;
import c2.crypto.AESEncryptor;
import c2.session.IOManager;
import util.Time;

class DNSEmulatorSubdomainCommsTest {

	public static byte[] TEST_END_PACKET = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, // Header
																														// bits
			0x03, // label len
			0x65, 0x66, 0x67, // Message
			0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, // Root domain of DNS query
			0x00, // null byte
			0x00, 0x01, 0x00, 0x01 };// Class flags

	public static byte[] TEST_MID_PACKET = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // Header
																														// bits
			0x03, // label len
			0x65, 0x66, 0x67, // Message
			0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, // Root domain of DNS query
			0x00, // null byte
			0x00, 0x01, 0x00, 0x01 };// Class flags

	@Test
	void testDetectEndPacket() {
		assertTrue(DNSEmulatorSubdomainComms.isLastInSequence(TEST_END_PACKET));
		assertFalse(DNSEmulatorSubdomainComms.isLastInSequence(TEST_MID_PACKET));
		try {
			DNSEmulatorSubdomainComms.isLastInSequence(new byte[5]);
			fail("Prior line should throw exception");
		} catch (IllegalArgumentException ex) {
			assertEquals("This is not a valid packet", ex.getMessage());
		}
	}

	@Test
	void testExtractDataSegment() {
		DNSEmulatorSubdomainComms testEmulator = new DNSEmulatorSubdomainComms();
		try (InputStream input = new FileInputStream("config" + File.separator + "test.properties")) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			testEmulator.initialize(null, prop, null, null);
			assertEquals("efg", testEmulator.extractB64Payload(TEST_END_PACKET, TEST_END_PACKET.length));

		} catch (IOException ex) {
			System.out.println("Unable to load config file");
			ex.printStackTrace();
		}

	}

	private void testTrivialMessage(Properties prop, DNSEmulatorSubdomainComms testEmulator, IOManager io, int session)
			throws IOException {
		ExecutorService service = Executors.newCachedThreadPool();
		service.submit(testEmulator);

		testEmulator.awaitStartup();

		DatagramSocket socket = new DatagramSocket();
		testInitialSessionExchangeMessage(prop, testEmulator, socket, 2, null, null);

		String message = "Test?";
		byte[] dnsId = { (byte) 0x98, (byte) 0xfa };
		List<byte[]> packets = buildMessageTransmission(message, "test.doma", dnsId, null, (short) session, false,
				null);

		InetAddress address = InetAddress.getByName("127.0.0.1");
		int port = Integer.parseInt(prop.getProperty(Constants.DNSPORT));

		for (byte[] packet : packets) {
			DatagramPacket dPacket = new DatagramPacket(packet, packet.length, address, port);
			socket.send(dPacket);
		}

		// 4 byte header + 1 first LL + last payload + fake domain + trailing null + 9
		// bytes of info
		int bytesOfPriorMessage = 4 + 1 + 1 + "Test?".length() + 10 + 1 + 9 + 1;

		byte buf[] = new byte[1000];
		DatagramPacket rPacket = new DatagramPacket(buf, buf.length);
		socket.receive(rPacket);
		String received = new String(rPacket.getData(), bytesOfPriorMessage, rPacket.getLength() - bytesOfPriorMessage);
		assertEquals("<control> No Command", received);

		String cannedResponse = "Canned reponse";
		io.sendCommand(session, cannedResponse);

		for (byte[] packet : packets) {
			DatagramPacket dPacket = new DatagramPacket(packet, packet.length, address, port);
			socket.send(dPacket);
		}

		socket.receive(rPacket);
		received = new String(rPacket.getData(), bytesOfPriorMessage, rPacket.getLength() - bytesOfPriorMessage);
		assertEquals(cannedResponse, received);

		socket.close();
		testEmulator.stop();
	}

	@Test
	void testConfiguration() {
		DNSEmulatorSubdomainComms testEmulator = new DNSEmulatorSubdomainComms();
		try (InputStream input = new FileInputStream("config" + File.separator + "test.properties")) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			// Make properties encryption go away
			prop.setProperty(Constants.WIREENCRYPTTOGGLE, "false");
			prop.setProperty(Constants.DNSPORT, "8080");

			IOManager io = new IOManager(Paths.get("test"), null);
			testEmulator.initialize(io, prop, null, null);

			testTrivialMessage(prop, testEmulator, io, 2);
		} catch (IOException ex) {
			System.out.println("Unable to load config file");
			ex.printStackTrace();
		}
	}

	@Test
	void testDnsIdExtraction() {
		byte[] dnsId = { (byte) 0x98, (byte) 0xfa };
		List<byte[]> payload = buildMessageTransmission("test", "fake.domain", dnsId, null, (short) 2, false, null);
		assertEquals(1, payload.size());
		DNSEmulatorSubdomainComms testEmulator = new DNSEmulatorSubdomainComms();
		byte[] testDnsId = testEmulator.extractDNSIdFromRequest(payload.get(0));
		assertEquals(dnsId.length, testDnsId.length);
		assertEquals(dnsId[0], testDnsId[0]);
		assertEquals(dnsId[1], testDnsId[1]);
	}

	@Test
	void testDomainNameExtraction() {
		byte[] dnsId = { (byte) 0x98, (byte) 0xfa };
		String domain = "fake.domain";
		List<byte[]> payload = buildMessageTransmission("test", domain, dnsId, null, (short) 2, true, null);
		assertEquals(1, payload.size());
		DNSEmulatorSubdomainComms testEmulator = new DNSEmulatorSubdomainComms();
		String domainEx = testEmulator.extractDomainFromRequest(payload.get(0));
		assertEquals(BOILERPLATE_HEADER + DEFAULT_UID + "test." + domain, domainEx);
	}

	@Test
	void testEncryptedMessageTransmission() {
		DNSEmulatorSubdomainComms testEmulator = new DNSEmulatorSubdomainComms();
		try (InputStream input = new FileInputStream("config" + File.separator + "test.properties")) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			IOManager io = new IOManager(Paths.get("test"), null);

			testEmulator.initialize(io, prop, null, null);
			ExecutorService service = Executors.newCachedThreadPool();
			service.submit(testEmulator);

			testEmulator.awaitStartup();
			byte[] dnsId = { (byte) 0x98, (byte) 0xfa };
			byte[] key = Base64.getDecoder().decode(prop.getProperty(Constants.WIREENCRYPTKEY));

			DatagramSocket socket = new DatagramSocket();
			testInitialSessionExchangeMessage(prop, testEmulator, socket, 2, key, null);

			String message = "Test?";

			List<byte[]> packets = buildMessageTransmission(message, "test.doma", dnsId, key, (short) 2, false, null);

			InetAddress address = InetAddress.getByName("127.0.0.1");
			int port = Integer.parseInt(prop.getProperty(Constants.DNSPORT));

			for (byte[] packet : packets) {
				DatagramPacket dPacket = new DatagramPacket(packet, packet.length, address, port);
				socket.send(dPacket);
			}

			int bytesOfPriorMessage = packets.get(packets.size() - 1).length - 1;

			byte buf[] = new byte[1000];
			DatagramPacket rPacket = new DatagramPacket(buf, buf.length);
			socket.receive(rPacket);
			String received = new String(rPacket.getData(), bytesOfPriorMessage,
					rPacket.getLength() - bytesOfPriorMessage);

			AESEncryptor encryptor = new AESEncryptor(key);
			received = encryptor.decrypt(received);
			assertEquals("<control> No Command", received);

			String cannedResponse = "Canned reponse";
			io.sendCommand(2, cannedResponse);

			for (byte[] packet : packets) {
				DatagramPacket dPacket = new DatagramPacket(packet, packet.length, address, port);
				socket.send(dPacket);
			}

			socket.receive(rPacket);
			received = new String(rPacket.getData(), bytesOfPriorMessage, rPacket.getLength() - bytesOfPriorMessage);
			received = encryptor.decrypt(received);
			assertEquals(cannedResponse, received);

			socket.close();
			testEmulator.stop();
		} catch (IOException ex) {
			System.out.println("Unable to load config file");
			ex.printStackTrace();
		}
	}

	@Test
	void testDeclaredSessionIDFormulationAndExtraction() {
		byte[] dnsId = { (byte) 0x98, (byte) 0xfa };
		List<byte[]> packets = buildMessageTransmission("Test?", "test.doma", dnsId, null, (short) 2, false, null);
		assertTrue(DNSEmulatorSubdomainComms.hasExistingSessionUID(packets.get(0)));
		assertEquals(2, DNSEmulatorSubdomainComms.getExistingSessionUID(packets.get(0)));

		packets = buildMessageTransmission("Test?", "test.doma", dnsId, null, (short) 0, true, null);
		assertFalse(DNSEmulatorSubdomainComms.hasExistingSessionUID(packets.get(0)));
		try {
			assertEquals(0, DNSEmulatorSubdomainComms.getExistingSessionUID(packets.get(0)));
			fail("Prior expression should throw exception");
		} catch (IllegalArgumentException ex) {

		}
	}

	private void testInitialSessionExchangeMessage(Properties prop, DNSEmulatorSubdomainComms testEmulator,
			DatagramSocket socket, int expectedSession, byte[] key, String customUID) throws IOException {
		String message = "<req-session>";
		byte[] dnsId = { (byte) 0x98, (byte) 0xfa };
		List<byte[]> packets = buildMessageTransmission(message, "test.doma", dnsId, key, (short) 0, true, customUID);

		InetAddress address = InetAddress.getByName("127.0.0.1");
		int port = Integer.parseInt(prop.getProperty(Constants.DNSPORT));

		for (byte[] packet : packets) {
			DatagramPacket dPacket = new DatagramPacket(packet, packet.length, address, port);
			socket.send(dPacket);
		}

		int bytesOfPriorMessage = packets.get(packets.size() - 1).length - 1;

		byte buf[] = new byte[1000];
		DatagramPacket rPacket = new DatagramPacket(buf, buf.length);
		socket.receive(rPacket);
		String received = new String(rPacket.getData(), bytesOfPriorMessage, rPacket.getLength() - bytesOfPriorMessage);
		if (key != null) {
			AESEncryptor encryptor = new AESEncryptor(key);
			received = encryptor.decrypt(received);
		}
		assertEquals(expectedSession + "", received);

	}

	@Test
	void testParallelTransmissionStreams() {
		DNSEmulatorSubdomainComms testEmulator = new DNSEmulatorSubdomainComms();
		try (InputStream input = new FileInputStream("config" + File.separator + "test.properties")) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			// Make properties encryption go away
			prop.setProperty(Constants.WIREENCRYPTTOGGLE, "false");
			IOManager io = new IOManager(Paths.get("test"), null);

			testEmulator.initialize(io, prop, null, null);

			ExecutorService service = Executors.newCachedThreadPool();
			service.submit(testEmulator);

			testEmulator.awaitStartup();

			DatagramSocket socket = new DatagramSocket();

			testInitialSessionExchangeMessage(prop, testEmulator, socket, 2, null, null);

			testInitialSessionExchangeMessage(prop, testEmulator, socket, 3, null, "987");

			for (int idx = 2; idx <= 3; idx++) {

				String message = "Test?";
				byte[] dnsId = { (byte) 0x98, (byte) 0xfa };
				List<byte[]> packets = buildMessageTransmission(message, "test.doma", dnsId, null, (short) idx, false,
						null);

				InetAddress address = InetAddress.getByName("127.0.0.1");
				int port = Integer.parseInt(prop.getProperty(Constants.DNSPORT));

				for (byte[] packet : packets) {
					DatagramPacket dPacket = new DatagramPacket(packet, packet.length, address, port);
					socket.send(dPacket);
				}

				// 4 byte header + 1 first LL + last payload + fake domain + trailing null + 9
				// bytes of info
				int bytesOfPriorMessage = 4 + 1 + 1 + "Test?".length() + 10 + 1 + 9 + 1;

				byte buf[] = new byte[1000];
				DatagramPacket rPacket = new DatagramPacket(buf, buf.length);
				socket.receive(rPacket);
				String received = new String(rPacket.getData(), bytesOfPriorMessage,
						rPacket.getLength() - bytesOfPriorMessage);
				assertEquals("<control> No Command", received);

				String cannedResponse = "Canned reponse";
				io.sendCommand(idx, cannedResponse);

				for (byte[] packet : packets) {
					DatagramPacket dPacket = new DatagramPacket(packet, packet.length, address, port);
					socket.send(dPacket);
				}

				socket.receive(rPacket);
				received = new String(rPacket.getData(), bytesOfPriorMessage,
						rPacket.getLength() - bytesOfPriorMessage);
				assertEquals(cannedResponse, received);
			}
			socket.close();
			testEmulator.stop();
		} catch (IOException ex) {
			System.out.println("Unable to load config file");
			ex.printStackTrace();
		}
	}

	@Test
	void testBasicHandshake() {

		DNSEmulatorSubdomainComms testEmulator = new DNSEmulatorSubdomainComms();
		try (InputStream input = new FileInputStream("config" + File.separator + "test.properties")) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			// Make properties encryption go away
			prop.setProperty(Constants.WIREENCRYPTTOGGLE, "false");
			IOManager io = new IOManager(Paths.get("test"), null);

			testEmulator.initialize(io, prop, null, null);

			ExecutorService service = Executors.newCachedThreadPool();
			service.submit(testEmulator);

			testEmulator.awaitStartup();

			DatagramSocket socket = new DatagramSocket();

			testInitialSessionExchangeMessage(prop, testEmulator, socket, 2, null, null);

			socket.close();
			testEmulator.stop();
		} catch (IOException ex) {
			System.out.println("Unable to load config file");
			ex.printStackTrace();
		}
	}

	// TODO: The indexing in this test isn't the best - clean it up
	@Test
	void testServerResponseAssembly() {
		DNSEmulatorSubdomainComms testEmulator = new DNSEmulatorSubdomainComms();
		try (InputStream input = new FileInputStream("config" + File.separator + "test.properties")) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			// Make properties encryption go away
			prop.setProperty(Constants.WIREENCRYPTTOGGLE, "false");
			IOManager io = new IOManager(Paths.get("test"), null);
			testEmulator.initialize(io, prop, null, null);
			byte[] dnsId = { (byte) 0x98, (byte) 0xfa };
			byte[] payload = testEmulator.buildResponsePayload("This is your next command", "RANDOMSTRING.domain.com",
					dnsId);
			// Check DNS ID
			assertEquals(dnsId[0], payload[0]);
			assertEquals(dnsId[1], payload[1]);
			// Check code
			assertEquals((byte) 0x81, payload[2]);
			assertEquals((byte) 0x80, payload[3]);
			// Queries
			assertEquals((byte) 0x00, payload[4]);
			assertEquals((byte) 0x01, payload[5]);
			// First label len
			assertEquals((byte) 0x0c, payload[6]);
			// Second label len
			assertEquals((byte) 0x06, payload[19]);
			// Third label Len
			assertEquals((byte) 0x03, payload[26]);
			// Trailing null byte
			assertEquals((byte) 0x00, payload[30]);
			// Response name
			assertEquals((byte) 0xc0, payload[31]);
			assertEquals((byte) 0x0c, payload[32]);
			// Type
			assertEquals((byte) 0x00, payload[33]);
			assertEquals((byte) 0x10, payload[34]);
			// Class (TXT)
			assertEquals((byte) 0x00, payload[35]);
			assertEquals((byte) 0x10, payload[36]);
			// Data Len
			assertEquals((byte) 0x00, payload[37]);
			assertEquals((byte) "This is your next command".length(), payload[38]);
			assertEquals((byte) "This is your next command".length(), payload[39]);
			// Test content
			int startPayloadIdx = 40;
			byte[] bytes = "This is your next command".getBytes();
			assertEquals(startPayloadIdx + "This is your next command".length(), payload.length);
			for (int idx = startPayloadIdx; idx < payload.length; idx++) {
				assertEquals(bytes[idx - startPayloadIdx], payload[idx]);
			}
		} catch (IOException ex) {
			System.out.println("Unable to load config file");
			ex.printStackTrace();
		}
	}

	@Test
	void testTrivialMessageResponse() {
		DNSEmulatorSubdomainComms testEmulator = new DNSEmulatorSubdomainComms();
		try (InputStream input = new FileInputStream("config" + File.separator + "test.properties")) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			// Make properties encryption go away
			prop.setProperty(Constants.WIREENCRYPTTOGGLE, "false");
			IOManager io = new IOManager(Paths.get("test"), null);

			testEmulator.initialize(io, prop, null, null);

			testTrivialMessage(prop, testEmulator, io, 2);
		} catch (IOException ex) {
			System.out.println("Unable to load config file");
			ex.printStackTrace();
		}
	}

	private static final String BOILERPLATE_HEADER = "HOST<spl>user<spl>1234<spl>UDP<spl>";
	private static final String DEFAULT_UID = "986<spl>";

	private List<byte[]> buildMessageTransmission(String message, String fakeDomain, byte[] dnsId, byte[] encryptionKey,
			short sessionKey, boolean includeHeader, String customUID) {
		String header = null;
		if (includeHeader) {
			if (customUID == null) {
				header = BOILERPLATE_HEADER + DEFAULT_UID + message;
			} else {
				header = BOILERPLATE_HEADER + customUID + "<spl>" + message;
			}
		} else {
			header = message;
		}
		if (encryptionKey != null) {
			AESEncryptor encryptor = new AESEncryptor(encryptionKey);
			header = encryptor.encrypt(header);
		}
		byte[] msgBytes = header.getBytes();
		List<byte[]> packets = new ArrayList<>();
		for (int idx = 0; idx < msgBytes.length; idx += 63) {
			int segmentLen = 63;
			if (idx + 63 > msgBytes.length) {
				segmentLen = msgBytes.length - idx;
			}
			List<Byte> newPacket = new ArrayList<>();
			newPacket.add(dnsId[0]);
			newPacket.add(dnsId[1]);
			for (int jdx = 2; jdx < 8; jdx++) {
				newPacket.add((byte) 0);
			}
			byte[] sessionCode = DNSEmulatorSubdomainComms.getBytesFromShort(sessionKey);
			newPacket.add(sessionCode[1]);// 8
			newPacket.add(sessionCode[0]);// 9
			newPacket.add((byte) 0);// idx 10
			if (idx + segmentLen == msgBytes.length) {
				newPacket.add((byte) 0x01);
			} else {
				newPacket.add((byte) 0);
			}
			newPacket.add((byte) segmentLen);
			for (int jdx = idx; jdx < idx + segmentLen; jdx++) {
				newPacket.add(msgBytes[jdx]);
			}
			String domainE[] = fakeDomain.split("\\.");
			for (String subDomain : domainE) {
				byte[] domainBytes = subDomain.getBytes();
				newPacket.add((byte) subDomain.length());
				for (int jdx = 0; jdx < domainBytes.length; jdx++) {
					newPacket.add(domainBytes[jdx]);
				}
			}
			newPacket.add((byte) 0);
			newPacket.add((byte) 0);
			newPacket.add((byte) 16);// TXT type
			newPacket.add((byte) 0);
			newPacket.add((byte) 1);

			byte[] packet = new byte[newPacket.size()];

			for (int jdx = 0; jdx < newPacket.size(); jdx++) {
				packet[jdx] = newPacket.get(jdx);
			}

			// System.out.println(idx + ":" + new String(packet));

			packets.add(packet);
		}

		return packets;
	}

	@Test
	void testFakeMessageBuilder() {
		byte[] dnsId = { (byte) 0x98, (byte) 0xfa };
		List<byte[]> packets = buildMessageTransmission(
				"This is my message, and there should be two packets. Need to make sure tht the message is great",
				".test.doma", dnsId, null, (short) 2, false, null);
		assertEquals(2, packets.size());
	}

	@Test
	void testTrivialMessageAssembly() {
		DNSEmulatorSubdomainComms testEmulator = new DNSEmulatorSubdomainComms();
		try (InputStream input = new FileInputStream("config" + File.separator + "test.properties")) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			// Make properties encryption go away
			prop.setProperty(Constants.WIREENCRYPTTOGGLE, "false");
			IOManager io = new IOManager(Paths.get("test"), null);
			testEmulator.initialize(io, prop, null, null);
			ExecutorService service = Executors.newCachedThreadPool();
			service.submit(testEmulator);

			testEmulator.awaitStartup();

			DatagramSocket socket = new DatagramSocket();
			testInitialSessionExchangeMessage(prop, testEmulator, socket, 2, null, null);

			String message = "This is my message. Need to validate transmission. Can I split and reassemble?";
			byte[] dnsId = { (byte) 0x98, (byte) 0xfa };
			List<byte[]> packets = buildMessageTransmission(message, "test.doma", dnsId, null, (short) 2, false, null);

			assertEquals(packets.size(), 2);
			String msg1 = testEmulator.extractB64Payload(packets.get(0), packets.get(0).length);
			assertEquals(63, msg1.length());
			assertEquals("This is my message. Need to validate transmission. Can I split ", msg1);
			String msg2 = testEmulator.extractB64Payload(packets.get(1), packets.get(1).length);
			assertEquals("and reassemble?", msg2);

			InetAddress address = InetAddress.getByName("127.0.0.1");
			int port = Integer.parseInt(prop.getProperty(Constants.DNSPORT));

			for (byte[] packet : packets) {
				DatagramPacket dPacket = new DatagramPacket(packet, packet.length, address, port);
				socket.send(dPacket);
			}

			Time.sleepWrapped(500);

			try {
				String processedMsg = io.pollIO(2);
				for (int idx = 0; idx < processedMsg.length(); idx++) {
					if (processedMsg.charAt(idx) != message.charAt(idx)) {
						// System.out.println("Orig: " + message.charAt(idx));
						// System.out.println("New: " + processedMsg.charAt(idx) + " " + new
						// Character(processedMsg.charAt(idx)).BYTES);
					}
					// System.out.println(processedMsg.charAt(idx) + ": " +
					// (processedMsg.charAt(idx) == message.charAt(idx)));
				}
				assertEquals(message, processedMsg);

			} catch (IllegalArgumentException ex) {
				fail(ex.getMessage());
			}
			socket.close();
			testEmulator.stop();
		} catch (IOException ex) {
			System.out.println("Unable to load config file");
			ex.printStackTrace();
		}
	}

}
