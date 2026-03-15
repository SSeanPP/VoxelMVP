package guiHandler;

import imgui.ImGui;
import main.GameState;

public class F3Menu {
	public F3Menu() {
		
	}
	
	public void run(GameState state) {
		if(state.getF3state()) {
			ImGui.setNextWindowPos(100, 100);

			ImGui.begin("Test Window");

			if (ImGui.button("Click me!")) {
			    System.out.println("Button pressed!");
			}
			ImGui.end();
		}
	}
}
