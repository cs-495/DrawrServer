package com.meow.kittypaintdev.server;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.List;

public class DrawrBrushes {
	
	List<Brush> brush_cache;
	
	public DrawrBrushes(){
		brush_cache = new ArrayList<Brush>();
		// TODO: make it so brushes timeout of the cache, so we don't have like millions of them in random colors
	}
	
	public Brush getBrush(String path, int size, int r, int g, int b){
		for(Brush br : brush_cache){
			if(br.matches(path, size, r, g, b)) return br;
		}
		Brush newbr = new Brush(path, size, r, g, b);
		brush_cache.add(newbr);
		return newbr;
	}
	
	////////////////////////////////////////////////////// here be dragons
	
	//Hope this actually works by reference benji says yes
	//THIS METHOD ASSUMES BUFFEREDIMG TYPE ARGB
	//MAKE IT SO BRUSH_IMGS ACTUALLY HAVE THIS TYPE!!!
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
	
	/*public static void setBrushColor(Brush brush, int r, int g, int b){
		if (r < 0 || g < 0 || b < 0 || r > 255 || g > 255 || b > 255) return;
		brush.color.r = r;
		brush.color.g = g;
		brush.color.b = b;
		
		for (int i = 0; i < brush.sized_images.length; ++i){
			BufferedImage img = setImageColor(brush.sized_images[i], brush.sizes[i], 
					brush.color.r, brush.color.g, brush.color.b);
			brush.sized_images[i] = img;
		}
	}
	
	public static BufferedImage getBrushImg(Brush brush, int size){
		if (brush.type == "brush"){
			int index = Utils.index_of(brush.sizes, size);
			if (index >= 0){
				return brush.sized_images[index];
			}
		}
		return brush.img;
	}*/
}
