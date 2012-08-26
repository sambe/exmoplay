/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.10.2010
 */
package exmoplay.engine;

import java.io.File;

import javax.sound.sampled.AudioFormat;

import exmoplay.access.MediaInfo;
import exmoplay.access.VideoFormat;
import exmoplay.access.XugglerMediaInputStream;
import exmoplay.engine.actorframework.Actor;
import exmoplay.engine.messages.CacheBlock;
import exmoplay.engine.messages.CachedFrame;
import exmoplay.engine.messages.FetchFrames;
import exmoplay.engine.messages.MediaInfoRequest;
import exmoplay.engine.messages.MediaInfoResponse;

public class FrameFetcher extends Actor {
    private static final boolean DEBUG = false;
    private static final boolean TRACE = false;

    private XugglerMediaInputStream mediaInputStream;
    private final File mediaFile;
    private final MediaInfo mediaInfo;
    private double frameRate = 0;

    public FrameFetcher(Actor errorHandler, File mediaFile, MediaInfo mediaInfo) {
        super(errorHandler, -1, Priority.NORM);
        this.mediaFile = mediaFile;
        this.mediaInfo = mediaInfo;
    }

    @Override
    protected void init() throws Exception {
        mediaInputStream = new XugglerMediaInputStream(mediaFile, mediaInfo);
        frameRate = mediaInputStream.getVideoFormat().getFrameRate();
    }

    @Override
    protected void destruct() {
        mediaInputStream.close();
    }

    @Override
    protected void act(Object message) {
        if (message instanceof FetchFrames) {
            handleFetchFrames((FetchFrames) message);
        } else if (message instanceof MediaInfoRequest) {
            handleMediaInfoRequest((MediaInfoRequest) message);
        } else {
            throw new IllegalStateException("received unknown message");
        }
    }

    private void handleFetchFrames(FetchFrames message) {
        long startMillis = System.currentTimeMillis();
        CacheBlock block = message.block;
        // set position and find seq nr
        long position = (long) (1000.0 * block.baseSeqNum / frameRate);
        long actualPosition = mediaInputStream.setPosition(position);
        if (TRACE) {
            System.out.println("TRACE: " + block.baseSeqNum + ": fetching at position " + position
                    + "ms and was positionioned at " + actualPosition + "ms (difference " + (actualPosition - position)
                    + "ms)");
        }
        long afterSetPositionMillis = System.currentTimeMillis();
        long startSeqNr = block.baseSeqNum; //Math.round(newPosition * frameRate);

        // read frames from stream
        for (CachedFrame f : block.frames) {
            // allocating a buffer if the cachedFrame does not bring one already
            if (f.frame == null) {
                f.frame = mediaInputStream.createFrame();
            }
            mediaInputStream.readFrame(f.frame);
            f.seqNum = startSeqNr++;
        }
        message.responseTo.send(block);
        long endMillis = System.currentTimeMillis();
        double seekingSeconds = (afterSetPositionMillis - startMillis) / 1000.0;
        double totalSeconds = (endMillis - startMillis) / 1000.0;
        if (DEBUG) {
            System.out.println("DEBUG: " + block.baseSeqNum + ": total fetch time " + totalSeconds + "s (seeking "
                    + seekingSeconds + "s)");
        }
    }

    private void handleMediaInfoRequest(MediaInfoRequest message) {
        VideoFormat videoFormat = mediaInputStream.getVideoFormat();
        AudioFormat audioFormat = mediaInputStream.getAudioFormat();
        long duration = mediaInputStream.getDuration() / 1000L;
        MediaInfoResponse response = new MediaInfoResponse(videoFormat, audioFormat, duration);
        message.responseTo.send(response);
    }
}
