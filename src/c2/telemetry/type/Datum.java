package c2.telemetry.type;

import java.time.ZonedDateTime;

public abstract class Datum {
	public final ZonedDateTime timestamp;

	protected Datum(ZonedDateTime timestamp) {
		this.timestamp = timestamp;
	}
	
	public abstract String printDatum();
}
