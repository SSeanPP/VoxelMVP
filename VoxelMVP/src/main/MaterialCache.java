package main;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MaterialCache {
	private Map<String, Material> materialCache;

    public MaterialCache() {
        this.materialCache = new HashMap<String, Material>();
    }
    
    public void cleanup() {
        Iterator<Map.Entry<String, Material>> it = materialCache.entrySet().iterator();
        
        while(it.hasNext()) {
        	//Map.Entry<String, Material> entry = it.next();
        	//entry.getValue().cleanup();
        	it.next();
        	it.remove();
        }
    }

    public Material createMaterial(String materialName, Texture texture) {
        if(!materialCache.containsKey(materialName)) {
        	Material material = new Material(texture);
        	materialCache.put(materialName, material);
        	return material;
        } else {
        	return materialCache.get(materialName);
        }
    }

    public Material getMaterial(String materialName) {
    	if(materialCache.containsKey(materialName)) {
    		return materialCache.get(materialName);
        } else {
        	throw new RuntimeException("No material!");
        }
    }
}
