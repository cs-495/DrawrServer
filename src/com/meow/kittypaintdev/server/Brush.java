package com.meow.kittypaintdev.server;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;

import javax.imageio.ImageIO;


public class Brush {
	BufferedImage img;
	String path;
	int r;
	int g;
	int b;
	
	public Brush(String path, int size, int r, int g, int b){
		// TODO: stamp
		this.img = null;
		this.path = path;
		this.r = r;
		this.g = g;
		this.b = b;
		
		
		// TODO: SANTIZE THIS GODDAMN PATH FUUUUUUUUCK
		try{
			String src = Utils.getPathEclipseSucks(path);
			File img_file = new File(src);
			img = ImageIO.read(img_file);
		}catch(Exception ex){
			img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			Graphics graphics = img.createGraphics();
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, img.getWidth(), img.getHeight());
			graphics.dispose();
		}
	}
	
	public BufferedImage getImage(){
		return img;
	}
	
	public static BufferedImage setImageColor(BufferedImage img, int size, int r, int g, int b){
		BufferedImage imgnew = Utils.deepCopy(img);
		
		byte[] pixels = ((DataBufferByte) imgnew.getRaster().getDataBuffer()).getData();
		for (int i = 0; i < pixels.length; i+=4){
			//pixels[i] = pixels[i]; //alpha
			pixels[i+1] = (byte)r; //red
			pixels[i+2] = (byte)g; //green
			pixels[i+3] = (byte)b; // blue
		}
		return imgnew;
	}
	
	public boolean matches(String path, int size, int r, int g, int b){
		return this.path.equals(path) && this.img.getWidth() == size && this.r == r && this.g == g && this.b == b;
	}
}
