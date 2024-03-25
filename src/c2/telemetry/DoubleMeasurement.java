package c2.telemetry;

import java.util.ArrayList;

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
	public Measurement dumpData(){
		Measurement snapshot = new DoubleMeasurement(this);
		data = new ArrayList<>();
		return snapshot;
	}
	
	protected void addDatum(DoubleDatum datum) {
		data.add(datum);
	}

}
