/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 24.04.2011
 */
package exmoplay.engine.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JComponent;

import exmoplay.engine.VideoRenderer;
import exmoplay.engine.actorframework.ObjectReceiver;
import exmoplay.engine.messages.CurrentScreen;
import exmoplay.engine.messages.ImageRequest;
import exmoplay.engine.messages.ImageResponse;

@SuppressWarnings("serial")
public class VideoScreen extends JComponent {

    private final VideoRenderer videoRenderer;

    private boolean active;

    public VideoScreen(VideoRenderer videoRenderer) {
        this.videoRenderer = videoRenderer;
        setBackground(Color.BLACK);
        setIgnoreRepaint(true);
        setDoubleBuffered(true);
        setOpaque(true);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                System.out.println("DEBUG: " + VideoScreen.this + " on top now.");
                VideoScreen.this.videoRenderer.send(new CurrentScreen(VideoScreen.this));
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (videoRenderer != null) {
            ObjectReceiver receiver = new ObjectReceiver();
            videoRenderer.send(new ImageRequest(receiver));
            ImageResponse imageResponse = (ImageResponse) receiver.waitForMessage();
            VideoRenderer.paintImageOnScreen((Graphics2D) g, this, imageResponse.image);
        }
    }

    /**
     * @return the connected
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @param connected the connected to set
     */
    public void setActive(boolean connected) {
        this.active = connected;
    }
}
