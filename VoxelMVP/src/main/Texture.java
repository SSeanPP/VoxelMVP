package main;

import static org.lwjgl.opengl.GL11.*;

import resourceLoader.TextureLoader;

public class Texture {
	
	public int textureId;
	public String texturePath;
	
	public Texture(String path) {
		this.texturePath = path;
		try {
			this.textureId = TextureLoader.loadTexture(path);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void cleanup() {
        glDeleteTextures(textureId);
    }
    
    public String getTexturePath() {
        return texturePath;
    }
}
