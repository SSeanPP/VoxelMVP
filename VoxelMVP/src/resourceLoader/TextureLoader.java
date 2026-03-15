package resourceLoader;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

public class TextureLoader {

    public static int loadTexture(String path) throws Exception {
    	//System.out.println("Current directory: " + new File(".").getAbsolutePath());
    	InputStream is = ResourceLoader.class.getClassLoader().getResourceAsStream(path);
    	BufferedImage image = ImageIO.read(is);

        int width = image.getWidth();
        int height = image.getHeight();

        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);

        // Flip vertically (OpenGL expects bottom-left origin)
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {

                int pixel = pixels[y * width + x];

                buffer.put((byte)((pixel >> 16) & 0xFF)); // R
                buffer.put((byte)((pixel >> 8) & 0xFF));  // G
                buffer.put((byte)(pixel & 0xFF));         // B
                buffer.put((byte)((pixel >> 24) & 0xFF)); // A
            }
        }

        buffer.flip();

        int textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8,
                width, height,
                0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        glBindTexture(GL_TEXTURE_2D, 0);
        
        return textureID;
    }
}
