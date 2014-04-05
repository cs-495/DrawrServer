package com.meow.kittypaintdev.server;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;

import com.meow.kittypaintdev.server.BaseServer;
import com.meow.kittypaintdev.server.DrawrServerMap;


public class DrawrServer extends BaseServer{ 
	private static String host = ""; //"127.0.0.1"
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
		DrawrServer drawrserver = new DrawrServer(port);
		drawrserver.serve_forever();
	}
}
