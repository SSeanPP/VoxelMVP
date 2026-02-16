package main;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

public class Renderer {
	public Renderer () {
		
	}
	
	public static void initDisplay() throws LWJGLException {
        Display.setDisplayMode(new DisplayMode(1280, 720));
        Display.setTitle("LWJGL 2 Simple 3D Loop");
        Display.create();
    }
	
	public static void cleanup(){
		Display.destroy();
	}
}
