package c2.telemetry.type;

import java.time.ZonedDateTime;

public class DoubleDatum extends Datum {

	public final double record;
	
	public DoubleDatum (ZonedDateTime dt, double record) {
		super(dt);
		this.record = record;
	}
	
	@Override
	public String printDatum() {
		return record + "";
	}

}
