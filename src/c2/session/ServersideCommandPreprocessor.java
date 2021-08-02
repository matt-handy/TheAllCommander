package c2.session;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import c2.portforward.LocalPortListener;

public class ServersideCommandPreprocessor {

	private IOManager io;

	public ServersideCommandPreprocessor(IOManager io) {
		this.io = io;
	}

	public static final String PROXY_FORMAT_ERROR = "'proxy' requires format 'proxy <IP> <port> <forward port>'";
	public static final String KILLPROXY_FORMAT_ERROR = "'killproxy' requires format 'killproxy <IP> <port>'";

	private Map<String, LocalPortListener> listenerMap = new HashMap<>();
	private ExecutorService threadRunner = Executors.newCachedThreadPool();

	public synchronized CommandPreprocessorOutcome processCommand(String command, int sessionId) {
		if (command.startsWith("proxy")) {
			String args[] = command.split(" ");
			if (args.length != 4) {
				return new CommandPreprocessorOutcome(PROXY_FORMAT_ERROR, false);
			}
			try {
				Integer.parseInt(args[2]);//Just testing that this field is an int
				int localPort = Integer.parseInt(args[3]);
				LocalPortListener listener = new LocalPortListener(io, sessionId, args[1] + ":" + args[2], localPort);
				listenerMap.put(sessionId + ":" + args[1] + ":" + args[2], listener);
				threadRunner.submit(listener);
			} catch (NumberFormatException ex) {
				return new CommandPreprocessorOutcome(PROXY_FORMAT_ERROR, false);
			}
			
		}else if(command.startsWith("killproxy")) {
			String args[] = command.split(" ");
			if (args.length != 3) {
				return new CommandPreprocessorOutcome(KILLPROXY_FORMAT_ERROR, false);
			}
			String listenerUID = sessionId + ":" + args[1] + ":" + args[2];
			if(listenerMap.containsKey(listenerUID)) {
				listenerMap.get(listenerUID).kill();
				listenerMap.remove(listenerUID);
			}else {
				return new CommandPreprocessorOutcome("Unknown proxy requested for deletion", false);
			}
			
		}
		return new CommandPreprocessorOutcome(true);
	}
}
