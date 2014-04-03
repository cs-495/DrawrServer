package com.meow.kittypaintdev.server;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;

public class DrawrServerMap {
	static int max_chunks = 500;
	static int chunks_loaded = 0;
	
	
	/*ALL CHUNKS SHOULD BE LOADED AT ALL TIMES???
	 * I DUNNO
	 * CAN EASILY UNLOAD AND RELOAD FROM THE CHUNK PNGS
	 * BUT HOW DO WE DECIDE WHEN WE CAN UNLOAD THEM???
	 */
	int chunk_block_size;
	HashMap<Integer, HashMap<Integer, DrawrServerChunk>> chunks;
	
	public DrawrServerMap(){
		chunk_block_size = 256;
		
		//Hash of chunks - not array because we need negative and positive location & skippin
		chunks = new HashMap<Integer, HashMap<Integer, DrawrServerChunk>>();
		
		for (int i = -1; i < 2; ++i){
			for (int j = -1; j < 2; ++j){
				loadChunk(i, j);
			}
		}
	}
	
	public boolean isChunkLoaded(int numx, int numy){
		if (chunks.containsKey(numx) &&
				chunks.get(numx).containsKey(numy) &&
				chunks.get(numx).get(numy) != null){
			return true;
		}
		return false;
	}
	
	public void loadChunk(int numx, int numy){
		// this will work differently later
		if(chunks_loaded > max_chunks) return;
		chunks_loaded++;
		
		if (!chunks.containsKey(numx)){
			chunks.put(numx, new HashMap<Integer, DrawrServerChunk>());
		}
		
		try{
			String src = Utils.getPathEclipseSucks("chunks/chunk" + numx + "x" + numy + ".png");
			File img = new File(src);
			BufferedImage chunk_im = ImageIO.read(img);
			chunks.get(numx).put(numy, new DrawrServerChunk(this, numx, numy, chunk_im));
		}catch(Exception ex){
			chunks.get(numx).put(numy, new DrawrServerChunk(this, numx, numy, null));
		}
	}
	
	public void updateChunkCache(){
		// TODO:
		// loop through chunks, do .updateCache();
		// make a new thread and call this function every 0.1 seconds? i guess
	}
	
	public int[][] getChunksAffected(int gamex, int gamey, BrushObj brush, int size){
		/*To find chunks affected: find 1 or more chunks for each 4 points of the square mask of the brush
        * getChunksAffected will always return in this order: topleft, bottomleft, topright, bottomright
        * if one of those 4 chunks isn't loaded, its location in the return array will be null*/
		
		//Preserve the order of the return value!
		int[][] chunks_found = {null, null, null, null};
		int brush_delta = size/2;
		//Coordinates of the 4 coordinates of the brush, in the correct order
		int[] brush_xs = {gamex-brush_delta, gamex-brush_delta, gamex+brush_delta, gamex+brush_delta};
		int[] brush_ys = {gamey-brush_delta, gamey+brush_delta, gamey-brush_delta, gamey+brush_delta};
		
		for (int i = 0; i < 4; ++i){
			//Calculate which chunk this pixel is in
			int chunk_numx = brush_xs[i] / this.chunk_block_size;
			int chunk_numy = brush_ys[i] / this.chunk_block_size;
			if (isChunkLoaded(chunk_numx, chunk_numy)){
				chunks_found[i] = new int[]{chunk_numx, chunk_numy};
			}
		}
		return chunks_found;
	}
	
	public int[][] getChunkLocalCoordinates(int gamex, int gamey, int[][] chunk_nums_affected, BrushObj brush){
		/*calculate pixel location in local coordinates of each of the 4 possible chunks.
        * getChunksAffected will always return in this order: topleft, bottomleft, topright, bottomright 
        * Preserve this order in this return
        * this function will probably explode if brush size > this.chunk_block_size. that should never happen.*/
		
		//These are correct for the chunk where the CENTER OF THE BRUSH is
		int chunk_general_localx = gamex % this.chunk_block_size;
		int chunk_general_localy = gamey % this.chunk_block_size;
		
		//Calculate which chunk the CENTER OF THE BRUSH is in
		int chunk_numx = gamex / this.chunk_block_size;
		int chunk_numy = gamey / this.chunk_block_size;
		
		int[][] chunk_local_coords = {null, null, null, null};
		for (int i = 0; i < 4; ++i){
			if (chunk_nums_affected[i] != null){
				int dx = chunk_numx - chunk_nums_affected[i][0]; //instead of 'x'
				int dy = chunk_numy - chunk_nums_affected[i][1]; //instead of 'y'
				chunk_local_coords[i] = new int[]{
						chunk_general_localx + dx * this.chunk_block_size,
						chunk_general_localy + dy * this.chunk_block_size};
			}
		}
		return chunk_local_coords;
	}
	
	public void addPoint(int gamex, int gamey, BrushObj brush, int size){
		int[][] chunks_affected = getChunksAffected(gamex, gamey, brush, size);
		int[][] chunks_local_coords = getChunkLocalCoordinates(gamex, gamey, chunks_affected, brush);
		
		//Store the chunks already written to, to avoid redundancy
		ArrayList<String> chunks_written =  new ArrayList<String>();
		
		for (int i = 0; i < 4; ++i){
			if (chunks_affected[i] != null && chunks_local_coords[i] != null){
				int chunk_numx = chunks_affected[i][0]; //instead of 'x'
				int chunk_numy = chunks_affected[i][1]; //instead of 'y'
				String chunk_written_id = chunk_numx + ":" + chunk_numy;
				if (!chunks_written.contains(chunk_written_id)){
					if (!isChunkLoaded(chunk_numx, chunk_numy))
						loadChunk(chunk_numx, chunk_numy);
					
					DrawrServerChunk chunk = chunks.get(""+chunk_numx).get(""+chunk_numy);
					chunk.addPoint(chunks_local_coords[i][0], chunks_local_coords[i][1], brush, size);
					
					chunks_written.add(chunk_written_id);
				}
			}
		}
	}
}
