package com.meow.kittypaintdev.server;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;

import com.meow.kittypaintdev.server.DrawrEvent;

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
	private static byte[] blank_chunk_png;
	List<DrawrEvent> clients;
	
	public DrawrServerMap() throws IOException{
		chunk_block_size = 256;
		clients = new ArrayList<DrawrEvent>();
		
		//Hash of chunks - not array because we need negative and positive location & skippin
		chunks = new HashMap<Integer, HashMap<Integer, DrawrServerChunk>>();
		
		for (int i = -1; i < 2; ++i){
			for (int j = -1; j < 2; ++j){
				loadChunkForce(i, j);
			}
		}
		
		loadBlankPng();
		
		// create chunk cacheing thread
		new Thread(){
			public void run(){
				while(true){
					try {
						//System.out.println("n=" + clients.size());
						updateChunkCache();
						Thread.sleep(250);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}
	
	public void addClient(DrawrEvent ev){
		clients.add(ev);
	}
	public void removeClient(Integer eventid){
		Iterator<DrawrEvent> it = clients.iterator();
		while(it.hasNext()){
			DrawrEvent ev = it.next();
			//System.out.println("{"+eventid+" =!!! "+ev.getId()+"}");
			if(ev.getId() == eventid){
				it.remove();
			}
		}
		/*for(DrawrEvent e : clients){
			if(e==ev){
				System.out.println("{"+e+" =!!! "+ev+"}");
			}
		}
		System.out.print("("+clients.size()+"->");
		clients.remove(ev);
		System.out.println(clients.size()+")");*/
	}
	
	public void loadBlankPng() throws IOException{
		BufferedImage im = new BufferedImage(chunk_block_size, chunk_block_size, BufferedImage.TYPE_INT_ARGB);
		Graphics g = im.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, im.getWidth(), im.getHeight());
		g.dispose();
		ByteArrayOutputStream bstream = new ByteArrayOutputStream();
		ImageIO.write(im, "png", bstream);
		blank_chunk_png = bstream.toByteArray();
	}
	
	public boolean isChunkLoaded(int numx, int numy){
		if (chunks.containsKey(numx) &&
				chunks.get(numx).containsKey(numy) &&
				chunks.get(numx).get(numy) != null){
			return true;
		}
		return false;
	}
	
	public DrawrServerChunk loadChunkForce(int numx, int numy) throws IOException{
		// this will work differently later
		if(chunks_loaded > max_chunks) return null;
		chunks_loaded++;
		
		DrawrServerChunk c = loadChunk(numx, numy);
		if(c == null){
			synchronized(chunks){
				chunks.get(numx).put(numy, new DrawrServerChunk(this, numx, numy, null));
			}
		}
		return chunks.get(numx).get(numy);
	}
	
	public DrawrServerChunk loadChunk(int numx, int numy){
		// only create a chunk if there's a file for it
		// otherwise, don't create a chunk and return null
		if(chunks_loaded > max_chunks) return null;
		chunks_loaded++;
		
		synchronized(chunks){
			if (!chunks.containsKey(numx)){
				chunks.put(numx, new HashMap<Integer, DrawrServerChunk>());
			}
		}
		
		try{
			String src = Utils.getPathInAssets("chunks/chunk" + numx + "x" + numy + ".png");
			File img = new File(src);
			BufferedImage chunk_im = ImageIO.read(img);
			synchronized(chunks){
				chunks.get(numx).put(numy, new DrawrServerChunk(this, numx, numy, chunk_im));
			}
			return chunks.get(numx).get(numy);
		}catch(Exception ex){
			return null;
		}
	}
	
	public void updateChunkCache() throws IOException{
		// update chunk's cache, and send UPDATEs to clients for all changed chunks
		synchronized(chunks){
			for(Map.Entry<Integer, HashMap<Integer, DrawrServerChunk>> entry : chunks.entrySet()){
				int x = entry.getKey();
				for(Map.Entry<Integer, DrawrServerChunk> ch : entry.getValue().entrySet()){
					int y = ch.getKey();
					if(ch.getValue().updateCache()){ // if it was updated, update clients
						//sendUpdateClients(x, y, ch.getValue().getPngImage());//websockets doesn't support binary data yet
						sendUpdateClients(x, y);
					}
				}
			}
		}
	}
	
	public void sendUpdateClients(int numx, int numy) throws IOException{
		for(DrawrEvent client : clients){
			//client.update(numx, numy, bin_img); //websockets doesn't support binary data yet
			client.update(numx, numy);
		}
	}
	
	public void sendUpdateClients(int numx, int numy, byte[] bin_img) throws IOException{
		for(DrawrEvent client : clients){
			//client.update(numx, numy, bin_img); //websockets doesn't support binary data yet
			client.update(numx, numy, bin_img);
		}
	}
	
	public byte[] getChunkPng(int numx, int numy) throws IOException{
		if(!isChunkLoaded(numx, numy)){
			DrawrServerChunk c = loadChunk(numx, numy);
			if(c != null){
				return c.getPngImage();
			}
		}else{
			return chunks.get(numx).get(numy).getPngImage();
		}
		return blank_chunk_png;
	}
	
	// DRAWING
	
	public int[][] getChunksAffected(int gamex, int gamey, Brush brush, int size){
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
			int chunk_numx = (int) Math.floor(brush_xs[i] / (double)this.chunk_block_size);
			int chunk_numy = (int) Math.floor(brush_ys[i] / (double)this.chunk_block_size);
			chunks_found[i] = new int[]{chunk_numx, chunk_numy};
		}
		return chunks_found;
	}
	
	public int[][] getChunkLocalCoordinates(int gamex, int gamey, int[][] chunk_nums_affected, Brush brush){
		/*calculate pixel location in local coordinates of each of the 4 possible chunks.
        * getChunksAffected will always return in this order: topleft, bottomleft, topright, bottomright 
        * Preserve this order in this return
        * this function will probably explode if brush size > this.chunk_block_size. that should never happen.*/
		
		//These are correct for the chunk where the CENTER OF THE BRUSH is
		int chunk_general_localx = Utils.mod(gamex, this.chunk_block_size);
		int chunk_general_localy = Utils.mod(gamey, this.chunk_block_size);
		
		//Calculate which chunk the CENTER OF THE BRUSH is in
		int chunk_numx = (int) Math.floor(gamex / (double)this.chunk_block_size);
		int chunk_numy = (int) Math.floor(gamey / (double)this.chunk_block_size);
		
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
	
	public void addPoint(int gamex, int gamey, Brush brush, int size) throws IOException{
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
					DrawrServerChunk chunk;
					if (!isChunkLoaded(chunk_numx, chunk_numy)){
						chunk = loadChunkForce(chunk_numx, chunk_numy);
					}else{
						chunk = chunks.get(chunk_numx).get(chunk_numy);
					}
					
					if(chunk != null){
						chunk.addPoint(chunks_local_coords[i][0], chunks_local_coords[i][1], brush, size);
					}
					
					chunks_written.add(chunk_written_id);
				}
			}
		}
	}
}
