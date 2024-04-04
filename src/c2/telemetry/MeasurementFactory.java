package c2.telemetry;

import c2.http.httphandlers.telemetry.IllegalTelemetryFormatException;
import c2.http.httphandlers.telemetry.TelemetryReport;

public class MeasurementFactory {

	public static Measurement getMeasurement(TelemetryReport report) throws IllegalTelemetryFormatException{
		if(report.getType() == "DOUBLE") {
			return new DoubleMeasurement(report.getHostname(), report.getMeasurementName(), report.isPidSpecific());
		}else {
			throw new IllegalTelemetryFormatException("Unknown type of telemetry");
		}
	}
}
