package c2.session;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import c2.session.wizard.Wizard;

public class SessionManager implements Runnable{

	private IOManager ioManager;
	private ExecutorService service = Executors.newCachedThreadPool();
	private int port;
	private int securePort;
	private Properties properties;
	private SessionInitiator sessionInitiator;
	private SecureSessionInitiator secureSessionInitiator;
	private Set<SessionHandler> sessions = new HashSet<>();
	private CommandMacroManager cmm;
	private List<Wizard> wizards;
	
	public SessionManager(IOManager ioManager, int port, int securePort, CommandMacroManager cmm, Properties properties,
			List<Wizard> wizards) {
		this.ioManager = ioManager;
		this.port = port;
		this.cmm = cmm;
		this.securePort = securePort;
		this.properties = properties;
		this.wizards = wizards;
	}
	
	public void addSession(SessionHandler sessionHandler){
		service.submit(sessionHandler);
		sessions.add(sessionHandler);
		//Let's have the HTTPSManager handle creating new sessions within the IOManager
	}
	
	public void stop() {
		System.out.println("Closing Session Initiator(s)");
		sessionInitiator.stop();
		secureSessionInitiator.stop();
		System.out.println("Closed Session Initiator(s)");
		for(SessionHandler handler : sessions) {
			System.out.println("Closing Session Handler");
			handler.stop();
			System.out.println("Closed Session Handler");
		}
	}
	
	@Override
	public void run(){
		System.out.println("Securely listening for commander sessions on: " + securePort);
		secureSessionInitiator = new SecureSessionInitiator(this, ioManager, securePort, cmm, properties, wizards);
		service.submit(secureSessionInitiator);
		System.out.println("Listening for commander sessions on: " + port);
		sessionInitiator = new SessionInitiator(this, ioManager, port, cmm, properties, wizards);
		sessionInitiator.run();
	}
	
}
