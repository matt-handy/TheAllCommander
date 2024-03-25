package c2.telemetry;

import java.util.ArrayList;
import java.util.List;

import c2.telemetry.type.Datum;

public abstract class Measurement {

	public final String hostname;
	public final boolean processSpecific;
	public final String measurementName;

	protected Measurement(String hostname, String measurementName, boolean processSpecific) {
		this.hostname = hostname;
		this.processSpecific = processSpecific;
		this.measurementName = measurementName;
	}

	protected List<Datum> data = new ArrayList<>();

	@Override
	public boolean equals(Object o1) {
		if (o1 instanceof Measurement) {
			Measurement other = (Measurement) o1;
			return hostname.equals(other.hostname) && processSpecific == other.processSpecific
					&& measurementName.equals(other.measurementName);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return hostname.hashCode() + measurementName.hashCode() + (processSpecific ? 0 : 1);
	}

	public List<Datum> peekData() {
		return new ArrayList<>(data);
	}

	public abstract Measurement dumpData();

}
