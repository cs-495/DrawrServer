package com.meow.kittypaintdev.server;

import java.awt.image.BufferedImage;


public class BrushObj {
	public class BrushColor {
		int r;
		int g;
		int b;
		public BrushColor(int r, int g, int b){
			this.r = r;
			this.g = g;
			this.b = b;
		}
	}
	
	BufferedImage img;
	String name;
	int size;
	int[] sizes;
	BufferedImage[] sized_images;
	BrushColor color;
	String type;
	boolean loaded;
	
	public BrushObj(String name, int size, BrushColor color, String type){
		this.img = null;
		this.name = name;
		this.size = size;
		this.sizes = null;
		this.sized_images = null;
		this.color = color;
		this.type = type;
		this.loaded = false;
	}
}
