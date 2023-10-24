package util.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import c2.Runner;

public class ServerRunner implements Runnable{
	public Runner main = new Runner();
	private String propName;
	
	public ServerRunner(String propName) {
		this.propName = propName;
	}

	@Override
	public void run() {
		try (InputStream input = new FileInputStream("config" + File.separator + propName)) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			main.engage(prop);

		} catch (IOException ex) {
			System.out.println("ServerRunner: Unable to load TheAllCommanderServer config file");
			ex.printStackTrace();
		}
	}
}
