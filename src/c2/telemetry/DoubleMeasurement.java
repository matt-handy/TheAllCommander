package c2.telemetry;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

import c2.http.httphandlers.telemetry.IllegalTelemetryFormatException;
import c2.http.httphandlers.telemetry.TelemetryReport;
import c2.telemetry.type.DoubleDatum;

public class DoubleMeasurement extends Measurement {

	public DoubleMeasurement(String hostname, String measurementName, boolean processSpecific) {
		super(hostname, measurementName, processSpecific);
	}

	private DoubleMeasurement(DoubleMeasurement original) {
		super(original.hostname, original.measurementName, original.processSpecific);
		data = new ArrayList<>(original.data);
	}

	@Override
	public Measurement dumpData() {
		Measurement snapshot = new DoubleMeasurement(this);
		data = new ArrayList<>();
		return snapshot;
	}

	protected void addDatum(DoubleDatum datum) {
		data.add(datum);
	}

	@Override
	public void ingestReport(TelemetryReport report) throws IllegalTelemetryFormatException {
		try {
			double value = Double.valueOf(report.getValue());
			ZonedDateTime dt = ZonedDateTime.parse(report.getTimestamp());
			addDatum(new DoubleDatum(dt, value));
		} catch (NumberFormatException ex) {
			throw new IllegalTelemetryFormatException("Cannot interpret double from: " + report.getValue());
		} catch (DateTimeParseException ex) {
			throw new IllegalTelemetryFormatException(
					"Cannot interpret zoned timestamp from: " + report.getTimestamp());
		}
	}

}
