package c2.http.httphandlers;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import c2.telemetry.TelemetryMemoryArchive;

public class TelemetryHandler implements HttpHandler{

	private TelemetryMemoryArchive archive;
	
	public TelemetryHandler(TelemetryMemoryArchive archive) {
		this.archive = archive;
	}
	
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
