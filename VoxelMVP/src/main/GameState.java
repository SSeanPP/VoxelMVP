package main;

import java.util.ArrayList;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.input.Mouse;

public class GameState {
	
	// Purely manages positions of things - lives on thread. Not for rendering or game input
	private ArrayList<Entity> entities = new ArrayList<Entity>();
	private Camera camera;
	private Vector3f pos;
	private final float speed = 2.0f; // units per second
	
	private Vector3f forward = new Vector3f();
    private Vector3f right = new Vector3f();
    private Vector3f up = new Vector3f(0, 1, 0);
    private float yaw = 0.0f;
    
	public GameState() {
		camera = new Camera();
		
	}
	
	public void integrate(double t, double dt, InputState input) {
		//Should move camera in this state
		final float speed = 10.0f;
		
        for (Entity entity : entities) {
        	
        	entity.capturePrevious();
            Vector3f pos = entity.getPosition();
            /*
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
            */
            entity.setPosition(pos.x, pos.y, pos.z);
        	 
        }
        camera.setPreviousPosition();
        pos = camera.getPosition();
        
     // Get camera's forward/right/up vectors
         // world up

        // Calculate forward direction from yaw (rotation.y)
        yaw = camera.getRotation().y;
        
        forward.set(
            (float) Math.sin(yaw),
            0,
            (float) -Math.cos(yaw)
        );
        // Calculate right direction (cross product)
        forward.cross(up, right);

        // Move relative to camera orientation
        if (input.forward)  pos.add(forward.mul(speed * (float)dt, new Vector3f()));
        if (input.backward) pos.sub(forward.mul(speed * (float)dt, new Vector3f()));
        if (input.right)    pos.add(right.mul(speed * (float)dt, new Vector3f()));
        if (input.left)     pos.sub(right.mul(speed * (float)dt, new Vector3f()));
        if (input.up)       pos.y -= speed * dt;  // keep vertical movement in world space
        if (input.down)     pos.y += speed * dt;
        
        camera.setPosition(pos.x, pos.y, pos.z);
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
    
    public Camera getCamera() {
    	return camera;
    }
    
}
