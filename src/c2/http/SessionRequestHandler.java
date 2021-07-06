package c2.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import c2.session.IOManager;
import c2.session.Session;

public class SessionRequestHandler implements HttpHandler {
	private IOManager io;
	
	public SessionRequestHandler(IOManager io) {
		this.io = io;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		
		
		Gson gson = new Gson();
		String response = "acknowledged response";
		if (t.getRequestMethod().equals("GET")) {
			List<AvailableSession> sessions = new ArrayList<>();
			for(Session session : io.getSessions()) {
				AvailableSession sessionInf = new AvailableSession();
				sessionInf.hostname = session.hostname;
				sessionInf.username = session.username;
				sessionInf.protocol = session.protocol;
				sessionInf.sessionId = session.id;
				sessions.add(sessionInf);
			}
			
			response = gson.toJson(sessions);
		}

		t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		t.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
		t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,HEAD,OPTIONS,POST,PUT");
		t.getResponseHeaders().add("Access-Control-Allow-Headers", "Access-Control-Allow-Headers, Origin,Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");
		t.sendResponseHeaders(200, response.getBytes().length);
		OutputStream os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}
	
	
}
