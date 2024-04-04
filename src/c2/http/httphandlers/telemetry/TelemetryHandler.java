package c2.http.httphandlers.telemetry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import c2.telemetry.TelemetryMemoryArchive;

public class TelemetryHandler implements HttpHandler {

	private TelemetryMemoryArchive archive;

	public TelemetryHandler(TelemetryMemoryArchive archive) {
		this.archive = archive;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String response = "acknowledged response";
		if (exchange.getRequestMethod().equals("GET")) {
			response = "GET not implemented";
		} else if (exchange.getRequestMethod().equals("POST")) {
			InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
			BufferedReader br = new BufferedReader(isr);
			StringBuilder jsonBlob = new StringBuilder();
			String nextLine;
			while ((nextLine = br.readLine()) != null) {
				jsonBlob.append(nextLine);
				jsonBlob.append(System.lineSeparator());
			}
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				TelemetryReport report = objectMapper.readValue(jsonBlob.toString(), TelemetryReport.class);
				try {
					archive.ingestTelemetryReport(report);
				} catch (IllegalTelemetryFormatException ex) {
					response = ex.getMessage();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		exchange.sendResponseHeaders(200, response.getBytes().length);
		OutputStream os = exchange.getResponseBody();
		os.write(response.getBytes());
		os.close();

	}

}
