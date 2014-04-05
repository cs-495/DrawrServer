package com.meow.kittypaintdev.server;

import java.io.*;
import java.net.*;

import com.meow.kittypaintdev.server.BaseServer;
import com.meow.kittypaintdev.server.DrawrServerMap;


public class DrawrServer extends BaseServer{ 
	private static int port = 27182; //80

	private DrawrServerMap drawr_map;
	private DrawrBrushes drawr_brushes;
	private Logger logger;
	
	public DrawrServer(int port) throws IOException{
		super(port);
		logger = new Logger();
		drawr_brushes = new DrawrBrushes();
		drawr_map = new DrawrServerMap();
	}

	public void handle(Socket clientsock) throws IOException{
		new DrawrHandler(clientsock, drawr_map, drawr_brushes, logger).handle();
	}

	public static void main(String[] args) throws IOException {
		// getPidFile().deleteOnExit(); //? http://barelyenough.org/blog/2005/03/java-daemon/
		
		/*
		 * [4:05:52 PM] KittyKatze: http://stackoverflow.com/questions/1787548/how-many-threads-to-create
		 * [4:05:57 PM] KittyKatze: http://stackoverflow.com/questions/130506/how-many-threads-should-i-use-in-my-java-program
		 * [4:07:35 PM] KittyKatze: http://docs.oracle.com/javase/tutorial/essential/concurrency/pools.html
		 * public static final int THREADS =  Runtime.getRuntime().availableProcessors();
		 * 
		 * http://codelatte.wordpress.com/2013/11/08/a-simple-cachedthreadpool-example/
		 * http://codelatte.wordpress.com/2013/11/09/a-simple-newfixedthreadpool-example/
		 */
		
		DrawrServer drawrserver = new DrawrServer(port);
		drawrserver.serve_forever();
	}
}
