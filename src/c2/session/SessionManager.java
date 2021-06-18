package c2.session;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SessionManager implements Runnable{

	private IOManager ioManager;
	private ExecutorService service = Executors.newCachedThreadPool();
	private int port;
	private SessionInitiator sessionInitiator;
	private Set<SessionHandler> sessions = new HashSet<>();
	private CommandMacroManager cmm;
	
	public SessionManager(IOManager ioManager, int port, CommandMacroManager cmm) {
		this.ioManager = ioManager;
		this.port = port;
		this.cmm = cmm;
	}
	
	public void addSession(SessionHandler sessionHandler){
		service.submit(sessionHandler);
		sessions.add(sessionHandler);
		//Let's have the HTTPSManager handle creating new sessions within the IOManager
	}
	
	public void stop() {
		System.out.println("Closing Session Initiator");
		sessionInitiator.stop();
		System.out.println("Closed Session Initiator");
		for(SessionHandler handler : sessions) {
			System.out.println("Closing Session Handler");
			handler.stop();
			System.out.println("Closed Session Handler");
		}
	}
	
	@Override
	public void run(){
		sessionInitiator = new SessionInitiator(this, ioManager, port, cmm);
		sessionInitiator.run();
		
	}
	
}
