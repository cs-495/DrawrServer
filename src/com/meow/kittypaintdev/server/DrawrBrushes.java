package com.meow.kittypaintdev.server;

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
	
	public Brush getStamp(String path, int size){
		for (Brush br : brush_cache){
			if(br.matches(path, size)) return br;
		}
		Brush newbr = new Brush(path, size);
		brush_cache.add(newbr);
		return newbr;
	}
	
	public Brush makeStamp(String dataUrl, byte[] data, int size){
		for (Brush br : brush_cache){
			if(br.matches(dataUrl, size)) return br;
		}
		Brush newbr = new Brush(dataUrl, data, size);
		brush_cache.add(newbr);
		return newbr;
	}
	
	////////////////////////////////////////////////////// here be dragons
	
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
