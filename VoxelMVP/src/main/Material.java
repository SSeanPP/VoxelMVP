package main;

public class Material {
	
    private Texture textureReference;

    public Material(Texture texture) {
        textureReference = texture;
    }
   
    public Texture getTexture() {
    	return textureReference;
    }
}
