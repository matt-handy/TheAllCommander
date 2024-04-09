package c2.session.wizard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import c2.Constants;

public interface Wizard {

	public void init(Properties properties) throws WizardConfigurationException;
	
	public String getHumanReadableName();
	
	public String getInvocationName();
	
	public void surrenderControlFlow(OutputStreamWriter bw, BufferedReader br) throws IOException;
	
	public static List<Wizard> initializeWizards(Properties properties){
		List<Wizard> wizards = new ArrayList<>();
		String serviceListString = properties.getProperty(Constants.WIZARDS);
		String[] serviceClassNames = serviceListString.split(",");
		for (String className : serviceClassNames) {
			try {
				Class<?> c = Class.forName(className);
				Constructor<?> cons = c.getConstructor();
				Object object = cons.newInstance();
				Wizard newModule = (Wizard) object;
				newModule.init(properties);
				wizards.add(newModule);
			} catch (Exception ex) {
				System.out.println("Unable to load class: " + className);
				ex.printStackTrace();
			}
		}
		return wizards;
	}
}
