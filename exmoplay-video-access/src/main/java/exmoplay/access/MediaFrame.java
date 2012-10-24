/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 24.10.2010
 */
package exmoplay.access;

import java.awt.image.BufferedImage;

public class MediaFrame {
    public final AudioBuffer audio;
    public final VideoBuffer video;
    boolean endOfMedia;
    double timestamp;

    public MediaFrame(AudioBuffer audio, VideoBuffer video) {
        this.audio = audio;
        this.video = video;
    }

    /*public double getPositionInSeconds(long frameNumber) {
        VideoFormat vf = (VideoFormat) this.video.getBuffer().getFormat();
        return frameNumber / vf.getFrameRate();
    }*/

    public boolean isEndOfMedia() {
        return endOfMedia;
    }

    /**
     * Only call this method, if you can be sure that this frame will no longer be used.
     */
    public void delete() {
        video.videoPicture.delete();
    }

    /**
     * @return the timestamp in milliseconds
     */
    public double getTimestamp() {
        return timestamp;
    }

    public int getSizeInBytes() {
        int audioBytes = this.audio.audioData.length;
        BufferedImage image = this.video.bufferedImage;
        int videoBytes = image.getWidth() * image.getHeight() * 4; // assuming 32 bit images
        return audioBytes + videoBytes;
    }
}
