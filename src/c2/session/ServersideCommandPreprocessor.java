package c2.session;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import c2.portforward.LocalPortListener;
import c2.portforward.socks.LocalSocksListener;

public class ServersideCommandPreprocessor {

	private IOManager io;

	public ServersideCommandPreprocessor(IOManager io) {
		this.io = io;
	}

	public static final String PROXY_FORMAT_ERROR = "'proxy' requires format 'proxy <IP> <port> <forward port>'";
	public static final String KILLPROXY_FORMAT_ERROR = "'killproxy' requires format 'killproxy <IP> <port>'";

	public static final String SOCKS5_FORMAT_ERROR = "'startSocks5' requires format 'startSocks5 <local port>'";
	public static final String EXISTING_SOCKS5_ERROR = "There can only be one SOCKS5 proxy running";
	public static final String NO_EXISTING_SOCKS5_ERROR = "No existing SOCKS5 proxy running";

	private Map<String, LocalPortListener> listenerMap = new HashMap<>();
	private Map<String, LocalSocksListener> socksMap = new HashMap<>();
	private ExecutorService threadRunner = Executors.newCachedThreadPool();

	public synchronized boolean haveActiveForward(int sessionId, String forwardIdentifier) {
		String listenerUID = sessionId + ":" + forwardIdentifier;
		return listenerMap.containsKey(listenerUID);
	}

	public synchronized CommandPreprocessorOutcome processCommand(String command, int sessionId) {
		if (command.startsWith("proxy")) {
			String args[] = command.split(" ");
			if (args.length != 4) {
				return new CommandPreprocessorOutcome(PROXY_FORMAT_ERROR, false);
			}
			try {
				Integer.parseInt(args[2]);// Just testing that this field is an int
				int localPort = Integer.parseInt(args[3]);
				LocalPortListener listener = new LocalPortListener(io, sessionId, args[1] + ":" + args[2], localPort);
				listenerMap.put(sessionId + ":" + args[1] + ":" + args[2], listener);
				threadRunner.submit(listener);
			} catch (NumberFormatException ex) {
				return new CommandPreprocessorOutcome(PROXY_FORMAT_ERROR, false);
			}

		} else if (command.startsWith("killproxy")) {
			String args[] = command.split(" ");
			if (args.length != 3) {
				return new CommandPreprocessorOutcome(KILLPROXY_FORMAT_ERROR, false);
			}
			String listenerUID = sessionId + ":" + args[1] + ":" + args[2];
			if (listenerMap.containsKey(listenerUID)) {
				listenerMap.get(listenerUID).kill();
				listenerMap.remove(listenerUID);
			} else {
				return new CommandPreprocessorOutcome("Unknown proxy requested for deletion", false);
			}

		} else if (command.startsWith("startSocks5")) {
			String args[] = command.split(" ");
			if (args.length != 2) {
				return new CommandPreprocessorOutcome(SOCKS5_FORMAT_ERROR, false, false);
			}
			if(socksMap.containsKey(sessionId + "")) {
				return new CommandPreprocessorOutcome(EXISTING_SOCKS5_ERROR, false, false);
			}
			try {
				int localport = Integer.parseInt(args[1]);
				LocalSocksListener localSocks = new LocalSocksListener(io, localport, sessionId, false);
				threadRunner.submit(localSocks);
				localSocks.awaitStartup();
				socksMap.put(sessionId + "", localSocks);
			} catch (NumberFormatException ex) {
				return new CommandPreprocessorOutcome(SOCKS5_FORMAT_ERROR, false, false);
			}
			return new CommandPreprocessorOutcome("Success", true, false);
		}else if(command.equalsIgnoreCase("killSocks5")) {
			if(socksMap.containsKey(sessionId + "")) {
				LocalSocksListener localSocks = socksMap.remove(sessionId + "");
				localSocks.kill();
			}else {
				return new CommandPreprocessorOutcome(NO_EXISTING_SOCKS5_ERROR, false, false);
			}
		}
		return new CommandPreprocessorOutcome(true);
	}
}
