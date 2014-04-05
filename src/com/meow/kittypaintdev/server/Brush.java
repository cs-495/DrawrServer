package com.meow.kittypaintdev.server;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;


public class Brush {
	BufferedImage img;
	String path;
	int r;
	int g;
	int b;
	
	public Brush(String path, int size, int r, int g, int b){
		img = null;
		this.path = path;
		this.r = r; this.g = g; this.b = b;
			
		try{
			Pattern pat = Pattern.compile("^brushes\\/[a-z0-9A-Z]+\\/[a-z0-9A-Z]+\\.png$");
			Matcher m = pat.matcher(path);

			if(!m.matches()){
				throw new Exception("invalid path");
			}
			
			if(r >= 0 && g >= 0 && b >= 0 && r < 256  && g < 256  && b < 256 ){
				String src = Utils.getPathInAssets(path);
				File img_file = new File(src);
				img = ImageIO.read(img_file);
				img = setImageColor(img, size, r, g, b);
			}else{
				throw new Exception("invalid arguments");
			}
		}catch(Exception ex){
			System.out.println("unknown brush requested: |" + path + "|");
			img = new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR);
			Graphics graphics = img.createGraphics();
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, img.getWidth(), img.getHeight());
			graphics.setColor(Color.RED);
			graphics.drawString("?", 5, 5);
			graphics.dispose();
		}
	}
	
	public Brush(String path, int size){
		img = null;
		this.path = path;
		r = -1; g = -1; b = -1;
		
		try{
			String src = Utils.getPathInAssets(path);
			File img_file = new File(src);
			img = ImageIO.read(img_file);
			
			//Scale the stamp upward!!
			int orig_size = img.getWidth();
			if (orig_size < size){
				double scale = (double)size/(double)orig_size;
				BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR);
				AffineTransform at = new AffineTransform();
				at.scale(scale, scale);
				AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
				scaled = scaleOp.filter(img, scaled);
				img = scaled;
			}
		}catch(Exception ex){
			img = new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR);
			Graphics graphics = img.createGraphics();
			graphics.setColor(Color.CYAN);
			graphics.fillRect(0, 0, img.getWidth(), img.getHeight());
			graphics.drawString("?", 5, 5);
			graphics.dispose();
		}
	}
	
	public BufferedImage getImage(){
		return img;
	}
	
	//THIS METHOD ASSUMES BUFFEREDIMG TYPE ABGR
	public static BufferedImage setImageColor(BufferedImage img, int size, int r, int g, int b){
		BufferedImage imgnew = img; //edit the old image/pass by reference //Utils.deepCopy(img);
		
		byte[] pixels = ((DataBufferByte) imgnew.getRaster().getDataBuffer()).getData();
		for (int i = 0; i < pixels.length; i+=4){
			//pixels[i] = pixels[i]; //alpha
			pixels[i+1] = (byte)b; //blue
			pixels[i+2] = (byte)g; //green
			pixels[i+3] = (byte)r; // red
		}
		return imgnew;
	}
	
	public boolean matches(String path, int size, int r, int g, int b){
		return this.path.equals(path) && this.img.getWidth() == size && this.r == r && this.g == g && this.b == b;
	}
	
	public boolean matches(String path, int size){
		return this.path.equals(path) && this.img.getWidth() == size;
	}
}
