package com.meow.kittypaintdev.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DrawrHandler implements DrawrEvent {
	private static int unique_conn_id = 0;
	public static int socket_timeout = 60000; // 60 seconds i guess
	
	private static int depth = 0; // weird debug thing - ignore
	private int thisdepth;
	
	private Socket clientsock;
	private InputStream rstream;
	private BufferedReader rreader;
	private OutputStream wstream;
	private PrintWriter wwriter;
	private String client_addr;
	private boolean close_connection;
	private int conn_id;
	
	private String path;
	private HashMap<String,String> headers;
	private DrawrServerMap drawr_map;
	private DrawrBrushes drawr_brushes;
	private Logger logger;
	
	public DrawrHandler(Socket cs, DrawrServerMap dm, DrawrBrushes db, Logger logger) throws IOException{
		this.logger = logger;
		clientsock = cs;
		client_addr = cs.getRemoteSocketAddress().toString();
		rstream = cs.getInputStream();
		wstream = cs.getOutputStream();
		rreader = new BufferedReader(new InputStreamReader(rstream));
		wwriter = new PrintWriter(wstream, true);
		cs.setSoTimeout(socket_timeout);
		
		drawr_map = dm;
		drawr_brushes = db;
	}
	
	public int getId(){
		return conn_id;
	}
	
	public void depthlog(String msg){
		for(int i=0;i<thisdepth;++i){
			System.out.print("  ");
		}
		System.out.println(msg);
	}
	public void log(String msg){
		logger.log(conn_id + "|" + msg);
	}
	public void debug(String msg){
		logger.debug(conn_id + "|" + msg);
	}
	public void error(String msg){
		logger.error(conn_id + "|" + msg);
	}
	
	public void handle() throws IOException{
		close_connection = false;
		conn_id = unique_conn_id;
		unique_conn_id++;
		
		//**
		thisdepth = depth;
		depth++;
		
		//depthlog("**CONN>: [" + conn_id + ", " + thisdepth + "] " + client_addr);
		
		try{
			while(!close_connection){
				handle_one_request();
			}
		}catch(SocketException e){
			debug("timeout");
		}catch(IOException e){
			debug("ioexception - closing");
			e.printStackTrace();
		}
		
		//depthlog("**xxxx<: [" + conn_id + ", " + thisdepth + "] " + client_addr);
		depth--;
		drawr_map.removeClient(conn_id);
		clientsock.close();
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
		
		if(command.equals("GET")){
			log("connect: "+client_addr+"; GET " + path + "");
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
			headers_str += line + "\n";
		}
		headers = Utils.parse_headers(headers_str);
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
			send_error(404, "Not Found (route)");
		}
	}
	
	public void request_chunk(String query) throws IOException{
		Pattern pat = Pattern.compile("^(?<x>[\\-0-9]+)&(?<y>[\\-0-9]+).*");
		Matcher m = pat.matcher(query);

		if(!m.matches()){
			send_error(404, "Not Found (request_chunk)");
			return;
		}

		int numx = Integer.parseInt(m.group("x"));
		int numy = Integer.parseInt(m.group("y"));
		send_binary(drawr_map.getChunkPng(numx, numy));
		
		/*
		String chunk_path = "chunks/chunk" + m.group("x") + "x" + m.group("y") + ".png";
		String blank_path = "chunks/blank.png";
		try{
			chunk_path = Utils.getPathEclipseSucks(chunk_path);
			blank_path = Utils.getPathEclipseSucks(blank_path);
			
			File f = new File(chunk_path);
			if(f.exists() && !f.isDirectory()){
				send_binary(Files.readAllBytes(Paths.get(chunk_path)));
			}else{
				send_binary(Files.readAllBytes(Paths.get(blank_path)));
			}
		}catch(FileNotFoundException e){
			e.printStackTrace();
			send_error(404, "Not Found (file)");
		}*/
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
		send_websocket_handshake(key);
		drawr_map.addClient(this);
		
		send_frame(">pony1");
		send_frame(">VIDEO2");
		send_frame(">gams3");
		
		while(true){
			String m = read_frame();
			if(m == null || m.equals("exit")) break;
			
			if(m.equals("PING")){
				send_frame("PONG");
			}else if(m.startsWith("BUTTSORT")){
				// FIBONACCI BUTTSORT
				String x = m.substring(m.indexOf(":") + 1);
				String o = "<b><i>";
				String[] a = x.toUpperCase().split("");
				for(int i=0;i<a.length;++i){
					if(i%2 == 0){
						o += "<u>" + a[i] + "</u>";
					}else{
						o += "<span style='text-decoration: overline;'>" + a[i] + "</span>";
					}
				}
				send_frame(o + "</i></b>");
			}else if(m.startsWith("ADDPOINTBR")){
				String[] parts = m.split("\\:");
				if(parts.length == 8){
					try{
						int x = Integer.parseInt(parts[1]);
						int y = Integer.parseInt(parts[2]);
						String path = parts[3];
						int size = Integer.parseInt(parts[4]);
						int r = Integer.parseInt(parts[5]);
						int g = Integer.parseInt(parts[6]);
						int b = Integer.parseInt(parts[7]);
						
						Brush br = drawr_brushes.getBrush(path, size, r, g, b);
						if(br != null){
							drawr_map.addPoint(x, y, br, size);
						}
						
					}catch(Exception e){
						debug("ERROR: parsing frame <" + m + ">");
					}
				}
			}else if(m.startsWith("ADDSTAMPBR")){
				String[] parts = m.split("\\:");
				if(parts.length == 5){
					try{
						int x = Integer.parseInt(parts[1]);
						int y = Integer.parseInt(parts[2]);
						String path = parts[3];
						int size = Integer.parseInt(parts[4]);
						
						Brush br = drawr_brushes.getStamp(path, size);
						if(br != null){
							drawr_map.addPoint(x, y, br, size);
						}
						
					}catch(Exception e){
						debug("ERROR: parsing frame <" + m + ">");
					}
				}
			}else{
				send_frame("UNKNOWN:MESSAGE:" + m);
			}
		}
		close_connection = true;
	}

	@Override
	public void update(int x, int y, byte[] bin_img) throws IOException {
		// TODO: client keeps track of its viewport with "UPDATESFOR:x1:y1:x2:y2", and only takes updates in that range
		//send_frame("UPDATE:" + x + ":" + y); // tells client to load chunk via HTTP
		byte[] msg_start = ("CHUNK:" + x + ":" + y + ":").getBytes("UTF-8");
		byte[] msg = new byte[msg_start.length + bin_img.length];
		System.arraycopy(msg_start, 0, msg, 0, msg_start.length);
		System.arraycopy(bin_img, 0, msg, msg_start.length, bin_img.length);
		send_frame(msg);
	}
	@Override
	public void update(int x, int y) throws IOException {
		// TODO: client keeps track of its viewport with "UPDATESFOR:x1:y1:x2:y2", and only takes updates in that range
		send_frame("UPDATE:" + x + ":" + y); // tells client to load chunk via HTTP
	}
	

	public void send_frame(String msg) throws IOException{
		try{
			wstream.write(Utils.make_websocket_frame(msg));
		}catch(IOException e){
			System.out.println("ERROR: " + msg);
			throw e;
		}
	}
	public void send_frame(byte[] msg) throws IOException{
		try{
			wstream.write(Utils.make_websocket_frame(msg));
		}catch(IOException e){
			System.out.println("ERROR: " + msg);
			throw e;
		}
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
		//debug("send_error() " + "HTTP/1.1 " + code + " " + message);
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
		
		//debug("send_binary() " + http_resp);
		
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