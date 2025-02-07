/*
 * This file is part of VLCJ.
 *
 * VLCJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VLCJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VLCJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2025 Caprica Software Limited.
 */

package uk.co.caprica.vlcj.lwjglx.demo;

import org.lwjgl.opengl.*;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.videoengine.VideoEngine;
import uk.co.caprica.vlcj.player.embedded.videosurface.videoengine.VideoEngineCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.videoengine.VideoEngineCallbackAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.videoengine.VideoEngineVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.videoengine.VideoEngineWindowCallback;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class LwjglxDemo {

    private static VideoEngineCallback videoEngineCallback = new VideoEngineHandler();

    private static MediaPlayerFactory mediaPlayerFactory;

    private static EmbeddedMediaPlayer mediaPlayer;

    /**
     * Video surface for the media player.
     */
    private static VideoEngineVideoSurface videoSurface;

    private static VideoEngineWindowCallback windowCallback;

    private static MyAWTGLCanvas canvas;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Specify an MRL");
            System.exit(0);
        }

        mediaPlayerFactory = new MediaPlayerFactory();

        mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
        videoSurface = mediaPlayerFactory.videoSurfaces().newVideoSurface(VideoEngine.libvlc_video_engine_opengl, videoEngineCallback);
        mediaPlayer.videoSurface().set(videoSurface);

        JFrame f = new JFrame("Dorpal");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLayout(new BorderLayout());
        GLData data = new GLData();
        canvas = new MyAWTGLCanvas(data);
        f.add(canvas, BorderLayout.CENTER);
        f.add(new JLabel("Ooooh"), BorderLayout.NORTH);
        f.setSize(800, 800);
        f.setVisible(true);
        f.transferFocus();

        // Resize is flickery, again this is just proof-of-concept, probably some synchronisation is missing or something
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (windowCallback == null) {
                    return;
                }
                Component c = e.getComponent();
                windowCallback.setSize(c.getWidth(), c.getHeight());
            }
        });

        Runnable renderLoop = new Runnable() {
            @Override
            public void run() {
                if (!canvas.isValid()) {
                    GL.setCapabilities((null));
                    return;
                }
                canvas.render();
                // Some sort of delay is required here
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                SwingUtilities.invokeLater(this);
            }
        };
        SwingUtilities.invokeLater(renderLoop);

        // We have to allow at least one render loop to create the context, clearly this is a hack, but a better solution will be possible
        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            mediaPlayer.media().play(args[0]);
        }).start();
    }

    private static class VideoEngineHandler extends VideoEngineCallbackAdapter {
        @Override
        public void onSetWindowCallback(VideoEngineWindowCallback windowCallback) {
            LwjglxDemo.windowCallback = windowCallback;
            windowCallback.setSize(canvas.getWidth(), canvas.getHeight());
        }

        @Override
        public long onGetProcAddress(Long opaque, String functionName) {
            return getFunctionProvider().getFunctionAddress(functionName);
        }

        @Override
        public boolean onMakeCurrent(Long opaque, boolean enter) {
            if (enter) {
                return canvas.enter();
            } else {
                return canvas.exit();
            }
        }

        @Override
        public void onSwap(Long opaque) {
            canvas.swapBuffers();
        }
    }

    private static class MyAWTGLCanvas extends AWTGLCanvas {

        public MyAWTGLCanvas(GLData data) {
            super(data);
        }

        @Override
        public void initGL() {
            createCapabilities();
            glClearColor(0.0f, 0.0f, 0.0f, 1);
        }

        @Override
        public void paintGL() {
            // Painting is handled by the LibVLC callback invoking canvas.swapBuffers()
        }

        public boolean enter() {
            this.platformCanvas.makeCurrent(this.context);
            return true;
        }

        public boolean exit() {
            this.platformCanvas.makeCurrent(0);
            return true;
        }
    }
}
