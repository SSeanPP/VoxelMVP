package guiHandler;

import bufferManager.SceneBufferManager;
import imgui.ImGui;
import main.GameState;

public class F3Menu {
	public F3Menu() {
		
	}
	
	public void run(GameState state) {
		if(state.getF3state()) {
			ImGui.setNextWindowPos(100, 100);

			
			ImGui.begin("GPU Buffers");

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
