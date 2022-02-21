package c2.portforward.socks;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import c2.session.IOManager;
import util.Time;

public class SocksHandler implements Runnable {

	private final Socket socket;
	private final int sessionId;
	private final IOManager io;
	public final int proxyId;

	public static final int MAIN_SOCKET_TIMEOUT = 10000;
	public static final int PROXY_SOCKET_TIMEOUT = 10;

	public static final byte SOCKS5_VER = 0x05;
	public static final byte SOCKS4_VER = 0x04;

	public static final byte SOCKS_CONNECT = 0x01;
	public static final byte SOCKS_BIND = 0x02;
	public static final byte SOCKS_UDP = 0x03;

	public static final byte[] ACCEPT_PROXY = { (byte) 0x05, (byte) 0x00 };
	public static final byte[] REJECT_PROXY = { (byte) 0x05, (byte) 0xFF };

	private static final int[] ADDR_Size = { -1, // '00' No such AType
			4, // '01' IP v4 - 4Bytes
			-1, // '02' No such AType
			-1, // '03' First Byte is Len
			16 // '04' IP v6 - 16bytes
	};

	private AtomicBoolean stayAlive = new AtomicBoolean(true);
	private CountDownLatch deathLatch = new CountDownLatch(1);

	private byte addressType;
	private byte socksVersion = 0;
	private byte socksCommand;
	private final byte[] destinationAddr = new byte[255];// 255 is max address leng
	private final byte[] destinationPort = new byte[2];

	private InetAddress serverIp = null;
	private int serverPort = 0;
	private InetAddress clientIp = null;
	private int clientPort = 0;

	private String remoteDomain = null;// Sent to the end remote daemon

	private boolean localSocks;
	private Socket localSocksSocket;

	private InputStream is = null;
	private OutputStream os = null;

	private byte[] m_Buffer = new byte[4096];

	public SocksHandler(Socket socket, int sessionId, int proxyId, boolean localSocks, IOManager io) {
		this.socket = socket;
		this.sessionId = sessionId;
		this.proxyId = proxyId;
		this.localSocks = localSocks;
		this.io = io;
	}

	@Override
	public void run() {

		try {
			socket.setSoTimeout(PROXY_SOCKET_TIMEOUT);
			is = socket.getInputStream();
			os = socket.getOutputStream();

			// Get Version number
			int versionNumber = is.read();
			if (versionNumber != SOCKS5_VER) {
				socket.close();
				return;
			}

			// Authenticate

			// For now, we ignore authentication headers and info
			byte numberOfMethods = (byte) is.read();
			for (int idx = 0; idx < numberOfMethods; idx++) {
				is.read();
			}
			sendToClient(ACCEPT_PROXY, ACCEPT_PROXY.length);// For now, respond with a "yah, you're good"

			// Get command
			getClientCommand();// Will throw exception if client negotiation fails

			switch (socksCommand) {
			case SOCKS_CONNECT:
				connect();
				break;

			case SOCKS_BIND:
				bind();
				break;

			case SOCKS_UDP:
				// comm.udp();//Not implemented
				return;
			}

			while (stayAlive.get()) {
				relay();
			}
			close();
		} catch (Exception ex) {
			ex.printStackTrace();
			return;
		}

		deathLatch.countDown();
	}

	public static byte getSuccessCode() {
		return 00;
	}

	public static byte getFailCode() {
		return 04;
	}

	public void relay() {

		// ---> Check for client data <---

		int dlen = checkClientConnectionActiveAndFillBuffer();

		if (dlen < 0) {
			stayAlive.set(false);
		}
		if (dlen > 0) {
			// logData(dlen, "Cli data");
			sendToServer(m_Buffer, dlen);
		}

		// ---> Check for Server data <---
		dlen = checkServerConnectionAndFillBuffer();

		if (dlen < 0) {
			stayAlive.set(false);
		}
		if (dlen > 0) {
			// logData(dlen, "Srv data");

			sendToClient(m_Buffer, dlen);
		}

		Thread.yield();
	}

	public InetAddress resolveExternalLocalIP() throws IOException {
		InetAddress IP = null;

		final String[] hosts = { "www.wikipedia.org", "www.google.com", "www.microsoft.com", "www.amazon.com",
				"www.zombo.com", "www.ebay.com" };

		for (String host : hosts) {
			try (Socket sct = new Socket(InetAddress.getByName(host), 80)) {
				IP = sct.getLocalAddress();
				break;
			} catch (Exception e) {

			}
		}

		if (IP == null) {
			throw new IOException("Error in BIND() - BIND reip Failed on all common hosts to determine external IP's");
		}

		return IP;
	}

	public void bind() throws IOException {
		int MyPort = 0;

		// Resolve External IP
		InetAddress MyIP = resolveExternalLocalIP();

		ServerSocket ssock = new ServerSocket(0);
		try {
			ssock.setSoTimeout(PROXY_SOCKET_TIMEOUT);
			MyPort = ssock.getLocalPort();
		} catch (IOException e) { // MyIP == null
			bindReply((byte) 92, MyIP, MyPort);
			ssock.close();
			return;
		}

		bindReply((byte) 90, MyIP, MyPort);

		Socket socket = null;

		while (socket == null) {
			if (checkClientConnectionActiveAndFillBuffer() >= 0) {
				ssock.close();
				return;
			}

			try {
				socket = ssock.accept();
				socket.setSoTimeout(PROXY_SOCKET_TIMEOUT);
			} catch (InterruptedIOException e) {
				// ignore
			}
			Thread.yield();
		}

		serverIp = socket.getInetAddress();
		serverPort = socket.getPort();

		bindReply((byte) 90, socket.getInetAddress(), socket.getPort());

		localSocksSocket = socket;

		ssock.close();
	}

	public int checkServerConnectionAndFillBuffer() {
		if (localSocks) {
			// The client side is not opened.
			if (localSocksSocket == null)
				return -1;

			int dlen;

			try {
				dlen = localSocksSocket.getInputStream().read(m_Buffer, 0, m_Buffer.length);
			} catch (InterruptedIOException e) {
				dlen = 0;
			} catch (IOException e) {
				dlen = -1;
			}

			if (dlen < 0) {
				close();
			}

			return dlen;
		} else {
			String b64Payload = io.receiveForwardedTCPTraffic(sessionId, "socksproxy:" + proxyId);
			if (b64Payload != null) {
				if (b64Payload.equals(Base64.getEncoder().encodeToString("socksterminatedatdaemon".getBytes()))) {
					close();
					return -1;
				} else {
					byte[] payload = Base64.getDecoder().decode(b64Payload.getBytes(StandardCharsets.UTF_8));
					int dlen = payload.length;
					for (int idx = 0; idx < dlen; idx++) {
						m_Buffer[idx] = payload[idx];
					}
					return dlen;
				}

			} else {
				return 0;
			}

		}
	}

	public int checkClientConnectionActiveAndFillBuffer() {

		// The client side is not opened.
		if (is == null)
			return -1;

		int dlen;

		try {
			dlen = is.read(m_Buffer, 0, m_Buffer.length);
		} catch (InterruptedIOException e) {
			dlen = 0;
		} catch (IOException e) {
			dlen = -1;
		}

		if (dlen < 0) {
			close();
		}

		return dlen;

	}

	private void close() {
		// Close connection with Client
		try {
			is.close();
			os.close();
			socket.close();
		} catch (IOException ex) {

		}

		if (localSocks) {
			try {
				localSocksSocket.close();
			} catch (IOException ex) {

			}
		} else {
			String command = "killSocks proxyID:" + proxyId; // TODO Build with constants
			io.sendCommand(sessionId, command);
		}
	}

	public void bindReply(byte ReplyCode, InetAddress IA, int PT) {
		final byte[] REPLY = new byte[8];
		final byte[] IP = IA.getAddress();

		REPLY[0] = 0;
		REPLY[1] = ReplyCode;
		REPLY[2] = (byte) ((PT & 0xFF00) >> 8);
		REPLY[3] = (byte) (PT & 0x00FF);
		REPLY[4] = IP[0];
		REPLY[5] = IP[1];
		REPLY[6] = IP[2];
		REPLY[7] = IP[3];

		if (socket != null && localSocksSocket != null) {
			sendToClient(REPLY, REPLY.length);
		}/* else {
			System.out.println("Closed BIND Client Connection");
		}*/
	}

	public void sendToServer(byte[] buffer, int len) {
		if (localSocks) {
			if (localSocksSocket != null && len > 0 && len <= buffer.length) {
				try {
					localSocksSocket.getOutputStream().write(buffer, 0, len);
					localSocksSocket.getOutputStream().flush();
				} catch (IOException e) {
					//System.out.println("Sending data to server");
				}
			}
		} else {
			io.forwardTCPTraffic(sessionId, "socksproxy:" + proxyId,
					Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));
		}
	}

	public void connect() throws Exception {
		if (localSocks) {
			// Connect to the Remote Host
			try {
				localSocksSocket = new Socket(serverIp.getHostAddress(), serverPort);
				localSocksSocket.setSoTimeout(PROXY_SOCKET_TIMEOUT);
			} catch (IOException e) {
				replyCommand(getFailCode()); // Connection Refused
				throw new Exception("Socks 4 - Can't connect to " + serverIp.toString() + ":" + serverPort);
			}
		} else {
			String command = "startSocks proxyID:" + proxyId; // TODO Build with constants
			if (remoteDomain != null) {
				command = command + " " + remoteDomain + ":" + serverPort;
			} else {
				command = command + " " + serverIp.toString() + ":" + serverPort;
			}
			io.sendCommand(sessionId, command);
			String response = io.pollIO(sessionId);
			int counter = 0;
			while(response == null) {
				response = io.pollIO(sessionId);
				Time.sleepWrapped(10);
				counter += 10;
				if(counter >= MAIN_SOCKET_TIMEOUT) {
					break;
				}
			}
			if (!response.equals("socksEstablished" + System.lineSeparator())) {// TODO replace with constant
				replyCommand(getFailCode()); // Connection Refused
				throw new Exception("Socks 4 - Can't connect to " + command);
			}
		}

		replyCommand(getSuccessCode());
	}

	public void sendToClient(byte[] buffer, int len) {
		if (os != null && len > 0 && len <= buffer.length) {
			try {
				os.write(buffer, 0, len);
				os.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public String replyName(byte code) {
		switch (code) {
		case 0:
			return "SUCCESS";
		case 1:
			return "General SOCKS Server failure";
		case 2:
			return "Connection not allowed by ruleset";
		case 3:
			return "Network Unreachable";
		case 4:
			return "HOST Unreachable";
		case 5:
			return "Connection Refused";
		case 6:
			return "TTL Expired";
		case 7:
			return "Command not supported";
		case 8:
			return "Address Type not Supported";
		case 9:
			return "to 0xFF UnAssigned";
		case 90:
			return "Request GRANTED";
		case 91:
			return "Request REJECTED or FAILED";
		case 92:
			return "Request REJECTED - SOCKS server can't connect to Identd on the client";
		case 93:
			return "Request REJECTED - Client and Identd report diff user-ID";
		default:
			return "Unknown Command";
		}
	}

	public void replyCommand(byte replyCode) {
		final int pt;

		byte[] REPLY = new byte[10];
		byte[] IP = new byte[4];

		if (socket != null) {
			pt = socket.getLocalPort();
		} else {
			IP[0] = 0;
			IP[1] = 0;
			IP[2] = 0;
			IP[3] = 0;
			pt = 0;
		}

		formGenericReply(replyCode, pt, REPLY, IP);

		sendToClient(REPLY, REPLY.length);
	}

	private void formGenericReply(byte replyCode, int pt, byte[] REPLY, byte[] IP) {
		REPLY[0] = SOCKS5_VER;
		REPLY[1] = replyCode;
		REPLY[2] = 0x00; // Reserved '00'
		REPLY[3] = 0x01; // DOMAIN NAME Address Type IP v4
		REPLY[4] = IP[0];
		REPLY[5] = IP[1];
		REPLY[6] = IP[2];
		REPLY[7] = IP[3];
		REPLY[8] = (byte) ((pt & 0xFF00) >> 8);// Port High
		REPLY[9] = (byte) (pt & 0x00FF); // Port Low
	}

	public void getClientCommand() throws IOException {
		socksVersion = (byte) is.read();
		socksCommand = (byte) is.read();
		/* byte RSV = */ is.read(); // Reserved. Must be'00'
		addressType = (byte) is.read();

		int Addr_Len = ADDR_Size[addressType];
		destinationAddr[0] = (byte) is.read();
		if (addressType == 0x03) {
			Addr_Len = destinationAddr[0] + 1;
		}

		for (int i = 1; i < Addr_Len; i++) {
			destinationAddr[i] = (byte) is.read();
		}
		destinationPort[0] = (byte) is.read();
		destinationPort[1] = (byte) is.read();

		if (socksVersion != SOCKS5_VER) {
			replyCommand((byte) 0xFF);
			throw new IOException("Incorrect SOCKS Version of Command: " + socksVersion);
		}

		if ((socksCommand < SOCKS_CONNECT) || (socksCommand > SOCKS_UDP)) {
			replyCommand((byte) 0x07);
			throw new IOException("SOCKS 5 - Unsupported Command: \"" + socksCommand + "\"");
		}

		if (addressType == 0x04) {
			replyCommand((byte) 0x08);
			throw new IOException("Unsupported Address Type - IP v6");
		}

		if ((addressType >= 0x04) || (addressType <= 0)) {
			replyCommand((byte) 0x08);
			throw new IOException("SOCKS 5 - Unsupported Address Type: " + addressType);
		}

		// if (localSocks) {
		if (calculateAndValidateIpAndPort()) { // Gets the IP Address
			replyCommand((byte) 0x04); // Host Not Exists...
			throw new IOException("SOCKS 5 - Unknown Host/IP address '" + serverIp.toString() + "'");
		}
		// }
	}

	public static InetAddress calcInetAddress(byte[] addr) {
		InetAddress IA;
		StringBuilder sIA = new StringBuilder();

		if (addr.length < 4) {
			//System.out.println("Invalid length of IP v4 - " + addr.length + " bytes");
			return null;
		}

		// IP v4 Address Type
		for (int i = 0; i < 4; i++) {
			sIA.append(byte2int(addr[i]));
			if (i < 3)
				sIA.append(".");
		}

		try {
			IA = InetAddress.getByName(sIA.toString());
		} catch (UnknownHostException e) {
			return null;
		}

		return IA;
	}

	public static int calcPort(byte Hi, byte Lo) {
		return ((byte2int(Hi) << 8) | byte2int(Lo));
	}

	public static int byte2int(byte b) {
		return (int) b < 0 ? 0x100 + (int) b : b;
	}

	public InetAddress calcInetAddress(byte type, byte[] addr) {
		InetAddress IA = null;

		switch (type) {
		// Version IP 4
		case 0x01:
			IA = calcInetAddress(addr);
			break;
		// Version IP DOMAIN NAME
		case 0x03:

			if (addr[0] <= 0) {
				//System.out.println("Bad IP in command - size : " + addr[0]);
				return null;
			}
			StringBuilder sIA = new StringBuilder();
			for (int i = 1; i <= addr[0]; i++) {
				sIA.append((char) addr[i]);
			}
			if (localSocks) {
				try {
					IA = InetAddress.getByName(sIA.toString());
				} catch (UnknownHostException e) {
					return null;
				}
			} else {
				remoteDomain = sIA.toString();
			}

			break;

		default:
			return null;
		}
		return IA;
	}

	private boolean calculateAndValidateIpAndPort() {
		serverIp = calcInetAddress(addressType, destinationAddr);
		serverPort = calcPort(destinationPort[0], destinationPort[1]);

		clientIp = socket.getInetAddress();
		clientPort = socket.getPort();

		if (localSocks) {
			return !((serverIp != null) && (serverPort >= 0));
		} else {
			if (remoteDomain != null) {
				return !(serverPort >= 0);
			} else {
				return !((serverIp != null) && (serverPort >= 0));
			}
		}
	}

	public String commName(byte code) {
		switch (code) {
		case 0x01:
			return "CONNECT";
		case 0x02:
			return "BIND";
		case 0x03:
			return "UDP Association";
		default:
			return "Unknown Command";
		}
	}

	public void stop() {
		stayAlive.set(false);
		try {
			deathLatch.await();
		} catch (InterruptedException e) {
			// Discard, move forward
		}
	}

	public boolean isAlive() {
		return stayAlive.get();
	}

}
