package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector3f;

public class GameState {
	
	private ArrayList<Entity> entities = new ArrayList<Entity>();
	//Camera camera
	public GameState() {
		
	}
	
	public void integrate(double t, double dt, InputState input) {
		//Should move camera in this state
		float speed = 5.0f; // units per second
	    
	    
        for (Entity entity : entities) {
        	
        	entity.capturePrevious();
            Vector3f pos = entity.getPosition();
            
            if (input.forward) {
                pos.z += speed * dt;
            }
            if (input.backward) {
                pos.z -= speed * dt;
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
        }
	    
	}
	
    public void cleanup() {
        
    }
    
    public ArrayList<Entity> getEntityList() {
    	return entities;
    }
    
    public void addEntity(Entity entity) {
    	entities.add(entity);
    }
    
    public void removeEntity(Entity entity) {
    	
    }
}
