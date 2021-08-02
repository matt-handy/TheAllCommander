package c2.http;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import c2.Constants;
import c2.session.IOManager;

public class PortForwardHandler implements HttpHandler {
	private IOManager io;

	public PortForwardHandler(IOManager io) {
		this.io = io;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		// TODO consolidate with HTTPSHandler
		String response = "acknowledged response";
		String hostname = t.getRequestHeaders().getFirst("Hostname");
		String pid = t.getRequestHeaders().getFirst("PID");
		String username = t.getRequestHeaders().getFirst("Username");
		String forwardRequest = t.getRequestHeaders().getFirst("ForwardRequest");

		String protocol = "HTTPS";

		Integer sessionId = null;
		if (hostname == null || pid == null || username == null) {
			t.sendResponseHeaders(400, response.getBytes().length);
			response = "Proxy return requires Hostname, PID, and Username headers";
		} else {
			String sessionUID = hostname + ":" + username + ":" + protocol;
			sessionId = io.getSessionId(sessionUID);
			if (sessionId == null) {
				t.sendResponseHeaders(400, response.getBytes().length);
				response = "Invalid session";
			} else {

				if (t.getRequestMethod().equals("GET")) {
					String b64Data = io.grabForwardedTCPTraffic(sessionId, forwardRequest);
					if (b64Data == null) {
						response = Constants.PORT_FORWARD_NO_DATA;
					} else {
						response = b64Data;
					}
				} else if (t.getRequestMethod().equals("POST")) {
					byte[] data = t.getRequestBody().readAllBytes();
					String b64Data = new String(data);
					io.queueForwardedTCPTraffic(sessionId, forwardRequest, b64Data);
				}
				t.sendResponseHeaders(200, response.getBytes().length);
			}
		}

		t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

		OutputStream os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}
}
