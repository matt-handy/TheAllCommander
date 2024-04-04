package c2.http.httphandlers.telemetry;

public class IllegalTelemetryFormatException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public IllegalTelemetryFormatException(String message) {
		super(message);
	}

}
