package c2.telemetry.type;

import java.time.ZonedDateTime;

public class StringDatum extends Datum {

	public final String record;
	
	public StringDatum(ZonedDateTime dt, String record) {
		super(dt);
		this.record = record;
	}
	
	@Override
	public String printDatum() {
		return record;
	}

}
