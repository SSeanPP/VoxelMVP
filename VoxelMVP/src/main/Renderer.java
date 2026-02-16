package main;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import static org.lwjgl.opengl.GL11.*;

public class Renderer {
	public Renderer () {
		
	}
	
	public void render(GameState state) {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		Display.update();
	}
	
	public void initDisplay() throws LWJGLException {
        Display.setDisplayMode(new DisplayMode(1280, 720));
        Display.setTitle("LWJGL 2 Simple 3D Loop");
        Display.create();
    }
	
	public void cleanup(){
		Display.destroy();
	}
}
