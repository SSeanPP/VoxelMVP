package main;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Entity {
	private final String id;
    private final String modelId;
    private Matrix4f modelMatrix;
    private Vector3f position;
    private Quaternionf rotation;
    private float scale;
    private Vector3f previousPosition;
    private Quaternionf previousRotation;
    private float previousScale;

    public Entity(String id, String modelId) {
        this.id = id;
        this.modelId = modelId;
        modelMatrix = new Matrix4f();
        position = new Vector3f();
        rotation = new Quaternionf();
        scale = 1;
        previousPosition = new Vector3f();
        previousRotation = new Quaternionf();
        previousScale = 1; 
    }
    
    public Entity(Entity input) {
    	this.id = input.id;
    	this.modelId = input.modelId;
    	this.modelMatrix = new Matrix4f(input.modelMatrix);
    	this.position = new Vector3f(input.position);
    	this.rotation = new Quaternionf(input.rotation);
    	this.scale = input.scale;
    }

    public String getId() {
        return id;
    }

    public String getModelId() {
        return modelId;
    }

    public Matrix4f getModelMatrix() {
        return modelMatrix;
    }

    public Vector3f getPosition() {
        return position;
    }
    
    public Vector3f getPreviousPosition() {
    	return previousPosition;
    }

    public Quaternionf getRotation() {
        return rotation;
    }
    
    public Quaternionf getPreviousRotation() {
    	return previousRotation;
    }

    public float getScale() {
        return scale;
    }
    
    public float getPreviousScale() {
    	return previousScale;
    }

    public void setPosition(float x, float y, float z) {
        position.x = x;
        position.y = y;
        position.z = z;
    }

    public void setRotation(float x, float y, float z, float angle) {
        this.rotation.fromAxisAngleRad(x, y, z, angle);
    }

    public void setScale(float scale) {
        this.scale = scale;
    }
    
    public void lerpPos(double alpha, Vector3f dest) {
    	dest.set(
                (float)(position.x * alpha + previousPosition.x * (1 - alpha)),
                (float)(position.y * alpha + previousPosition.y * (1 - alpha)),
                (float)(position.z * alpha + previousPosition.z * (1 - alpha))
            );
    }
    
    public void capturePrevious() {
        previousPosition.set(position);
        previousRotation.set(rotation);
        previousScale = scale;
    }
}
