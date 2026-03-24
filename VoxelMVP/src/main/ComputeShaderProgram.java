package main;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL43.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public class ComputeShaderProgram {
    public final int programId;
    private int computeShaderId;
    private final Map<String, Integer> uniforms = new HashMap<String, Integer>();
    private final FloatBuffer matrixBuffer = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

    public ComputeShaderProgram(String shaderCode) throws Exception {
        programId = glCreateProgram();
        if (programId == 0) throw new Exception("Could not create compute shader program");

        computeShaderId = glCreateShader(GL_COMPUTE_SHADER);
        if (computeShaderId == 0) throw new Exception("Could not create compute shader");

        glShaderSource(computeShaderId, shaderCode);
        glCompileShader(computeShaderId);
        if (glGetShaderi(computeShaderId, GL_COMPILE_STATUS) == 0)
            throw new Exception("Compute shader compile error: " + glGetShaderInfoLog(computeShaderId, 1024));

        glAttachShader(programId, computeShaderId);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0)
            throw new Exception("Compute shader link error: " + glGetProgramInfoLog(programId, 1024));

        glDetachShader(programId, computeShaderId);
        glDeleteShader(computeShaderId);
    }

    public void createUniform(String name) {
        int location = glGetUniformLocation(programId, name);
        if (location < 0) throw new RuntimeException("Could not find uniform [" + name + "]");
        uniforms.put(name, location);
    }

    public void setUniform(String name, Vector4f v) {
        Integer loc = uniforms.get(name);
        if (loc == null) throw new RuntimeException("Could not find uniform [" + name + "]");
        glUniform4f(loc, v.x, v.y, v.z, v.w);
    }
    
    public void setUniform(String name, int value) {
        Integer loc = uniforms.get(name);
        if (loc == null) throw new RuntimeException("Could not find uniform [" + name + "]");
        glUniform1i(loc, value);
    }

    public void bind() { glUseProgram(programId); }
    public void unbind() { glUseProgram(0); }
    public void cleanup() { unbind(); if (programId != 0) glDeleteProgram(programId); }
}