package com.meow.kittypaintdev.server;

import java.io.*;
import java.util.Date;

public class Logger {
	
	File log_file;
	PrintWriter log;
	File debug_file;
	PrintWriter debug;
	File error_file;
	PrintWriter error;
	
	public Logger() throws IOException{
		log_file = new File(Utils.getPathInLog("drawr.log"));
		debug_file = new File(Utils.getPathInLog("debug.log"));
		error_file = new File(Utils.getPathInLog("error.log"));
		log = new PrintWriter(log_file);
		debug = new PrintWriter(debug_file);
		error = new PrintWriter(error_file);
		log.println("***starting...");
		log.flush();
	}
	
	public void close() throws IOException{
		log.close();
		debug.close();
		error.close();
	}
	
	public void write_logfile(PrintWriter bw, String msg){
		bw.println("[" + new Date().toString() + "] " + msg);
		bw.flush();
	}
	
	public void log(String msg){
		// disabled for now - log file gets really big really fast
		//write_logfile(log, msg);
	}
	
	public void debug(String msg){
		write_logfile(debug, msg);
	}
	
	public void error(String msg){
		write_logfile(error, msg);
	}
}
