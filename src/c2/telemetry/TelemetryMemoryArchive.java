package c2.telemetry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import c2.telemetry.type.DoubleDatum;

public class TelemetryMemoryArchive {

	private Map<String, Set<Measurement>> runningArchive = new HashMap<>();
	private Map<String,Map<Integer, Set<Measurement>>> pidMeasurements = new HashMap<>();
	
	public Set<Measurement> dumpHostMeasurement(String hostname){
		Set<Measurement> dump = new HashSet<>();
		if(runningArchive.containsKey(hostname)) {
			for(Measurement measure: runningArchive.get(hostname)) {
				dump.add(measure.dumpData());
			}
			return dump;
		}else {
			throw new IllegalArgumentException("Unknown hostname: " + hostname);
		}
	}
	
	public void addDoubleHostDatum(String hostname, String measurementName, DoubleDatum dd) {
		if(runningArchive.containsKey(hostname)) {
			Set<Measurement> measurements = runningArchive.get(hostname);
			Measurement measure = null;
			for(Measurement candidate : measurements) {
				if(candidate.measurementName.equals(measurementName)) {
					measure = candidate;
					break;
				}
			}
			if(measure == null) {
				measure = new DoubleMeasurement(hostname, measurementName, false);
				measurements.add(measure);
			}
			if(!(measure instanceof DoubleMeasurement)) {
				throw new IllegalArgumentException("Measurement: " + measurementName + " is not a Double");
			}
			DoubleMeasurement measureCast = (DoubleMeasurement) measure;
			measureCast.addDatum(dd);
		}
	}
	
	
}
