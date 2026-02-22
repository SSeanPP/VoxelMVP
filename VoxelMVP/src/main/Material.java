package main;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Material {
	
    private Texture textureReference;

    public Material(Texture texture) {
        textureReference = texture;
    }
   
    public Texture getTexture() {
    	return textureReference;
    }
}
