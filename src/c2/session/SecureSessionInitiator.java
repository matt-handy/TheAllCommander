package c2.session;

import java.io.FileInputStream;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import c2.Constants;
import c2.session.wizard.Wizard;

public class SecureSessionInitiator extends SessionInitiator {

	public SecureSessionInitiator(SessionManager sessionManager, IOManager ioManager, int port, CommandMacroManager cmm,
			Properties properties, List<Wizard> wizards) {
		super(sessionManager, ioManager, port, cmm, properties, wizards);
	}

	@Override
	protected ServerSocket getServerSocket() throws Exception {
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
		SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
		sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

		SSLServerSocketFactory sslFactory = sslContext.getServerSocketFactory();
		ServerSocket ss = sslFactory.createServerSocket(port);
		((SSLServerSocket)ss).setNeedClientAuth(false);
		return ss;
	}

}
