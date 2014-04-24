package com.meow.kittypaintdev.server;

import java.net.*;
import java.io.*;


public abstract class BaseServer {
	private ServerSocket serversock;

	public BaseServer(int port) throws IOException{
		serversock = new ServerSocket(port);
	}
	
	class ClientThread implements Runnable {
		Socket clientsock;
		
		public ClientThread(Socket cs){
			clientsock = cs;
		}
		
		public void run() {
			try {
				handle(clientsock);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void serve_forever() throws IOException{
		while(true){
			final Socket clientsock = serversock.accept();
			
			try{
			    new Thread(new ClientThread(clientsock)).start();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	abstract public void handle(Socket clientSocket) throws IOException;
	
}
