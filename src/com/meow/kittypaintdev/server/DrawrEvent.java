package com.meow.kittypaintdev.server;

import java.io.IOException;

public interface DrawrEvent {

	void update(int x, int y, byte[] bin_img) throws IOException;
	void update(int x, int y) throws IOException;
	
}
