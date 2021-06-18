package c2.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import c2.HarvestProcessor;

public class HarvestHandler implements HttpHandler {
	private HarvestProcessor harvester;

	public HarvestHandler(HarvestProcessor harvester) {
		this.harvester = harvester;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		String response = "acknowledged response";
		if (t.getRequestMethod().equals("GET")) {
			response = "GET not implemented";
		} else if (t.getRequestMethod().equals("POST")) {
			InputStreamReader isr = new InputStreamReader(t.getRequestBody(), "utf-8");
			BufferedReader br = new BufferedReader(isr);
			StringBuilder file = new StringBuilder();
			String nextLine;
			while ((nextLine = br.readLine()) != null) {
				file.append(nextLine);
				file.append(System.lineSeparator());
			}

			String hostname = t.getRequestHeaders().getFirst("Hostname");
			String pid = t.getRequestHeaders().getFirst("PID");
			String username = t.getRequestHeaders().getFirst("Username");
			String harvestHeader = t.getRequestHeaders().getFirst("Harvest");
			harvester.processHarvest(harvestHeader, hostname, pid, username, file.toString());
		}

		t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		t.sendResponseHeaders(200, response.getBytes().length);
		OutputStream os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}
}
