package main;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class TextureCache {
	private Map<Integer, Texture> textureMap;

    public TextureCache() {
        this.textureMap = new HashMap<Integer, Texture>();
    }
    
    public void cleanup() {
        Iterator<Map.Entry<Integer, Texture>> it = textureMap.entrySet().iterator();
        
        while(it.hasNext()) {
        	Map.Entry<Integer, Texture> entry = it.next();
        	entry.getValue().cleanup();
        	it.remove();
        }
    }

    public Texture createTexture(int key, String texturePath) {
        if(!textureMap.containsKey(key)) {
        	Texture newTex = new Texture(texturePath);
            textureMap.put(key, newTex);
            return newTex;
        } else {
        	return textureMap.get(key);
        }
    }

    public Texture getTexture(int key, String texturePath) {
    	if(textureMap.containsKey(key)) {
    		return textureMap.get(key);
        } else {
        	throw new RuntimeException("No texture!");
        }
    }
}
