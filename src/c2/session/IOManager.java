package c2.session;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import c2.Constants;
import c2.session.log.IOLogger;
import util.Time;

public class IOManager {

	private Map<Integer, Session> sessions = new HashMap<>();
	private int nextSessionId = 2;

	private CommandLoader cl;

	private ServersideCommandPreprocessor preprocessor;

	private IOLogger logger;

	public IOManager(IOLogger logger, CommandLoader cl) {
		sessions.put(1, new Session(1, "default", "default", "default"));
		this.cl = cl;
		this.preprocessor = new ServersideCommandPreprocessor(this);
		this.logger = logger;
	}

	public ServersideCommandPreprocessor getCommandPreprocessor() {
		return preprocessor;
	}

	/**
	 * C2Interface implementations use this method to indicate that a new daemon is
	 * facilitating this commanding pathway.
	 *
	 * @param sessionId The Id of the session to check for a command
	 * @param daemonUID The new daemon's UID which will fulfill this command pathway
	 *                  roll
	 */
	public synchronized void updateDaemonUID(int sessionId, String daemonUID) {
		if (sessions.containsKey(sessionId)) {
			sessions.get(sessionId).updateDaemonUID(daemonUID);
		} else {
			throw new IllegalArgumentException("Invalid session id");
		}
	}

	/**
	 * C2Interface implementations use this method to query the next command to be
	 * sent to connected daemons. The C2Interface is responsible for determining
	 * what the applicable sessionId is prior to invocation
	 *
	 * @param sessionId The Id of the session to check for a command
	 * @return with a String containing the next command, or null if no commands are
	 *         available
	 */
	public synchronized String pollCommand(int sessionId) {
		if (sessions.containsKey(sessionId)) {
			String command = sessions.get(sessionId).pollCommand();
			return command;
		} else {
			throw new IllegalArgumentException("Invalid session id");
		}
	}

	/**
	 * C2Interface implementations use this method to query the current daemon UID
	 * associated with this session
	 *
	 * @param sessionId The Id of the session
	 * @return with a String containing the UID of the current daemon
	 */
	public synchronized String getDaemonUID(int sessionId) {
		if (sessions.containsKey(sessionId)) {
			return sessions.get(sessionId).getDaemonUID();
		} else {
			throw new IllegalArgumentException("Invalid session id");
		}
	}

	public int determineAndGetCorrectSessionId(String hostname, String username, String protocol, String daemonUID) {
		String sessionUID = hostname + ":" + username + ":" + protocol;
		Integer sessionId = getSessionId(sessionUID);
		if (sessionId == null) {
			sessionId = addSession(username, hostname, protocol);
			if (daemonUID != null) {
				updateDaemonUID(sessionId, daemonUID);
			}
		}
		if (daemonUID != null) {
			if (!daemonUID.equals(getDaemonUID(sessionId))) {
				if (isContactLate(sessionId)) {
					updateDaemonUID(sessionId, daemonUID);
				} else {
					Integer candidateSessionId = getSessionId(sessionUID + ":" + daemonUID);
					if (candidateSessionId == null) {
						sessionId = addSession(username, hostname, protocol, daemonUID);
					} else {
						sessionId = candidateSessionId;
					}
				}
			}
		}
		return sessionId;
	}

	/**
	 * C2Interface implementations use this method to check if a session has not
	 * received a contact attempt within the expected interval.
	 *
	 * @param sessionId The Id of the session
	 * @return whether other not contact is late
	 */
	private synchronized boolean isContactLate(int sessionId) {
		if (sessions.containsKey(sessionId)) {
			return sessions.get(sessionId).isContactLate();
		} else {
			throw new IllegalArgumentException("Invalid session id");
		}
	}

	/**
	 * C2Interface implementations use this method notify that a session has had a
	 * successful contact attempt
	 *
	 * @param sessionId The Id of the session
	 */
	public void updateSessionContactTime(int sessionId) {
		if (sessions.containsKey(sessionId)) {
			sessions.get(sessionId).updateSessionContactTime();
		} else {
			throw new IllegalArgumentException("Invalid session id");
		}
	}

	/**
	 * TheAllCommander and implementations of AbstractCommandMacro use this method
	 * to place a command in the queue for transmission to a client
	 *
	 * @param sessionId The Id of the session
	 * @param command   The command to be sent
	 */
	public synchronized void sendCommand(int sessionId, String command) {
		if (sessions.containsKey(sessionId)) {
			CommandPreprocessorOutcome outcome = preprocessor.processCommand(command, sessionId);
			//Sometimes the command is successful, but we still don't need to send to Daemon
			if (!outcome.outcome || !outcome.sendingCmdToClient) {
				sendIO(sessionId, outcome.message + System.lineSeparator());
				return;
			}

			Session session = sessions.get(sessionId);
			session.sendCommand(command);
			try {
				logger.writeSendCommand(command, session);
			} catch (Exception e) {
				System.out.println("Can't write to log file");
				e.printStackTrace();
			}
		} else {
			throw new IllegalArgumentException("Invalid session id");
		}
	}

	/**
	 * TheAllCommander and AbstractCommandMacro implementations use these method to
	 * query for responses received from a client. The general concept is that
	 * sendCommand is invoked, and then pollIO is checked to see if a response has
	 * been received. The call is non-blocking, and the AbstractCommandMacro is free
	 * to wait as long or little as necessary before inferring silence as an
	 * outcome.
	 *
	 * @param sessionId The Id of the session to check for a response
	 * @return with a String containing the next IO, or null if no commands are
	 *         available
	 */
	public synchronized String pollIO(int sessionId) {
		if (sessions.containsKey(sessionId)) {
			return sessions.get(sessionId).pollIO();
		} else {
			throw new IllegalArgumentException("Invalid session id");
		}
	}

	/**
	 * C2Interface implementations use this method to send a response back to the
	 * commanding session. The C2Interface is responsible for determining what the
	 * applicable sessionId is prior to invocation
	 *
	 * @param sessionId The Id of the session to check for a command
	 * @param response  the response message to be sent
	 */
	public synchronized void sendIO(int sessionId, String response) {
		if (sessions.containsKey(sessionId)) {
			Session session = sessions.get(sessionId);
			session.sendIO(response);
			try {
				logger.writeReceivedIO(response, session);
			} catch (Exception e) {
				System.out.println("Can't write to log file");
				e.printStackTrace();
			}
		} else {
			throw new IllegalArgumentException("Invalid session id");
		}
	}

	public synchronized Set<Session> getSessions() {
		return new HashSet<Session>(sessions.values());
	}

	private void checkForExistingSession(String sessionUid) {
		for (Session session : sessions.values()) {
			if (session.uid.contentEquals(sessionUid)) {
				throw new IllegalArgumentException("Session Id Already Exists");
			}
		}
	}

	private void sendPresetCommands(int sessionId, String username, String hostname) {
		if (cl != null) {
			if (!cl.getDefaultCommands().isEmpty()) {
				for (String cmd : cl.getDefaultCommands()) {
					System.out.println("Add default cmd: " + cmd);
					sendCommand(sessionId, cmd);
				}
			}

			List<String> userCmds = cl.getUserCommands(username);
			if (userCmds != null) {
				for (String cmd : userCmds) {
					System.out.println("Adding user cmd: " + cmd);
					sendCommand(sessionId, cmd);
				}
			}

			List<String> hostCmds = cl.getHostCommands(hostname);
			if (hostCmds != null) {
				for (String cmd : hostCmds) {
					System.out.println("Adding host cmd: " + cmd);
					sendCommand(sessionId, cmd);
				}
			}
		}
	}

	/**
	 * C2Interface implementations use this method to add a new session and retrieve
	 * a session ID that can be used for further communication with the class. This
	 * method should only be used if there is not a prior session established with
	 * the same username, protocol, and hostname.
	 *
	 * @param username  The username of the connecting session
	 * @param hostname  The hostname of the connecting session
	 * @param protocol  The protocol of the connecting session
	 * @param daemonUID The UID presented by the daemon
	 * @return an int representing the registered session ID.
	 */
	public synchronized int addSession(String username, String hostname, String protocol, String daemonUID) {
		checkForExistingSession(hostname + ":" + username + ":" + protocol + ":" + daemonUID);
		int newSessionId = nextSessionId++;
		sessions.put(newSessionId, new Session(newSessionId, hostname, username, protocol, daemonUID));

		sendPresetCommands(newSessionId, username, hostname);

		return newSessionId;
	}

	/**
	 * C2Interface implementations use this method to add a new session and retrieve
	 * a session ID that can be used for further communication with the class.
	 *
	 * @param username The username of the connecting session
	 * @param hostname The hostname of the connecting session
	 * @param protocol The protocol of the connecting session
	 * @return an int representing the registered session ID.
	 */
	public synchronized int addSession(String username, String hostname, String protocol) {
		checkForExistingSession(hostname + ":" + username + ":" + protocol);
		int newSessionId = nextSessionId++;
		sessions.put(newSessionId, new Session(newSessionId, hostname, username, protocol));

		sendPresetCommands(newSessionId, username, hostname);

		return newSessionId;
	}

	public Integer getSessionId(String uid) {
		for (Session session : sessions.values()) {
			if (session.uid.contentEquals(uid)) {
				return session.id;
			}
		}
		return null;
	}

	public Session getSession(int id) {
		return sessions.get(id);
	}

	/**
	 * C2Interface implementations can use this method to de-register a session ID
	 * if a client will not be connecting again under the same session
	 *
	 * @param sessionId The sessionID to remove
	 */
	public synchronized void removeSession(int sessionId) {
		sessions.remove(sessionId);
	}

	/**
	 * Command macro implementations can use this method to return all IO that has
	 * been sent from the connected client in a single String
	 *
	 * @param sessionId The sessionID to query
	 * @return The assembled sum of all return IO.
	 */
	public String readAllMultilineCommands(int sessionId) {
		String nextIo = pollIO(sessionId);
		StringBuilder sb = new StringBuilder();
		while (nextIo != null) {
			sb.append(nextIo);
			sb.append(System.lineSeparator());
			nextIo = pollIO(sessionId);
		}
		String cleanedIO = sb.toString();
		//Need to clean IO if received from Windows on a Linux system
		if (!System.getProperty("os.name").contains("Windows")) {
			cleanedIO = cleanedIO.replaceAll("\r\n", System.lineSeparator());
		}
		return cleanedIO;
	}

	/**
	 * Command macro implementations can use this method wait up to the configurable
	 * Max Response Wait interval for input from the client
	 *
	 * @param sessionId The sessionID to query
	 * @return The assembled sum of all return IO.
	 */
	public String awaitMultilineCommands(int sessionId) {
		String nextIo = readAllMultilineCommands(sessionId);
		int counter = 0;
		while (nextIo.length() == 0 && counter < Constants.getConstants().getMaxResponseWait()) {
			nextIo = readAllMultilineCommands(sessionId);
			counter += Constants.getConstants().getRepollForResponseInterval();
			Time.sleepWrapped(Constants.getConstants().getRepollForResponseInterval());
		}
		return nextIo;
	}

	/**
	 * This method is used by the user interface front end to queue traffic for
	 * transmission to the remote host for forwarding to the remote socket.
	 *
	 * @param sessionId     The sessionID to to send traffic
	 * @param forwardUrl    The ip:port to which the base64Forward data should be
	 *                      sent
	 * @param base64Forward The traffic received from the local port, to be
	 *                      forwarded to the remote host
	 * @throws IllegalArgumentException in response to invalid sessionId
	 */
	public synchronized void forwardTCPTraffic(int sessionId, String forwardUrl, String base64Forward) {
		if (sessions.containsKey(sessionId)) {
			sessions.get(sessionId).forwardTCPTraffic(forwardUrl, base64Forward);
		} else {
			throw new IllegalArgumentException("Invalid session id");
		}
	}

	/**
	 * This method returns a set of all address:port forwards allocated to a session
	 *
	 * @param sessionId The sessionID check for available forwards
	 * @return the set of available forward addresses
	 * @throws IllegalArgumentException in response to invalid sessionId
	 */
	public synchronized Set<String> availableForwards(int sessionId) {
		if (sessions.containsKey(sessionId)) {
			return sessions.get(sessionId).availableForwards();
		} else {
			throw new IllegalArgumentException("Invalid session id");
		}
	}

	/**
	 * This method is used by C2Interface implementations to pull forwarded TCP
	 * traffic which has been queued for transmission and forward it.
	 *
	 * @param sessionId  The sessionID to to receive traffic
	 * @param forwardUrl The ip:port to which the base64Forward data should be sent
	 * @return Base64 encoded information to be send to the remote client for
	 *         forwarding
	 * @throws IllegalArgumentException in response to invalid sessionId
	 */
	public synchronized String grabForwardedTCPTraffic(int sessionId, String forwardUrl) {
		if (sessions.containsKey(sessionId)) {
			String returnedPackets = sessions.get(sessionId).grabForwardedTCPTraffic(forwardUrl);
			return returnedPackets;
		} else {
			throw new IllegalArgumentException("Invalid session id");
		}
	}

	/**
	 * This method is used by the user interface front end to pull traffic which has
	 * been received from the remote port forward and sends it on to the client.
	 *
	 * @param sessionId  The sessionID to to receive traffic
	 * @param forwardUrl The ip:port to which the base64Forward data should be sent
	 * @return Base64 encoded information to be send to the local forwarded port
	 * @throws IllegalArgumentException in response to invalid sessionId
	 */
	public synchronized String receiveForwardedTCPTraffic(int sessionId, String forwardUrl) {
		if (sessions.containsKey(sessionId)) {
			String returnedPackets = sessions.get(sessionId).receiveForwardedTCPTraffic(forwardUrl);
			return returnedPackets;
		} else {
			throw new IllegalArgumentException("Invalid session id");
		}
	}

	/**
	 * This method is used by C2Interface implementations to queue forwarded traffic
	 * received from the remote port forward daemon for transmission to the local
	 * forward port.
	 *
	 * @param sessionId     The sessionID to to receive traffic
	 * @param forwardUrl    The ip:port to which the base64Forward data should be
	 *                      sent
	 * @param base64Forward encoded information to be send to the local client for
	 *                      forwarding
	 * @throws IllegalArgumentException in response to invalid sessionId
	 */
	public synchronized void queueForwardedTCPTraffic(int sessionId, String forwardUrl, String base64Forward) {
		if (sessions.containsKey(sessionId)) {
			sessions.get(sessionId).queueForwardedTCPTraffic(forwardUrl, base64Forward);
		} else {
			throw new IllegalArgumentException("Invalid session id");
		}
	}

}
