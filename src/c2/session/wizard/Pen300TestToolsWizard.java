package c2.session.wizard;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import util.test.OutputStreamWriterHelper;

public class Pen300TestToolsWizard implements Wizard{

	public static String INVOKE_COMMAND = "study_pen300";
	
	private List<Wizard> children = new ArrayList<>();
	
	@Override
	public void init(Properties properties) throws WizardConfigurationException {
		try (InputStream input = new FileInputStream(Paths.get("config", "pen300_study_tools", "msf_caesar_wrapper.properties").toFile())) {
			Properties prop = new Properties();
			prop.load(input);
			MsfShellcodeCaesarWizard wizard = new MsfShellcodeCaesarWizard();
			wizard.init(prop);
			children.add(wizard);
		} catch (IOException ex) {
			throw new WizardConfigurationException("Cannot load configuration for MSF Caesar Wizard");
		}
	}

	@Override
	public String getHumanReadableName() {
		return "PEN300 Study Support Tools";
	}

	@Override
	public String getInvocationName() {
		return INVOKE_COMMAND;
	}

	@Override
	public void surrenderControlFlow(OutputStreamWriter bw, BufferedReader br) throws IOException {
		boolean stayAlive = true;
		while(stayAlive) {
			OutputStreamWriterHelper.writeAndSend(bw, "Welcome to the PEN300 study wizard, which automates a some tool generations and configuration. Please select one of the available tool wizards, or 'quit' to return.");
			for(Wizard wizard : children) {
				OutputStreamWriterHelper.writeAndSend(bw, wizard.getInvocationName() + " - " + wizard.getHumanReadableName());
			}
			String line = br.readLine();
			if(line.equalsIgnoreCase("quit")) {
				stayAlive = false;
			}else {
				boolean foundAWizard = false;
				for(Wizard wizard : children) {
					if(wizard.getInvocationName().equalsIgnoreCase(line)) {
						wizard.surrenderControlFlow(bw, br);
						foundAWizard = true;
						break;
					}
				}
				if(!foundAWizard) {
					OutputStreamWriterHelper.writeAndSend(bw, "I'm sorry, I didn't understand that.");
				}
			}
			
		}
		
	}

}
