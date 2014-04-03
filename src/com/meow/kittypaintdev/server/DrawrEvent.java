package com.meow.kittypaintdev.server;

import java.io.IOException;

public interface DrawrEvent {
	
	void update(int x, int y) throws IOException;
	
}
