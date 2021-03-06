/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 10.04.2011
 */
package exmoplay.engine;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.SwingUtilities;

import exmoplay.engine.actorframework.Actor;
import exmoplay.engine.messages.CachedFrame;
import exmoplay.engine.messages.CurrentScreen;
import exmoplay.engine.messages.ImageRequest;
import exmoplay.engine.messages.ImageResponse;
import exmoplay.engine.messages.Repaint;
import exmoplay.engine.ui.VideoScreen;

public class VideoRenderer extends Actor {

    private VideoScreen currentScreen;

    private CachedFrame currentFrame;

    private Image currentImage;

    public VideoRenderer(Actor errorHandler) {
        super(errorHandler, -1, Priority.MAX);
    }

    @Override
    protected void act(Object message) {
        if (message instanceof CachedFrame) {
            CachedFrame frame = (CachedFrame) message;
            if (!frame.frame.isEndOfMedia()) {
                if (currentFrame != null) {
                    currentFrame.recycle();
                    currentFrame = null;
                }
                currentFrame = frame;
                currentImage = currentFrame.frame.video.getImage();
                paintImageOnScreen(currentScreen, currentImage);
            } else {
                frame.recycle();
            }
        } else if (message instanceof CurrentScreen) {
            currentScreen = ((CurrentScreen) message).screen;
        } else if (message instanceof Repaint) {
            Repaint repaint = (Repaint) message;
            paintImageOnScreen(repaint.screen, currentImage);
        } else if (message instanceof ImageRequest) {
            ImageRequest request = (ImageRequest) message;
            request.responseTo.send(new ImageResponse(currentImage));
        } else {
            throw new IllegalArgumentException("unknown type of message: " + message.getClass().getName());
        }

    }

    private static void paintImageOnScreen(final VideoScreen screen, final Image image) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Graphics2D g2d = (Graphics2D) screen.getGraphics();
                paintImageOnScreen(g2d, screen, image);
            }
        });
    }

    public static void paintImageOnScreen(Graphics2D g2d, VideoScreen screen, Image image) {
        if (g2d != null) {
            if (image == null) {
                paintBlackRectangle(screen, g2d);
            } else {
                paintImage(screen, g2d, image);
            }
        }
    }

    private static void paintBlackRectangle(VideoScreen screen, Graphics g) {
        int x = 0;
        int y = 0;
        int width = screen.getWidth();
        int height = screen.getHeight();

        g.setColor(Color.BLACK);
        g.fillRect(x, y, width, height);
    }

    private static void paintImage(VideoScreen screen, Graphics2D g, Image image) {
        int x = 0;
        int y = 0;
        int width = screen.getWidth();
        int height = screen.getHeight();

        g.setColor(Color.BLACK);
        if (width * 3 > height * 4) { // if wider than 4:3
            int d = (width - height * 4 / 3);
            x += d / 2;
            width -= d;
            g.fillRect(0, 0, d / 2, height);
            g.fillRect(width + d / 2, 0, (d + 1) / 2, height);
        } else { // if higher than 4:3
            int d = (height - width * 3 / 4);
            y += d / 2;
            height -= d;
            g.fillRect(0, 0, width, d / 2);
            g.fillRect(0, height + d / 2, width, (d + 1) / 2);
        }
        // Waaayyyy tooo slow!!!
        //g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, x, y, width, height, null);
    }

}
