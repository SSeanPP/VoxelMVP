package main;

import java.util.ArrayList;
import java.util.List;

public class Model {
	private final String id;
    private List<Entity> entitiesList;
    private List<Mesh> meshList;

    public Model(String id, List<Mesh> meshList) {
        this.id = id;
        this.meshList = meshList;
        entitiesList = new ArrayList<Entity>();
    }

    public void cleanup() {
        for (Mesh mesh : meshList) {
        	mesh.cleanup();
        };
    }

    public List<Entity> getEntitiesList() {
        return entitiesList;
    }
    
    public String getId() {
        return id;
    }

    public List<Mesh> getMeshList() {
        return meshList;
    }
}
