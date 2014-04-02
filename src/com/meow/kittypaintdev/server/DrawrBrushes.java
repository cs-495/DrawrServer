package com.meow.kittypaintdev.server;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

public class DrawrBrushes {
	
	
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
	
	public static void setBrushColor(BrushObj brush, int r, int g, int b){
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
	
	public static BufferedImage getBrushImg(BrushObj brush, int size){
		if (brush.type == "brush"){
			int index = Utils.index_of(brush.sizes, size);
			if (index >= 0){
				return brush.sized_images[index];
			}
		}
		return brush.img;
	}
}
