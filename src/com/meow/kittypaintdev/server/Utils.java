package com.meow.kittypaintdev.server;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.*;



class Base64 { // thx http://www.wikihow.com/Encode-a-String-to-Base64-With-Java
	 
    private static final String base64code = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "abcdefghijklmnopqrstuvwxyz" + "0123456789" + "+/";
 
    private static final int splitLinesAt = 76;
 
    public static byte[] zeroPad(int length, byte[] bytes) {
        byte[] padded = new byte[length]; // initialized to zero by JVM
        System.arraycopy(bytes, 0, padded, 0, bytes.length);
        return padded;
    }
    
    public static String encode(String string){
    	try {
			return encode(string.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
    }
    
    public static String encode(byte[] stringArray) {
 
        String encoded = "";
        // determine how many padding bytes to add to the output
        int paddingCount = (3 - (stringArray.length % 3)) % 3;
        // add any necessary padding to the input
        stringArray = zeroPad(stringArray.length + paddingCount, stringArray);
        // process 3 bytes at a time, churning out 4 output bytes
        // worry about CRLF insertions later
        for (int i = 0; i < stringArray.length; i += 3) {
            int j = ((stringArray[i] & 0xff) << 16) +
                ((stringArray[i + 1] & 0xff) << 8) + 
                (stringArray[i + 2] & 0xff);
            encoded = encoded + base64code.charAt((j >> 18) & 0x3f) +
                base64code.charAt((j >> 12) & 0x3f) +
                base64code.charAt((j >> 6) & 0x3f) +
                base64code.charAt(j & 0x3f);
        }
        // replace encoded padding nulls with "="
        return encoded.substring(0, encoded.length() - paddingCount) + "==".substring(0, paddingCount);
    }
 
}

public class Utils {
	
	public static long utc_now(){
		return System.currentTimeMillis();
	}
	
	/***************
	** HTTP STUFF **
	***************/
	public static String parse_path(String p){
		String reg = "^([a-z]+:\\/\\/[^\\/]*)?\\/*(?<path>.*)$";
		Pattern pat = Pattern.compile(reg);
		Matcher m = pat.matcher(p);
		if(!m.matches()){
			System.out.println("!!!!!!!! nomatch <" + p + ">");
		}
		return m.group("path");
	}
	
	public static HashMap<String, String> parse_headers(String headers_str){
		//sexy regex thx to http://stackoverflow.com/questions/4685217/parse-raw-http-headers
		HashMap<String, String> headers = new HashMap<String, String>();
		String reg = "^(?<name>.*?): ?(?<value>.*)\\s*$";
		Pattern pat = Pattern.compile(reg);
		String hs[] = headers_str.split("\n"); // \r?\n
		System.out.println(hs.length + "LENGTH");
		for(int i=0; i<hs.length; ++i){
			Matcher m = pat.matcher(hs[i]);
			if(m.matches()){
				headers.put(m.group("name"), m.group("value"));
			}
		}
		return headers;
	}
	
	public static HashMap<String, String> form_resp_headers(int body_len){
		return form_resp_headers(body_len, "text/html; charset=utf-8");
	}
	
	public static HashMap<String, String> form_resp_headers(int body_len, String mime){
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", mime);
		headers.put("Content-Length", "" + body_len);
		headers.put("Access-Control-Allow-Origin", "*");
		headers.put("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.put("Pragma", "no-cache");
		headers.put("Expires", "0");
		headers.put("Connection", "close");
		return headers;
	}
	
	public static String form_header_str(HashMap<String, String> headers){
		String str = "";
		Iterator<Map.Entry<String, String>> it = headers.entrySet().iterator();
		
		while(it.hasNext()){
			Map.Entry<String, String> entry = it.next();
			str += entry.getKey() + ": " + entry.getValue() + "\r\n";
		}
		return str;
	}
	
	/********************
	** WEBSOCKET STUFF **
	********************/
	public static String hash_websocket_key(String key){
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
			String magic_string = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
			md.update((key + magic_string).getBytes("UTF-8"));
			return Base64.encode(md.digest());
		} catch(Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static byte[] make_websocket_frame(String message){
		//http://stackoverflow.com/questions/8125507/how-can-i-send-and-receive-websocket-messages-on-the-server-side
		
		try{
			byte[] msg = message.getBytes("UTF-8");
			ByteArrayOutputStream frame = new ByteArrayOutputStream();
			frame.write(129);
			
			// write the message length
			if(msg.length <= 125){
				frame.write(msg.length);
			}else if(msg.length <= 65535){
				frame.write(126);
				frame.write((msg.length >> 8) & 255);
				frame.write(msg.length & 255); 
			}else{
				frame.write(127);
				int n;
				for(int i=0; i<8; ++i){
					n = (7 - i) * 8;
					frame.write((msg.length >> n) & 255);
				}
			}
			// write the message
			frame.write(msg);
			
			return frame.toByteArray();
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	public static String read_websocket_frame(InputStream ins) throws IOException{
		if(ins.read() < 0) return null;
		
		// read length
		int length = ins.read() & 127;
		if(length == 126){
			length = ins.read() << 8;
			length += ins.read();
		}else if(length == 127){
			length = 0;
			int n;
			for(int i=0; i<8; ++i){
				n = (7 - i) * 8;
				length += (ins.read() << n) & 255;
			}
		}
		
		// read message masks (THESE ARE STUPID WHY DO THEY EXIST)
		byte[] masks = new byte[4];
		for(int i=0; i<4; ++i) masks[i] = (byte)ins.read();
		
		// read and unmask message
		byte[] msg = new byte[length];
		for(int i=0; i<length; ++i){
			byte bytein = (byte)ins.read();
			msg[i] = (byte) (bytein ^ masks[i % 4]);
		}
		
		return new String(msg, "UTF-8");
	}
}
