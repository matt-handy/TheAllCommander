package c2.telemetry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import c2.http.httphandlers.telemetry.IllegalTelemetryFormatException;
import c2.http.httphandlers.telemetry.TelemetryReport;

public class TelemetryMemoryArchive {

	private Path telemetryArchive;

	public TelemetryMemoryArchive(Path telemetryArchive) {
		this.telemetryArchive = telemetryArchive;
	}

	public void ingestTelemetryReport(TelemetryReport report) throws IllegalTelemetryFormatException {
		try {
			Path archiveFolder = Paths.get(report.getHostname());
			if (report.isPidSpecific()) {
				archiveFolder = Paths.get(archiveFolder.toString(), report.getPid());
			}
			Path archiveFile = telemetryArchive
					.resolve(archiveFolder.resolve(Paths.get(report.getMeasurementName())));
			if (Files.notExists(archiveFile.getParent())) {
				Files.createDirectories(archiveFile.getParent());
			}
			if(Files.notExists(archiveFile)) {
				Files.createFile(archiveFile);
			}
			String logLine = report.getTimestamp() + " '" + report.getValue() + "' " + report.getType()
					+ System.lineSeparator();
			Files.writeString(archiveFile, logLine, StandardOpenOption.APPEND);
		} catch (IOException ex) {
			ex.printStackTrace();
			throw new IllegalTelemetryFormatException("Unable to write to log file for telm: " + ex.getMessage());
		}

	}

}
