package guiHandler;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import imgui.ImGui;
import imgui.ImInput;
import main.GameState;

public class GUIHelper {
	
	private F3Menu f3Menu;
	private F4Menu f4Menu;
	
	private boolean isMouseCaptured;
	
	public GUIHelper() {
		f3Menu = new F3Menu();
		f4Menu = new F4Menu();
		isMouseCaptured = false;
	}
	
	//Returns True if mouseCaptured, returns false if not
	public boolean runGUI(GameState state, int totalIndices, int totalVertices) {
		
		ImInput.handleMouseAndScroll();
		ImGui.newFrame();
		
		f3Menu.run(state, totalIndices, totalVertices);
		isMouseCaptured = f4Menu.run(state);
		
		GL11.glEnable(GL11.GL_BLEND);
		GL14.glBlendEquation(GL14.GL_FUNC_ADD);
		GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

		ImGui.render();
		
		GL11.glDisable(GL11.GL_BLEND);
		
		return isMouseCaptured;
	}
}
