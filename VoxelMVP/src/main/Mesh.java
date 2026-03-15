package main;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

//Depreciated.

public class Mesh {
	private int numVertices;
    private int vaoId;
    private List<Integer> vboIdList;
    private Material material;

    public Mesh(float[] positions, int[] indices, Material material) {
        this.numVertices = indices.length;
        this.material = material;
        vboIdList = new ArrayList<Integer>();

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        
        // 5 floats * sizeof(float)
        int stride = 5 * 4;
        
        // Positions VBO
        int vboId = glGenBuffers();
        vboIdList.add(vboId);
        FloatBuffer positionsBuffer = ByteBuffer.allocateDirect(positions.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        positionsBuffer.put(positions).flip();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, positionsBuffer, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3*4);

        // Index VBO
        vboId = glGenBuffers();
        vboIdList.add(vboId);
        IntBuffer indicesBuffer = ByteBuffer.allocateDirect(indices.length * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        indicesBuffer.put(indices).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        glBindVertexArray(0);

        indicesBuffer.clear();
        indicesBuffer = null;
        
        positionsBuffer.clear();
        positionsBuffer = null;
    }

    public void cleanup() {
        for(Integer i : vboIdList) {
        	glDeleteBuffers(i);
        };
        glDeleteVertexArrays(vaoId);
    }

    public int getNumVertices() {
        return numVertices;
    }

    public final int getVaoId() {
        return vaoId;
    }
    
    public Material getMaterial() {
    	return this.material;
    }
}
