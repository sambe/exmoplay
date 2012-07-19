/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.10.2010
 */
package exmoplay.engine.messages;

import java.io.File;

import exmoplay.access.MediaInfo;

public class NewVideo {

    public final File videoFile;
    public final MediaInfo mediaInfo;
    public final Long initialPosition; // in milliseconds
    public final Long positionMin;
    public final Long positionMax;

    public NewVideo(File mediaFile, MediaInfo mediaInfo) {
        this(mediaFile, mediaInfo, null);
    }

    public NewVideo(File mediaFile, MediaInfo mediaInfo, Long initialPosition) {
        this(mediaFile, mediaInfo, initialPosition, null, null);
    }

    public NewVideo(File mediaFile, MediaInfo mediaInfo, Long positionMin, Long positionMax) {
        this(mediaFile, mediaInfo, null, positionMin, positionMax);
    }

    public NewVideo(File mediaFile, MediaInfo mediaInfo, Long initialPosition, Long positionMin, Long positionMax) {
        this.videoFile = mediaFile;
        this.mediaInfo = mediaInfo;
        this.initialPosition = initialPosition;
        this.positionMin = positionMin;
        this.positionMax = positionMax;
    }

}
