package com.mojang.rubydung;

import com.mojang.rubydung.level.Chunk;
import com.mojang.rubydung.level.Level;
import com.mojang.rubydung.level.LevelRenderer;
import com.mojang.util.GLAllocation;

import net.lax1dude.eaglercraft.EagRuntime;
import net.lax1dude.eaglercraft.internal.buffer.FloatBuffer;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.gluPerspective;

public class RubyDung implements Runnable {

    private final Timer timer = new Timer(60);

    private Level level;
    private LevelRenderer levelRenderer;
    private Player player;

    private final FloatBuffer fogColor = GLAllocation.createFloatBuffer(4);

    /**
     * Initialize the game.
     * Setup display, keyboard, mouse, rendering and camera
     *
     * @throws LWJGLException Game could not be initialized
     */
    public void init() throws LWJGLException {
        // Write fog color
        this.fogColor.put(new float[]{
                14 / 255.0F,
                11 / 255.0F,
                10 / 255.0F,
                255 / 255.0F
        }).flip();

        int width = Display.getWidth();
        int height = Display.getHeight();

        // Setup I/O
        Display.create();
        Keyboard.create();
        Mouse.create();

        // Setup rendering
        glEnable(GL_TEXTURE_2D);
        glShadeModel(GL_SMOOTH);
        glClearColor(0.5F, 0.8F, 1.0F, 0.0F);
        glClearDepth(1.0);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glDepthFunc(GL_LEQUAL);

        // Setup camera
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        gluPerspective(70, width / (float) height, 0.05F, 1000);
        glMatrixMode(GL_MODELVIEW);

        // Create level and player (Has to be in main thread)
        this.level = new Level(256, 256, 64);
        this.levelRenderer = new LevelRenderer(this.level);
        this.player = new Player(this.level);

        // Grab mouse cursor
        Mouse.setGrabbed(true);
    }

    /**
     * Destroy mouse, keyboard and display
     */
    public void destroy() {
        this.level.save();
        EagRuntime.destroy();
    }

    /**
     * Main game thread
     * Responsible for the game loop
     */
    @Override
    public void run() {
        try {
            // Initialize the game
            init();
        } catch (Exception e) {
            // Show error message dialog and stop the game
        	System.out.println("Failed to start RubyDung");
        	throw new RuntimeException(e);
        }

        // To keep track of framerate
        int frames = 0;
        long lastTime = System.currentTimeMillis();

        try {
            // Start the game loop
            while (!Keyboard.isKeyDown(1) && !Display.isCloseRequested()) {
                // Update the timer
                this.timer.advanceTime();

                // Call the tick to reach updates 20 per seconds
                for (int i = 0; i < this.timer.ticks; ++i) {
                    tick();
                }

                // Render the game
                render(this.timer.partialTicks);

                // Increase rendered frame
                frames++;

                // Loop if a second passed
                while (System.currentTimeMillis() >= lastTime + 1000L) {
                    // Print amount of frames
                    System.out.println(frames + " fps, " + Chunk.updates);

                    // Reset global rebuild stats
                    Chunk.updates = 0;

                    // Increase last time printed and reset frame counter
                    lastTime += 1000L;
                    frames = 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Destroy I/O and save game
            destroy();
        }
    }

    /**
     * Game tick, called exactly 20 times per second
     */
    private void tick() {
        this.player.tick();
    }

    /**
     * Move and rotate the camera to players location and rotation
     *
     * @param partialTicks Overflow ticks to calculate smooth a movement
     */
    private void moveCameraToPlayer(float partialTicks) {
        Player player = this.player;

        // Eye height
        glTranslatef(0.0f, 0.0f, -0.3f);

        // Rotate camera
        glRotatef(player.xRotation, 1.0f, 0.0f, 0.0f);
        glRotatef(player.yRotation, 0.0f, 1.0f, 0.0f);

        // Smooth movement
        double x = this.player.prevX + (this.player.x - this.player.prevX) * partialTicks;
        double y = this.player.prevY + (this.player.y - this.player.prevY) * partialTicks;
        double z = this.player.prevZ + (this.player.z - this.player.prevZ) * partialTicks;

        // Move camera to players location
        GL11.glTranslated(-x, -y, -z);
    }


    /**
     * Rendering the game
     *
     * @param partialTicks Overflow ticks to calculate smooth a movement
     */
    private void render(float partialTicks) {
        // Get mouse motion
        float motionX = Mouse.getDX();
        float motionY = Mouse.getDY();

        // Rotate the camera using the mouse motion input
        this.player.turn(motionX, motionY);

        // Clear color and depth buffer and reset the camera
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glLoadIdentity();

        // Move camera to middle of level
        moveCameraToPlayer(partialTicks);

        // Setup fog
        glEnable(GL_FOG);
        glFogi(GL_FOG_MODE, GL_LINEAR);
        glFogf(GL_FOG_START, -10);
        glFogf(GL_FOG_END, 20);
        glFog(GL_FOG_COLOR, this.fogColor);
        glDisable(GL_FOG);

        // Render bright tiles
        this.levelRenderer.render(0);

        // Enable fog to render shadow
        glEnable(GL_FOG);

        // Render dark tiles in shadow
        this.levelRenderer.render(1);

        // Finish rendering
        glDisable(GL_TEXTURE_2D);

        // Update the display
        Display.update();
    }

    /**
     * Entry point of the game
     *
     * @param args Program arguments (unused)
     */
    public static void main(String[] args) {
        new Thread(new RubyDung()).start();
    }
}
