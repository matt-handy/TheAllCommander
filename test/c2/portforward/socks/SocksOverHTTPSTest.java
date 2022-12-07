package c2.portforward.socks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import c2.KeyloggerProcessor;
import c2.http.HTTPSManager;
import c2.session.IOManager;
import c2.session.log.IOLogger;

class SocksOverHTTPSTest {

	@AfterEach
	void cleanup() {
		try {
			Files.deleteIfExists(Paths.get("test", "testuserHTTPS"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@BeforeEach
	void setupHTTPS() {
		TrustManager[] trustAllCerts = new TrustManager[] { (TrustManager) new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[0];
			}

			@Override
			public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
					throws CertificateException {
				// TODO Auto-generated method stub

			}

			@Override
			public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
					throws CertificateException {
				// TODO Auto-generated method stub

			}

		} };

		
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			e.printStackTrace();
		}
		

		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	}
	
	@Test
	void test() {
		Path testPath = null;
		if (System.getProperty("os.name").contains("Windows")) {
			testPath = Paths.get("config", "test.properties");
		}else {
			testPath = Paths.get("config", "test_linux.properties");
		}
		try (InputStream input = new FileInputStream(testPath.toFile())) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			// Make properties encryption go away
			IOManager io = new IOManager(new IOLogger(Paths.get("test", "log")), null);

			KeyloggerProcessor keyProcessor = new KeyloggerProcessor();
			keyProcessor.initialize("test");
			HTTPSManager httpsManager = new HTTPSManager();
			httpsManager.initialize(io, prop, keyProcessor, null);
			ExecutorService service = Executors.newCachedThreadPool();
			service.submit(httpsManager);
			httpsManager.awaitStartup();
			
			postData("test", "hi!", new HashMap<>());
			
			String encodedOutgoingTransmission = Base64.getEncoder().encodeToString(new String("0123456789").getBytes(StandardCharsets.UTF_8));
			String encodedIncomingTransmission = Base64.getEncoder().encodeToString(new String("ABCDEFGHIJ").getBytes(StandardCharsets.UTF_8));
			
			io.forwardTCPTraffic(2, "socksproxy:1", encodedOutgoingTransmission);
		
			Map<String, String> headers = new HashMap<>();
			headers.put("ProxyId", "socksproxy:1");
			String outgoingXmission = pollServer("socks5", headers);
			assertEquals(encodedOutgoingTransmission, outgoingXmission);
			
			postData("socks5", encodedIncomingTransmission, headers);
			
			String incomingXmission = io.receiveForwardedTCPTraffic(2, "socksproxy:1");
			assertEquals(encodedIncomingTransmission, incomingXmission);
			
			httpsManager.stop();
			
		} catch (IOException ex) {
			fail("Unable to load config file");
		}
	}
	
	private void postData(String resource, String data, Map<String, String> headers) {
		try {
			URL url = new URL("https://127.0.0.1:8000/"+resource);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestMethod("POST");

			addHeadersToConnection(connection, headers);

			connection.setUseCaches(false);
			connection.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			wr.writeBytes(data);
			wr.flush();
			wr.close();

			if (connection.getResponseCode() == 200) {
				byte[] cmdRaw = connection.getInputStream().readAllBytes();
				connection.disconnect();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String pollServer(String resource, Map<String, String> headers) {
		try {
			URL url = new URL("https://127.0.0.1:8000/" + resource);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			// Prepare a GET request Action
			connection.setRequestMethod("GET");

			addHeadersToConnection(connection, headers);

			connection.setUseCaches(false);
			connection.setDoOutput(true);

			if (connection.getResponseCode() == 200) {
				byte[] cmdRaw = connection.getInputStream().readAllBytes();
				String data = new String(cmdRaw);
				connection.disconnect();
				return data;
			} else {
				return null;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	private void addHeadersToConnection(HttpsURLConnection connection, Map<String, String> headers) {
		connection.setRequestProperty("User-Agent",
				"Mozilla/5.0 (X11; Linux x86_64; rv:68.0) Gecko/20100101 Firefox/68.0");
		connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml");
		connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
		connection.setRequestProperty("Connection", "close");
		connection.setRequestProperty("Upgrade-Insecure-Requests", "1");
		connection.setRequestProperty("Content-type", "text/plain");
		connection.setRequestProperty("Hostname", "test");
		connection.setRequestProperty("Username", "user");
		connection.setRequestProperty("PID", "1999");
		connection.setRequestProperty("UID", "myTestUID");

		if (headers != null) {
			for (String key : headers.keySet()) {
				connection.setRequestProperty(key, headers.get(key));
			}
		}
	}
}
