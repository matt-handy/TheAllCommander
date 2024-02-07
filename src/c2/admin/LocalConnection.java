package c2.admin;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import c2.Constants;
import c2.session.CommandWizard;
import util.Time;

public class LocalConnection {
	
	public static String CMD_QUIT_LOCAL = "quitLocal";
	
	public static Path CSHARP_TMP_FILE = Paths.get("test_tmp.exe");
	public static Path JAVA_TMP_FILE = Paths.get("DaemonLoader.jar");

	public static void main(String args[]) throws NumberFormatException, UnknownHostException, IOException, Exception {
		try (InputStream input = new FileInputStream(args[2])) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			LocalConnection lc = new LocalConnection();
			lc.engage(args, new BufferedReader(new InputStreamReader(System.in)), System.out, prop);

		} catch (IOException ex) {
			System.out.println("Initialization failed: Unable to load TheAllCommander config file");
		}
		
	}
	
	public static Socket getSocket(String targetIp, int port, Properties properties) throws Exception {
		TrustManager[] trustAllCerts = new TrustManager[] { (TrustManager) new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
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
		
		char[] password = properties.getProperty(Constants.HTTPS_KEYSTORE_PASSWORD).toCharArray();
		KeyStore ks = KeyStore.getInstance("JKS");
		FileInputStream fis = new FileInputStream(properties.getProperty(Constants.HTTPS_KEYSTORE_PATH));
		ks.load(fis, password);
		// setup the key manager factory
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(ks, password);
		SSLContext sc = SSLContext.getInstance("TLSv1.3");
		sc.init(kmf.getKeyManagers(), trustAllCerts, null);
		SSLSocketFactory factory = sc.getSocketFactory();
        SSLSocket socket = (SSLSocket)factory.createSocket(targetIp, port);
        socket.startHandshake();
        return socket;
	}

	public void engage(String args[], BufferedReader stdIn, PrintStream terminalOut, Properties properties)
			throws NumberFormatException, UnknownHostException, IOException, Exception {
		Socket remote = getSocket(args[0], Integer.parseInt(args[1]), properties);
		//Socket remote = new Socket(args[0], Integer.parseInt(args[1]));

        PrintWriter out = new PrintWriter(
                new BufferedWriter(
                new OutputStreamWriter(
                		remote.getOutputStream())));
		BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

		boolean stayAlive = true;

		// BufferedReader cReader = new BufferedReader(console.reader());

		Runnable printer = new Runnable() {
			public void run() {
				while (true) {
					try {
						String output = br.readLine();
						if (output != null) {
							if (output.startsWith("<control> uplinked ")) {
								String[] elements = output.split(" ");
								byte[] data = Base64.getDecoder().decode(elements[3]);
								try (OutputStream stream = new FileOutputStream(elements[2])) {
									stream.write(data);
								}
								terminalOut.println("Received File");
							}else if(output.startsWith("<control> " + CommandWizard.CMD_GENERATE_CSHARP)) {
								String[] elements = output.split(" ");
								byte[] data = Base64.getDecoder().decode(elements[2]);
								try (OutputStream stream = new FileOutputStream(CSHARP_TMP_FILE.toFile())) {
									stream.write(data);
								}
								terminalOut.println("Exe downloaded");
							}else if(output.startsWith("<control> " + CommandWizard.CMD_GENERATE_JAVA)) {
								String[] elements = output.split(" ");
								byte[] data = Base64.getDecoder().decode(elements[2]);
								try (OutputStream stream = new FileOutputStream(JAVA_TMP_FILE.toFile())) {
									stream.write(data);
								}
								terminalOut.println("Jar downloaded");
							} else {
								terminalOut.println(output);
							}
						}
					} catch (Exception e) {
						// Keep going
					}
				}
			}
		};

		ExecutorService es = Executors.newCachedThreadPool();
		es.submit(printer);

		while (stayAlive) {

			String nextCmd = stdIn.readLine();
			if (nextCmd.equals(CMD_QUIT_LOCAL)) {
				stayAlive = false;
			} else if (nextCmd.startsWith("sendFile")) {
				// TODO: need to handle bad command format here
				String filename = nextCmd.split(" ")[1];
				// TODO: handle exception from bad name
				byte[] fileBytes = Files.readAllBytes(Paths.get(filename));
				byte[] encoded = Base64.getEncoder().encode(fileBytes);
				String encodedString = new String(encoded, StandardCharsets.US_ASCII);
				out.println("<control> download " + Paths.get(filename).getFileName().toString().replaceAll(" ", "_") + " "
						+ encodedString);
				out.flush();
			}else if(nextCmd.equals("sleep")) {
				Time.sleepWrapped(10000);
			} else {
				out.println(nextCmd);
				out.flush();
			}
		}

		remote.close();
	}
}
