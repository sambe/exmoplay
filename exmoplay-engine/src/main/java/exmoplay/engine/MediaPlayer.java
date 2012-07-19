/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 11.09.2011
 */
package exmoplay.engine;

import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.swing.SwingUtilities;

import exmoplay.access.MediaInfo;
import exmoplay.access.VideoFormat;
import exmoplay.engine.actorframework.Actor;
import exmoplay.engine.actorframework.MessageSendable;
import exmoplay.engine.actorframework.ObjectReceiver;
import exmoplay.engine.actorframework.RegisterForUpdates;
import exmoplay.engine.messages.CachedFrame;
import exmoplay.engine.messages.ControlCommand;
import exmoplay.engine.messages.ControlCommand.Command;
import exmoplay.engine.messages.CurrentScreen;
import exmoplay.engine.messages.FrameRequest;
import exmoplay.engine.messages.MediaError;
import exmoplay.engine.messages.MediaInfoRequest;
import exmoplay.engine.messages.MediaInfoResponse;
import exmoplay.engine.messages.NewVideo;
import exmoplay.engine.messages.PositionUpdate;
import exmoplay.engine.messages.SetPosition;
import exmoplay.engine.messages.StatusRequest;
import exmoplay.engine.messages.StatusResponse;
import exmoplay.engine.ui.ControlBar;
import exmoplay.engine.ui.VideoScreen;

public class MediaPlayer {

    private Actor errorHandler = new Actor(null, -1) {
        @Override
        protected void act(Object message) {
            if (message instanceof MediaError) {
                MediaError me = (MediaError) message;
                System.err.println("A media error occured: " + me.message);
                me.exception.printStackTrace();
            } else {
                System.err.println("A unknown type of error occured: " + message);
            }
        }
    };
    private Controller controller = new Controller(errorHandler);
    private VideoRenderer videoRenderer;
    private MediaInfoResponse mediaInfo;

    public MediaPlayer() {
        errorHandler.start();
        controller.start();
        videoRenderer = controller.getVideoRenderer();
    }

    public VideoScreen createScreen() {
        VideoScreen screen = new VideoScreen(videoRenderer);
        return screen;
    }

    public ControlBar createControlBar() {
        ControlBar controlBar = new ControlBar(controller);
        return controlBar;
    }

    public void setActiveScreen(VideoScreen screen) {
        videoRenderer.send(new CurrentScreen(screen));
    }

    public void openVideo(File file, MediaInfo mediaInfo) {
        openVideo(new NewVideo(file, mediaInfo));
    }

    public void openVideo(File file, MediaInfo mediaInfo, long initialPosition) {
        openVideo(new NewVideo(file, mediaInfo, initialPosition));
    }

    public void openVideo(File file, MediaInfo mediaInfo, long timerMin, long timerMax) {
        openVideo(new NewVideo(file, mediaInfo, timerMin, timerMax));
    }

    public void openVideo(File file, MediaInfo mediaInfo, long initialPosition, long timerMin, long timerMax) {
        openVideo(new NewVideo(file, mediaInfo, initialPosition, timerMin, timerMax));
    }

    private void openVideo(NewVideo message) {
        controller.send(message);
        ObjectReceiver r = new ObjectReceiver();
        controller.send(new MediaInfoRequest(r));
        mediaInfo = (MediaInfoResponse) r.waitForMessage();
    }

    public AudioFormat getAudioFormat() {
        return mediaInfo.audioFormat;
    }

    public VideoFormat getVideoFormat() {
        return mediaInfo.videoFormat;
    }

    public long getVideoDuration() {
        return mediaInfo.duration;
    }

    public long getPosition() {
        ObjectReceiver receiver = new ObjectReceiver();
        controller.send(new StatusRequest(receiver));
        StatusResponse response = (StatusResponse) receiver.waitForMessage();
        return response.position;
    }

    public void setPosition(long position) {
        controller.send(new SetPosition(position));
    }

    public void setPositionAnimated(long position) {
        controller.send(new SetPosition(position, true));
    }

    public void start() {
        controller.send(new ControlCommand(Command.START));
    }

    public void stop() {
        controller.send(new ControlCommand(Command.STOP));
    }

    public void addPositionListener(final PositionListener listener) {
        controller.send(new RegisterForUpdates(PositionUpdate.class, new MessageSendable() {
            @Override
            public void send(final Object message) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        listener.receive((PositionUpdate) message);
                    }
                });
            }
        }));
    }

    public void restrictPositionRange(Long min, Long max) {
        controller.send(new StatusRequest(min, max, null));
    }

    /**
     * Returns a frame (CachedFrame).
     * 
     * Very important: never forget to recycle() the returned frame (or you'll block a part of the cache forever)
     * 
     * @param seqNum
     * @return
     */
    public CachedFrame getFrame(long seqNum) {
        ObjectReceiver r = new ObjectReceiver();
        controller.send(new FrameRequest(seqNum, 1, false, r));
        return (CachedFrame) r.waitForMessage();
    }
}
