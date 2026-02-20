package main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector3f;

public class GameState {
	
	private Map<String, Model> modelMap = new HashMap<String, Model>();
	//Camera camera
	public GameState() {
		
	}
	
	public GameState(Projection old) {
		
	}
	
	public GameState copy() {
	    GameState copy = new GameState();
	    
	    // Deep copy each model and its entities
	    for (Map.Entry<String, Model> entry : this.modelMap.entrySet()) {
	        String modelId = entry.getKey();
	        Model originalModel = entry.getValue();
	        
	        // Create new model with same mesh list (meshes can be shared)
	        Model copiedModel = new Model(modelId, originalModel.getMeshList());
	        
	        // Deep copy each entity
	        for (Entity originalEntity : originalModel.getEntitiesList()) {
	            Entity copiedEntity = copyEntity(originalEntity);
	            copiedModel.getEntitiesList().add(copiedEntity);
	        }
	        
	        copy.modelMap.put(modelId, copiedModel);
	    }
	    
	    return copy;
	}
	
	public void integrate(double t, double dt, InputState input) {
		//Should move camera in this state
		float speed = 5.0f; // units per second
	    
	    for (Model model : modelMap.values()) {
	        for (Entity entity : model.getEntitiesList()) {
	            Vector3f pos = entity.getPosition();
	            
	            if (input.forward) {
	                pos.z -= speed * dt;
	            }
	            if (input.backward) {
	                pos.z += speed * dt;
	            }
	            if (input.left) {
	                pos.x -= speed * dt;
	            }
	            if (input.right) {
	                pos.x += speed * dt;
	            }
	            if (input.up) {
	                pos.y += speed * dt;
	            }
	            if (input.down) {
	                pos.y -= speed * dt;
	            }
	            
	            entity.setPosition(pos.x, pos.y, pos.z);
	            entity.updateModelMatrix(); // <-- rebuild matrix
	        }
	    }
	}
	
	public static GameState lerp(GameState previous, GameState current, double alpha) {
		GameState result = new GameState();
	    // Iterate through current state's models (this is what exists NOW)
	    for (Map.Entry<String, Model> currentEntry : current.modelMap.entrySet()) {
	        String modelId = currentEntry.getKey();
	        Model currentModel = currentEntry.getValue();
	        Model previousModel = previous.modelMap.get(modelId);
	        
	        // Create result model with same mesh list (meshes don't change)
	        Model resultModel = new Model(modelId, currentModel.getMeshList());
	        
	        // Lerp entities
	        for (Entity currentEntity : currentModel.getEntitiesList()) {
	            Entity resultEntity;
	            
	            // Try to find this entity in previous state
	            Entity previousEntity = null;
	            if (previousModel != null) {
	                previousEntity = findEntityById(previousModel.getEntitiesList(), currentEntity.getId());
	            }
	            
	            if (previousEntity != null) {
	                // Entity exists in both states - lerp position
	                resultEntity = lerpEntity(previousEntity, currentEntity, alpha);
	            } else {
	                // Entity is new (wasn't in previous) - just use current position
	                resultEntity = copyEntity(currentEntity);
	            }
	            
	            resultModel.getEntitiesList().add(resultEntity);
	        }
	        
	        result.modelMap.put(modelId, resultModel);
	    }
	    
	    return result;
	}
	
	public void addEntity(Entity entity) {
        String modelId = entity.getModelId();
        Model model = modelMap.get(modelId);
        if (model == null) {
            throw new RuntimeException("Could not find model [" + modelId + "]");
        }
        model.getEntitiesList().add(entity);
    }

    public void addModel(Model model) {
        modelMap.put(model.getId(), model);
    }
    
    public void addBoth(Model model, Entity entity) {
    	modelMap.put(model.getId(), model);
    	model.getEntitiesList().add(entity);
    }

    public void cleanup() {
        for (Model model : modelMap.values()) {
        	model.cleanup();
        }
    }

    public Map<String, Model> getModelMap() {
        return modelMap;
    }
    
    private static Entity findEntityById(List<Entity> entities, String id) {
        for (Entity e : entities) {
            if (e.getId().equals(id)) {
                return e;
            }
        }
        return null;
    }

    private static Entity lerpEntity(Entity prev, Entity curr, double alpha) {
        Entity result = new Entity(curr.getId(), curr.getModelId());
        
        // Lerp position
        Vector3f prevPos = prev.getPosition();
        Vector3f currPos = curr.getPosition();
        result.setPosition(
            (float)(currPos.x * alpha + prevPos.x * (1 - alpha)),
            (float)(currPos.y * alpha + prevPos.y * (1 - alpha)),
            (float)(currPos.z * alpha + prevPos.z * (1 - alpha))
        );
        
        // For rotation/scale, just use current for now (proper quaternion slerp is more complex)
        result.setScale(curr.getScale());
        result.updateModelMatrix();
        
        return result;
    }

    private static Entity copyEntity(Entity entity) {
        Entity copy = new Entity(entity.getId(), entity.getModelId());
        Vector3f pos = entity.getPosition();
        copy.setPosition(pos.x, pos.y, pos.z);
        copy.setScale(entity.getScale());
        copy.updateModelMatrix();
        return copy;
    }
}
