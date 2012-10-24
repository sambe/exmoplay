/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on May 20, 2012
 */
package exmoplay.access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class MediaInfo {

    public final List<Long> keyFrameTimestamps;
    public final List<AudioSamplesInfo> audioSamplesInfo;
    public final List<VideoPictureInfo> videoPictureInfo;
    public final double videoPacketTimeBase;
    public final double audioPacketTimeBase;
    public final double pictureTimeBase;
    public final double samplesTimeBase;
    public final int audioFrameSize;
    public final double videoFrameRate;

    private SortedMap<Long, AudioSamplesInfo> audioSamplesInfoBySamplesOffset;

    public MediaInfo(List<Long> keyFrameTimestamps, List<AudioSamplesInfo> audioSamplesInfo,
            List<VideoPictureInfo> videoPictureInfo, double videoPacketTimeBase, double audioPacketTimeBase,
            double pictureTimeBase, double samplesTimeBase, int audioFrameSize, double videoFrameRate) {
        this.keyFrameTimestamps = Collections.unmodifiableList(new ArrayList<Long>(keyFrameTimestamps));
        this.audioSamplesInfo = Collections.unmodifiableList(new ArrayList<AudioSamplesInfo>(audioSamplesInfo));
        this.videoPictureInfo = Collections.unmodifiableList(new ArrayList<VideoPictureInfo>(videoPictureInfo));
        this.videoPacketTimeBase = videoPacketTimeBase;
        this.audioPacketTimeBase = audioPacketTimeBase;
        this.pictureTimeBase = pictureTimeBase;
        this.samplesTimeBase = samplesTimeBase;
        this.audioFrameSize = audioFrameSize;
        this.videoFrameRate = videoFrameRate;
    }

    public long findRelevantKeyframeTimestamp(long targetValue) {
        if (targetValue < 0)
            return 0;
        int low = 0;
        int high = keyFrameTimestamps.size() - 1;
        if (high == -1)
            return targetValue; // just return value itself as a fallback
        while (low < high) {
            int median = low + (high - low + 1) / 2;
            long valueAtMedian = keyFrameTimestamps.get(median);
            if (valueAtMedian > targetValue)
                high = median - 1;
            else if (valueAtMedian < targetValue)
                low = median;
            else
                return targetValue; // exact match
        }
        return keyFrameTimestamps.get(low);
    }

    public static class AudioSamplesInfo {
        public final int nr;
        public final long timestamp;
        public final long samplesOffset;
        public final int samplesLength;

        public AudioSamplesInfo(int nr, long timestamp, long samplesOffset, int samplesLength) {
            this.nr = nr;
            this.timestamp = timestamp;
            this.samplesOffset = samplesOffset;
            this.samplesLength = samplesLength;
        }
    }

    public static class VideoPictureInfo {
        public final int nr;
        public final long timestamp;
        public final boolean key;

        public VideoPictureInfo(int nr, long timestamp, boolean key) {
            this.nr = nr;
            this.timestamp = timestamp;
            this.key = key;
        }
    }

    public AudioSamplesInfo findAudioSamplesInfoContainingOffset(long offset) {
        if (audioSamplesInfoBySamplesOffset == null) {
            audioSamplesInfoBySamplesOffset = new TreeMap<Long, MediaInfo.AudioSamplesInfo>();
            for (AudioSamplesInfo info : audioSamplesInfo) {
                audioSamplesInfoBySamplesOffset.put(info.samplesOffset, info);
            }
        }

        Long key = audioSamplesInfoBySamplesOffset.headMap(offset + 1).lastKey();
        return audioSamplesInfoBySamplesOffset.get(key);
    }

    public VideoPictureInfo findVideoPictureInfoByFrameNumber(int frameNr) {
        if (frameNr > videoPictureInfo.size())
            return null;
        return videoPictureInfo.get(frameNr);
    }
}
