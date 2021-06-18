package c2.admin;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Console;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalConnection {
	
	public static void main(String args[]) throws NumberFormatException, UnknownHostException, IOException{
		LocalConnection lc = new LocalConnection();
		lc.engage(args);
	}
	
	public void engage (String args[]) throws NumberFormatException, UnknownHostException, IOException {
		Socket remote = new Socket(args[0], Integer.parseInt(args[1]));

		OutputStreamWriter bw = new OutputStreamWriter(new BufferedOutputStream(remote.getOutputStream()));
		BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

		Console console = System.console();

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
								System.out.println("Received File");
							} else {
								System.out.println(output);
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
			

			String nextCmd = console.readLine();
			if (nextCmd.equals("quitLocal")) {
				stayAlive = false;
			} else if (nextCmd.startsWith("sendFile")) {
				// TODO: need to handle bad command format here
				String filename = nextCmd.split(" ")[1];
				// TODO: handle exception from bad name
				byte[] fileBytes = Files.readAllBytes(Paths.get(filename));
				byte[] encoded = Base64.getEncoder().encode(fileBytes);
				String encodedString = new String(encoded, StandardCharsets.US_ASCII);
				bw.write("<control> download " + Paths.get(filename).getFileName().toString().replaceAll(" ", "_") + " " + encodedString + System.lineSeparator());
				bw.flush();
			} else {
				bw.write(nextCmd + System.lineSeparator());
				bw.flush();
			}
			/*
			 * //String nextCmd = console.readLine(); if(cReader.ready()) {
			 * System.out.println("Reading from console..."); String nextCmd =
			 * cReader.readLine(); System.out.println("Echo: '" + nextCmd + "'");
			 * 
			 * if(nextCmd.equals("quitLocal")){ stayAlive = false; }else
			 * if(nextCmd.startsWith("sendFile")) { //TODO: need to handle bad command
			 * format here String filename = nextCmd.split(" ")[1]; //TODO: handle exception
			 * from bad name byte[] fileBytes = Files.readAllBytes(Paths.get(filename));
			 * byte[] encoded = Base64.getEncoder().encode(fileBytes); String encodedString
			 * = new String(encoded,StandardCharsets.US_ASCII);
			 * bw.write("<control> download " + encodedString + System.lineSeparator());
			 * bw.flush(); }else{ bw.write(nextCmd + System.lineSeparator()); bw.flush(); }
			 * }
			 * 
			 * if(br.ready()) { String output = br.readLine(); if(output != null)
			 * if(output.startsWith("<control> uplinked ")) { byte[] data =
			 * Base64.getDecoder().decode(output.split(" ")[2]); try (OutputStream stream =
			 * new FileOutputStream("decode")) { stream.write(data); }
			 * console.writer().println("Received File"); }else {
			 * console.writer().println(output); } }
			 * 
			 * try { Thread.sleep(100); } catch (InterruptedException e) { // TODO
			 * Auto-generated catch block e.printStackTrace(); }
			 * 
			 */

		}

		remote.close();
	}
}
