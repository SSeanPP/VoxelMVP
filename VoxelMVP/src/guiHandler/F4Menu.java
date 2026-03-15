package guiHandler;

import org.lwjgl.input.Mouse;

import imgui.ImGui;
import main.GameState;

public class F4Menu {

	private boolean wasGuiOpen = false;
	private boolean[] open = { true };
	
	F4Menu() {
		
	}
	
	public boolean run(GameState state) {
		if(state.getF4state()) {
			if (state.getF4state() != wasGuiOpen) {
		        Mouse.setGrabbed(!state.getF4state());
		        wasGuiOpen = state.getF4state();
		    }
			
			ImGui.showDemoWindow(open);
			return true;
		} else {
			if (state.getF4state() != wasGuiOpen) {
		        Mouse.setGrabbed(!state.getF4state());
		        wasGuiOpen = state.getF4state();
		    }
			return false;
		}
	}
}
