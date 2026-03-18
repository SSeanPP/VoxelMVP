package guiHandler;

import bufferManager.SceneBufferManager;
import imgui.ImGui;
import main.GameState;
import main.Settings;

public class F3Menu {
	private double renderStartTime;
	private double timeTaken;
	private boolean hasFinished = false;
	
	private static int fps;
	private static int frames;
	private static long fpsTimer;
	
	
	Runtime rt = Runtime.getRuntime();
    long usedMB;
    
	public F3Menu() {
		fpsTimer = System.currentTimeMillis();
	    frames = 0;
	    fps = 0;
	}
	
	public void run(GameState state, int totalIndices, int totalVertices) {
		if(state.getF3state()) {
			
			usedMB = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
			
			ImGui.setNextWindowPos(10, 10);

			
			ImGui.begin("Meshing");
			
			ImGui.text("Meshing Progress");
			/*
			 * 
			 * if (MeshQueue.Empty && !hasFinished) {
				timeTaken = (System.nanoTime() - renderStartTime) / 1000000000.0;
				hasFinished=true;
			} else if (!hasFinished) {
				timeTaken = (System.nanoTime() - renderStartTime) / 1000000000.0;
			}
			
			ImGui.text("Time to mesh = "+ (timeTaken));
			 * 
			 * */
			
			
			ImGui.separator();
			
			ImGui.text("VBO");
			float vboFill = 1.0f - (float) SceneBufferManager.vboFreeBytes / Settings.VBO_SIZE_BYTES;
			ImGui.progressBar(vboFill, String.format("%.1f / "+Settings.VBO_SIZE_BYTES/1048576+ " MiB", (Settings.VBO_SIZE_BYTES - SceneBufferManager.vboFreeBytes) / (1024f * 1024f)));
			ImGui.text("Free regions: " + SceneBufferManager.vboFreeRegions);

			ImGui.separator();

			ImGui.text("EBO");
			float eboFill = 1.0f - (float) SceneBufferManager.eboFreeBytes / Settings.EBO_SIZE_BYTES;
			ImGui.progressBar(eboFill, String.format("%.1f / "+Settings.EBO_SIZE_BYTES /1048576+ " MiB", (Settings.EBO_SIZE_BYTES - SceneBufferManager.eboFreeBytes) / (1024f * 1024f)));
			ImGui.text("Free regions: " + SceneBufferManager.eboFreeRegions);

			ImGui.end();
			
			ImGui.setNextWindowPos(210, 10);
			ImGui.begin("General Data");
			ImGui.text("Used MB: " + usedMB);
			frames++;
        	if (System.currentTimeMillis() - fpsTimer >= 1000) {
        	    fps = frames;
        	    frames = 0;
        	    fpsTimer += 1000;
        	}
        	
        	ImGui.text("FPS: " + fps);
        	ImGui.text("Vertices: " + totalVertices+" Indices: "+totalIndices);
			ImGui.end();
		}
	}
	
	/*
	 * ImGui.begin("Test Window");

			if (ImGui.button("Click me!")) {
			    System.out.println("Button pressed!");
			}
			
			ImGui.end();
	 */
}
