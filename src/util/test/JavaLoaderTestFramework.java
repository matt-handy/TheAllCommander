package util.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

import c2.java.DaemonLoaderGenerator;
import util.Time;

public class JavaLoaderTestFramework {

	private static Path TMP_JAR = Paths.get("DaemonLoader.jar");
	
	private Process httpServerProcess;
	private Process jarProcess;
	
	private List<String> jarsToHost;
	private String mainMethod;
	
	public void init(Path testDaemonJar, List<String> jarsToHost, String mainMethod) throws Exception {
		this.mainMethod = mainMethod;
		this.jarsToHost = jarsToHost;
		
		//Stand up HTTP server
		String pythonServerArgs[] = {"python", "-m", "http.server", "8011", "-d", Paths.get("").toString()};
		httpServerProcess = Runtime.getRuntime().exec(pythonServerArgs);
		System.out.println("Python server started");
		
		
		
		//Move in the jars we need
		for(String jar : jarsToHost) {
			System.out.println("Moving jar: " + jar);
			Path pJar = Paths.get(testDaemonJar.toString(), jar);
			Files.copy(pJar, Paths.get(jar));
		}
		System.out.println("Test jars staged");
		
	}
	
	public void generateAndPlaceLoaderJar() throws Exception {
		String b64 = DaemonLoaderGenerator.generateDaemonLoaderB64Exe("localhost:8011", jarsToHost, mainMethod);
		byte[] jar = Base64.getDecoder().decode(b64);
		Files.write(TMP_JAR, jar);
	}
	
	public void launchJarAndReturn() throws Exception {
		jarProcess = Runtime.getRuntime().exec("java -cp .\\" + TMP_JAR + " DaemonLoader");
	}
	
	public boolean isLoaderJarRunning() {
		return jarProcess.isAlive();
	}
	
	public String getLoaderJarOutput() throws Exception {
		StringBuilder sb = new StringBuilder();
		int availErrors = jarProcess.getErrorStream().available();
		if(availErrors > 0) {
			sb.append("stderr: " + new String(jarProcess.getErrorStream().readNBytes(availErrors)) + System.lineSeparator());
		}
		int availOp = jarProcess.getInputStream().available();
		if(availOp > 0) {
			sb.append("stdout: " + new String(jarProcess.getInputStream().readNBytes(availOp)) + System.lineSeparator());
		}
		return sb.toString();
	}
	
	public void teardownTest() throws Exception{
		httpServerProcess.destroyForcibly();
		for(String jar : jarsToHost) {
			Files.delete(Paths.get(jar));
		}
		
		jarProcess.destroyForcibly();
		while(jarProcess.isAlive()) {
			Time.sleepWrapped(500);
		}
		Files.delete(TMP_JAR);
	}
	
}
