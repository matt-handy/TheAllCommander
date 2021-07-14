package c2.udp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Base64;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import c2.Constants;
import c2.crypto.AESEncryptor;
import c2.crypto.Encryptor;
import c2.crypto.NullEncryptor;
import util.Time;
import util.test.ClientServerTest;

public class UDPServerTest extends ClientServerTest {

	
	@Test
	void testLocal() {
		test();
	}
	
	public static void test() {
		Encryptor encryptor = new NullEncryptor();
		String propertiesFile = "test_with_cmds.properties";
		try (InputStream input = new FileInputStream("test" + File.separator + propertiesFile)) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			if(Boolean.parseBoolean(prop.getProperty(Constants.WIREENCRYPTTOGGLE))) {
				System.out.println("Initializing with AES");
				byte[] key = Base64.getDecoder().decode(prop.getProperty(Constants.WIREENCRYPTKEY));
				encryptor = new AESEncryptor(key);
			}

		} catch (IOException ex) {
			System.out.println("Unable to load config file");
			fail(ex.getMessage());
		}
		
		initiateServer(propertiesFile);
		
		Time.sleepWrapped(1000);
		
		try {
			DatagramSocket socket = new DatagramSocket();
			InetAddress address = InetAddress.getByName("localhost");
			//Try a deliberately bad packet to break the decryptor, then send a valid one. 
			//Make sure that the UDP acceptor on the server side will ignore bad input.
			
			//Intentionally bad header to cause encryptor to start working on incomplete fragment
			String header = "XXXXXXXXX";
			String encrypted = header + encryptor.encrypt("1234567890ABHostname<spl>Username<spl>PID<spl>NoOp<spl><poll>");
			
			byte buf[] = encrypted.getBytes();
			DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 8001);
			socket.send(packet);
			
			//First 12 bytes are fake DNS header
			header = "XXXXXXXXXXXX";
			encrypted = header + encryptor.encrypt("1234567890ABHostname<spl>Username<spl>PID<spl>NoOp<spl><poll>");
			
			buf = encrypted.getBytes();
			packet = new DatagramPacket(buf, buf.length, address, 8001);
			socket.send(packet);
			packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			String received = new String(packet.getData(), 0, packet.getLength());
			//Discard the 12 byte header
			received = received.substring(12);
			received = encryptor.decrypt(received);
			assertEquals(received, "pwd");
			socket.close();
		} catch (Exception ex) {
			fail(ex.getMessage());
		}
	
		//Something is jammed here and the server is not tearing down, but only on this test.
		teardown();
		
	}

}
