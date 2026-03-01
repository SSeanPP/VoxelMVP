package main;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Camera {

    private Vector3f direction;
    private Vector3f position;
    private Vector3f previousPosition;
    private Vector3f right;
    private Vector2f rotation;
    private Vector2f previousRotation;
    private Vector3f up;
    private Matrix4f viewMatrix;
    private final float MOUSE_SENSITIVITY = 0.35f;

    public Camera() {
        direction = new Vector3f();
        right = new Vector3f();
        up = new Vector3f();
        previousPosition = new Vector3f();
        position = new Vector3f();
        viewMatrix = new Matrix4f();
        rotation = new Vector2f();
        previousRotation = new Vector2f();
    }

    public void addRotation(float x, float y) {
    	
        rotation.add(x, y);
        recalculate();
    }
    
    public Vector2f getRotation() {
    	return rotation;
    }

    public Vector3f getPosition() {
        return position;
    }
    
    public void setPreviousPosition() {
    	previousPosition.set(position);
    }

    public Matrix4f handleCameraLerpAndMatrix(double alpha, Vector3f vec) {
        lerpPos(alpha, vec);
        
        viewMatrix.identity()
            .rotateX(rotation.x)
            .rotateY(rotation.y)
            .translate(-vec.x, -vec.y, -vec.z);
        
        return viewMatrix;
    }
    
    public Matrix4f getViewMatrix() {
    	return viewMatrix;
    }

    public void moveBackwards(float inc) {
        viewMatrix.positiveZ(direction).negate().mul(inc);
        position.sub(direction);
        recalculate();
    }

    public void moveDown(float inc) {
        viewMatrix.positiveY(up).mul(inc);
        position.sub(up);
        recalculate();
    }

    public void moveForward(float inc) {
        viewMatrix.positiveZ(direction).negate().mul(inc);
        position.add(direction);
        recalculate();
    }

    public void moveLeft(float inc) {
        viewMatrix.positiveX(right).mul(inc);
        position.sub(right);
        recalculate();
    }

    public void moveRight(float inc) {
        viewMatrix.positiveX(right).mul(inc);
        position.add(right);
        recalculate();
    }

    public void moveUp(float inc) {
        viewMatrix.positiveY(up).mul(inc);
        position.add(up);
        recalculate();
    }

    private void recalculate() {
        viewMatrix.identity()
                .rotateX(rotation.x)
                .rotateY(rotation.y)
                .translate(-position.x, -position.y, -position.z);
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        //recalculate();
    }

    public void setRotation(float x, float y) {
        rotation.set(x, y);
        //recalculate();
    }
    
    public void lerpPos(double alpha, Vector3f dest) {
    	dest.set(
                (float)(position.x * alpha + previousPosition.x * (1 - alpha)),
                (float)(position.y * alpha + previousPosition.y * (1 - alpha)),
                (float)(position.z * alpha + previousPosition.z * (1 - alpha))
            );
    }
    
    public void updateCameraMatrix(float x, float y) {
    	addRotation(
                (float) Math.toRadians(-y * MOUSE_SENSITIVITY),
                (float) Math.toRadians(x * MOUSE_SENSITIVITY));
    }
}