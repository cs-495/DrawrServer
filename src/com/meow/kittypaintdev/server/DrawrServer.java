package com.meow.kittypaintdev.server;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;

import com.meow.kittypaintdev.server.DrawrServerMap;
import com.meow.kittypaintdev.server.BaseServer;

class DrawrHandler {
	private static int unique_conn_id = 0;
	public static int socket_timeout = 60000; // 60 seconds i guess
	
	private Socket clientsock;
	private InputStream rstream;
	private BufferedReader rreader;
	private OutputStream wstream;
	private PrintWriter wwriter;
	private boolean verbose;
	private boolean debug = true;
	private boolean close_connection;
	private int conn_id;
	private String client_addr;
	
	private String path;
	private HashMap<String,String> headers;
	
	public DrawrHandler(Socket cs, boolean v) throws IOException{
		clientsock = cs;
		client_addr = cs.getRemoteSocketAddress().toString();
		verbose = v;
		rstream = cs.getInputStream();
		wstream = cs.getOutputStream();
		rreader = new BufferedReader(new InputStreamReader(rstream));
		wwriter = new PrintWriter(wstream, true);
		cs.setSoTimeout(socket_timeout);
	}
	
	public void log(String msg){
		if(verbose){
			System.out.println(conn_id + "|" + msg);
		}
	}
	
	public void debug(String msg){
		if(debug) log("!|" + msg);
	}
	
	public void handle() throws IOException{
		close_connection = false;
		conn_id = unique_conn_id;
		unique_conn_id++;
		
		log("**CONNECT: " + client_addr);
		
		try{
			while(!close_connection){
				handle_one_request();
			}
		}catch(SocketException e){
			debug("timeout");
		}catch(IOException e){
			debug("ioexception - closing");
		}
		
		log("**CLOSED: " + client_addr);
		clientsock.close(); // ?
	}
	
	public void handle_one_request() throws IOException{
		String line = rreader.readLine();
		if(line == null) return; // END OF STREAM
		line = line.trim();
		
		String[] words = line.split(" ");
		
		if(words.length != 2 && words.length != 3){
			send_error(404, "Invalid Request");
			return;
		}
		
		String command = words[0];
		path = words[1];
		
		log("DEBUG path<" + path + ">");
		if(command.equals("GET")){
			read_request_handlers();
			route();
		}else{
			send_error(404, "Invalid Request - only GET supported");
		}
	}
	
	public void read_request_handlers() throws IOException{
		String headers_str = "";
		while(true){
			String line = rreader.readLine();
			if(line == null) return; // END OF STREAM
			if(line.trim().equals("")){
				break; // blank line, finished reading headers
			}
			headers_str += line;
		}
		debug("headers done");
		debug("headers_____: " + headers_str);
		headers = Utils.parse_headers(headers_str);
		debug("headers: " + headers.toString());
	}
	
	public void route() throws IOException{
		String just_path = Utils.parse_path(path);

		Pattern pat = Pattern.compile("(?<path>[a-z]*)\\??(?<query>.*)$");
		Matcher m = pat.matcher(just_path);
		if(!m.matches()){
			send_error(404, "Not Found");
			return;
		}
		String mpath = m.group("path");
		String mquery = m.group("query");
		
		if(mpath.equals("drawr") || mpath.equals("") || headers.containsKey("Upgrade")){
			handle_drawr_session();
		}else if(mpath.equals("chunk")){
			request_chunk(mquery);
		}else{
			send_error(404, "Not Found");
		}
	}
	
	public String getPathEclipseSucks(String filename) throws IOException{
		String p = new File( "." ).getCanonicalPath();
		p += "/assets/" + filename;
		return p;
	}
	
	public void request_chunk(String query) throws IOException{
		// TODO: MAKE MORE EFFECIENT. THESE CHUNKS ARE ALL ALREADY LOADED IN THE MAP.
		Pattern pat = Pattern.compile("^(?<x>[0-9]+)&(?<y>[0-9]+)");
		Matcher m = pat.matcher(query);

		if(!m.matches()){
			send_error(404, "Not Found");
			return;
		}
		
		String chunk_path = "chunks/chunk" + m.group("x") + "x" + m.group("x") + ".png";
		String blank_path = "chunks/blank.png";
		
		try{
			chunk_path = getPathEclipseSucks(chunk_path);
			blank_path = getPathEclipseSucks(blank_path);
			
			File f = new File(chunk_path);
			if(f.exists() && !f.isDirectory()){
				send_binary(Files.readAllBytes(Paths.get(chunk_path)));
			}else{
				send_binary(Files.readAllBytes(Paths.get(blank_path)));
			}
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}
		close_connection = true;
	}
	
	/******************
	 * SOCKET SESSION 
	 * @throws IOException *
	 ******************/
	
	public void handle_drawr_session() throws IOException{
		String key = "";
		if(headers.containsKey("Sec-WebSocket-Key")){
			key = Utils.hash_websocket_key(headers.get("Sec-WebSocket-Key"));
		}
		debug("key: " + headers.get("Sec-WebSocket-Key"));
		debug("accept: " + key);
		send_websocket_handshake(key);
		
		send_frame(">pony1");
		send_frame(">VIDEO2");
		send_frame(">gams3");
		
		while(true){
			String m = read_frame();
			if(m == null || m.equals("exit")) break;
			
			// FIBONACCI BUTTSORT
			String o = "<b><i>";
			String[] a = m.split("");
			for(int i=0;i<a.length;++i){
				if(i%2 == 0){
					o += "<u>" + a[i] + "</u>";
				}else{
					o += "<span style='text-decoration: overline;'>" + a[i] + "</span>";
				}
			}
			send_frame(o + "</i></b>");
		}
	}
	
	
	public void send_frame(String msg) throws IOException{
		wstream.write(Utils.make_websocket_frame(msg));
	}
	
	public String read_frame() throws IOException{
		return Utils.read_websocket_frame(rstream);
	}
	
	/***********
	 * IO'NPUT 
	 * @throws IOException *
	 ***********/
	
	public void send_response(int code, String httpmsg, String body, String mime) throws IOException{
		String http_resp = "HTTP/1.1 " + code + " " + httpmsg;
		String headers = Utils.form_header_str(Utils.form_resp_headers(body.length(), mime));
		String full_resp = http_resp + "\r\n" + headers + "\r\n" + body;

		wwriter.print(full_resp);
		wwriter.flush();
		close_connection = true;
	}
	
	public void send_error(int code, String message) throws IOException{
		String body = "error. " + code + " " + message + "\n";
		debug("send_error() " + "HTTP/1.1 " + code + " " + message);
		send_response(code, message, body, "text/html; charset=utf-8");
	}
	
	public void send_response(String body) throws IOException{
		send_response(200, "OK", body, "text/html; charset=utf-8");
	}
	
	public void send_binary(byte[] body) throws IOException{ send_binary(body, "image/png"); }
	public void send_binary(byte[] body, String mime) throws IOException{
		String http_resp = "HTTP/1.1 200 OK";
		String headers = Utils.form_header_str(Utils.form_resp_headers(body.length, mime));
		String full_resp = http_resp + "\r\n" + headers + "\r\n";
		
		debug("send_binary() " + http_resp);
		
		wwriter.print(full_resp);
		wstream.write(body);
		close_connection = true;
		wwriter.flush();
		wstream.flush();
	}
	
	public void send_websocket_handshake(String accept_key) throws IOException{
		String http_resp = "HTTP/1.1 101 WebSocket Protocol Handshake";
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("Connection", "Upgrade");
		headers.put("Upgrade", "WebSocket");
		if(!accept_key.equals("")){
			headers.put("Sec-WebSocket-Accept", accept_key);
		}
		String full_resp = http_resp + "\r\n" + Utils.form_header_str(headers) + "\r\n";
		
		wwriter.print(full_resp);
		wwriter.flush();
		wstream.flush();
	}
}

public class DrawrServer extends BaseServer{ 
	private static String host = ""; //"127.0.0.1"
	private static int port = 27182; //80
	private static boolean verbose = true;
	
	public DrawrServer(int port) throws IOException{
		super(port);
	}

	public void handle(Socket clientsock) throws IOException{
		new DrawrHandler(clientsock, verbose).handle();
	}

	public static void main(String[] args) throws IOException {
		DrawrServer drawrserver = new DrawrServer(port);
		drawrserver.serve_forever();
	}
}
