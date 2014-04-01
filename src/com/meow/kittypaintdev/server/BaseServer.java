package com.meow.kittypaintdev.server;

import java.net.*;
import java.io.*;

public abstract class BaseServer {
	private ServerSocket serversock;

	public BaseServer(int port) throws IOException{
		serversock = new ServerSocket(port);
	}
	
	public void serve_forever() throws IOException{
		while(true){
			final Socket clientsock = serversock.accept();
			PrintWriter out = new PrintWriter(clientsock.getOutputStream(), true);  
			out.println("HTTP/1.1 200 OK\nContent-Type: text/html\n\nasdfasdf");
			
			Thread t = new Thread(){
		    	public void run(){
		    		try {
						handle(clientsock);
					} catch (IOException e) {
						e.printStackTrace();
					}
		    	}
		    };
			
			try{
			    t.start();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	abstract public void handle(Socket clientSocket) throws IOException;
	

}
