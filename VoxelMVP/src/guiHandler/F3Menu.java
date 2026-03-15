package guiHandler;

import bufferManager.SceneBufferManager;
import imgui.ImGui;
import main.GameState;
import main.WorldMap;

public class F3Menu {
	private double renderStartTime = System.nanoTime();
	private double timeTaken;
	private boolean hasFinished = false;
	public F3Menu() {
		
	}
	
	public void run(GameState state) {
		if(state.getF3state()) {
			ImGui.setNextWindowPos(100, 100);

			
			ImGui.begin("GPU Buffers");
			
			ImGui.text("Meshing Progress");
			if (WorldMap.allMeshed() && !hasFinished) {
				timeTaken = (System.nanoTime() - renderStartTime) / 1000000000.0;
				hasFinished=true;
			} else if (!hasFinished) {
				timeTaken = (System.nanoTime() - renderStartTime) / 1000000000.0;
			}
			
			ImGui.text("Time to mesh = "+ (timeTaken));
			
			ImGui.separator();
			
			ImGui.text("VBO");
			float vboFill = 1.0f - (float) SceneBufferManager.vboFreeBytes / SceneBufferManager.bufferSize;
			ImGui.progressBar(vboFill, String.format("%.1f / 256 MB", (SceneBufferManager.bufferSize - SceneBufferManager.vboFreeBytes) / (1024f * 1024f)));
			ImGui.text("Free regions: " + SceneBufferManager.vboFreeRegions);

			ImGui.separator();

			ImGui.text("EBO");
			float eboFill = 1.0f - (float) SceneBufferManager.eboFreeBytes / SceneBufferManager.bufferSize;
			ImGui.progressBar(eboFill, String.format("%.1f / 256 MB", (SceneBufferManager.bufferSize - SceneBufferManager.eboFreeBytes) / (1024f * 1024f)));
			ImGui.text("Free regions: " + SceneBufferManager.eboFreeRegions);

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
