package com.meow.kittypaintdev.server;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class DrawrServerChunk {
	int numx;
	int numy;
	BufferedImage chunk_im;
	Graphics g;
	byte[] chunkPngCache;
	boolean changedSinceLastCache;
	
	public DrawrServerChunk(DrawrServerMap drawr_map, int numx, int numy, BufferedImage chunk_im){
		//Create image not canvas
		int size = drawr_map.chunk_block_size;
		if (chunk_im == null){
			this.chunk_im = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			g = this.chunk_im.createGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, this.chunk_im.getWidth(), this.chunk_im.getHeight());
			//g.dispose();
		}else{
			this.chunk_im = chunk_im;
			g = this.chunk_im.createGraphics();
		}
		
		this.numx = numx;
		this.numy = numy;
		this.chunkPngCache = null;
		this.changedSinceLastCache = true;
	}
	
	public void addPoint(int local_x, int local_y, BrushObj brush, int size){
		BufferedImage brush_img = DrawrBrushes.getBrushImg(brush, size);
		
		int s = (int)Math.floor(size/2.0);
		
		//http://examples.javacodegeeks.com/desktop-java/awt/image/drawing-on-a-buffered-image/
		g.drawImage(brush_img, local_x-s, local_y-s, null);
		changedSinceLastCache = true;
	}
	
	public void updatePngCache() throws IOException{
		if(changedSinceLastCache){
			ByteArrayOutputStream bstream = new ByteArrayOutputStream();
			ImageIO.write(chunk_im, "png", bstream);
			chunkPngCache = bstream.toByteArray();
		}
	}
	
	@SuppressWarnings("unused")
	public boolean updateCache() throws IOException{
		//Output to png file to be server with HTTP
		//Every 0.1 seconds or so? <--
		
		updatePngCache();
		
		if(false){
			try{
				File output_file = new File("chunks/chunk" + numx + "x" + numy + ".png");
				ImageIO.write(chunk_im, "png", output_file);
				return true;
			}catch(Exception ex){
				return false;
			}
		}
		return false;
	}
	
	public byte[] getPngImage(){
		return chunkPngCache;
	}
}
