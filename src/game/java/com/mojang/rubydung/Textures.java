package com.mojang.rubydung;

import java.io.IOException;
import java.io.InputStream;

import org.lwjgl.opengl.GL11;

import com.mojang.util.GLAllocation;

import net.lax1dude.eaglercraft.EagRuntime;
import net.lax1dude.eaglercraft.internal.buffer.ByteBuffer;
import net.lax1dude.eaglercraft.internal.buffer.IntBuffer;
import net.lax1dude.eaglercraft.opengl.ImageData;

public class Textures {

    private static int lastId = Integer.MIN_VALUE;

    /**
     * Load a texture into OpenGL
     *
     * @param resourceName Resource path of the image
     * @param mode         Texture filter mode (GL_NEAREST, GL_LINEAR)
     * @return Texture id of OpenGL
     */
    public static int loadTexture(String resourceName, int mode) {
        // Generate a new texture id
        IntBuffer e = GLAllocation.createIntBuffer(1);
        GL11.glGenTextures(e);
        int id = e.get(0);

        // Bind this texture id
        bind(id);

        // Set texture filter mode
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mode);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mode);

        // Read from resources
        InputStream inputStream = EagRuntime.getResourceStream(resourceName);

        // Read to buffered image
		ImageData bufferedImage = ImageData.loadImageFile(inputStream).swapRB();

		// Get image size
		int width = bufferedImage.getWidth();
		int height = bufferedImage.getHeight();

		// Write image pixels into array
		int[] pixels = new int[width * height];
		bufferedImage.getRGB(0, 0, width, height, pixels, 0, width);

		// Flip RGB order of the integers
		for (int i = 0; i < pixels.length; i++) {
		    int alpha = pixels[i] >> 24 & 0xFF;
		    int red = pixels[i] >> 16 & 0xFF;
		    int green = pixels[i] >> 8 & 0xFF;
		    int blue = pixels[i] & 0xFF;

		    // ARGB to ABGR
		    pixels[i] = alpha << 24 | blue << 16 | green << 8 | red;
		}

		// Create bytebuffer from pixel array
		ByteBuffer byteBuffer = GLAllocation.createByteBuffer(width * height * 4);
		byteBuffer.asIntBuffer().put(pixels);

		// Write texture to opengl
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, byteBuffer);

        return id;
    }

    /**
     * Bind the texture to OpenGL using the id from {@link #loadTexture(String, int)}
     *
     * @param id Texture id
     */
    public static void bind(int id) {
        if (id != lastId) {
        	GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
            lastId = id;
        }
    }
}