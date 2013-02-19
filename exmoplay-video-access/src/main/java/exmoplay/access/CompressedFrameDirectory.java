/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Dec 27, 2012
 */
package exmoplay.access;

import java.util.ArrayList;
import java.util.List;

public class CompressedFrameDirectory {

    public static class Video {
        private int[] compressedTimestamps;
        private int[] compressedKeyFrames;

        private Video(int[] compressedTimestamps, int[] compressedKeyFrames) {
            this.compressedTimestamps = compressedTimestamps;
            this.compressedKeyFrames = compressedKeyFrames;
        }

        public List<MediaInfo.VideoPictureInfo> decompress() {
            int[] diffedTimestamps = uncompress(compressedTimestamps);
            int[] diffedKeyFrames = uncompress(compressedKeyFrames);
            long[] timestamps = longUndiff(diffedTimestamps);
            int[] keyFrames = undiff(diffedKeyFrames);

            List<MediaInfo.VideoPictureInfo> ret = new ArrayList<MediaInfo.VideoPictureInfo>(timestamps.length);
            int k = 0;
            for (int i = 0; i < timestamps.length; i++) {
                boolean key = false;
                if (k < keyFrames.length && keyFrames[k] == i) {
                    key = true;
                    k++;
                }
                ret.add(new MediaInfo.VideoPictureInfo(i, timestamps[i], key));
            }
            return ret;
        }
    }

    public static Video compressVideo(List<MediaInfo.VideoPictureInfo> pictureInfo) {
        long[] timestamps = new long[pictureInfo.size()];
        List<Integer> keyFrames = new ArrayList<Integer>();
        for (int i = 0; i < pictureInfo.size(); i++) {
            timestamps[i] = pictureInfo.get(i).timestamp;
            if (pictureInfo.get(i).key)
                keyFrames.add(i);
        }
        int[] keyFramesArray = new int[keyFrames.size()];
        for (int i = 0; i < keyFrames.size(); i++) {
            keyFramesArray[i] = keyFrames.get(i);
        }

        int[] diffedTimestamps = longDiff(timestamps);
        int[] diffedKeyFrames = diff(keyFramesArray);
        int[] compressedTimestamps = compress(diffedTimestamps);
        int[] compressedKeyFrames = compress(diffedKeyFrames);

        return new Video(compressedTimestamps, compressedKeyFrames);
    }

    public static class Audio {
        private int[] compressedTimestamps;
        private int[] compressedSampleOffsets;
        private int[] compressedSampleLengths;

        public Audio(int[] compressedTimestamps, int[] compressedSampleOffsets, int[] compressedSampleLengths) {
            this.compressedTimestamps = compressedTimestamps;
            this.compressedSampleOffsets = compressedSampleOffsets;
            this.compressedSampleLengths = compressedSampleLengths;
        }

        public List<MediaInfo.AudioSamplesInfo> decompress() {
            int[] diffedTimestamps = uncompress(compressedTimestamps);
            int[] diffedSampleOffsets = uncompress(compressedSampleOffsets);
            int[] sampleLengths = uncompress(compressedSampleLengths);
            long[] timestamps = longUndiff(diffedTimestamps);
            long[] sampleOffsets = longUndiff(diffedSampleOffsets);

            List<MediaInfo.AudioSamplesInfo> ret = new ArrayList<MediaInfo.AudioSamplesInfo>(timestamps.length);
            for (int i = 0; i < timestamps.length; i++) {
                ret.add(new MediaInfo.AudioSamplesInfo(i, timestamps[i], sampleOffsets[i], sampleLengths[i]));
            }
            return ret;
        }

    }

    public static Audio compressAudio(List<MediaInfo.AudioSamplesInfo> audioInfo) {
        long[] timestamps = new long[audioInfo.size()];
        long[] sampleOffsets = new long[audioInfo.size()];
        int[] sampleLengths = new int[audioInfo.size()];
        for (int i = 0; i < audioInfo.size(); i++) {
            timestamps[i] = audioInfo.get(i).timestamp;
            sampleOffsets[i] = audioInfo.get(i).samplesOffset;
            sampleLengths[i] = audioInfo.get(i).samplesLength;
        }
        int[] diffedTimestamps = longDiff(timestamps);
        int[] diffedSampleOffsets = longDiff(sampleOffsets);

        int[] compressedTimestamps = compress(diffedTimestamps);
        int[] compressedSampleOffsets = compress(diffedSampleOffsets);
        int[] compressedSampleLengths = compress(sampleLengths);

        return new Audio(compressedTimestamps, compressedSampleOffsets, compressedSampleLengths);
    }

    public static int[] diff(int[] list) {
        if (list.length == 0)
            return list;
        int[] ret = new int[list.length];
        ret[0] = (int) list[0];
        for (int i = 1; i < list.length; i++) {
            ret[i] = (int) (list[i] - list[i - 1]);
        }
        return ret;
    }

    public static int[] undiff(int[] diffList) {
        if (diffList.length == 0)
            return diffList;
        int[] ret = new int[diffList.length];
        ret[0] = diffList[0];
        for (int i = 1; i < diffList.length; i++) {
            ret[i] = ret[i - 1] + diffList[i];
        }
        return ret;
    }

    public static int[] longDiff(long[] list) {
        if (list.length == 0) {
            return new int[0];
        }

        int[] ret = new int[list.length];
        if (list[0] < Integer.MIN_VALUE || list[0] > Integer.MAX_VALUE)
            throw new IllegalArgumentException(
                    "unable to diff, because list starts with a value that is outside of integer range");
        ret[0] = (int) list[0];
        for (int i = 1; i < list.length; i++) {
            ret[i] = (int) (list[i] - list[i - 1]);
        }
        return ret;
    }

    public static long[] longUndiff(int[] diffList) {
        if (diffList.length == 0) {
            return new long[0];
        }

        long[] ret = new long[diffList.length];
        ret[0] = diffList[0];
        for (int i = 1; i < diffList.length; i++) {
            ret[i] = ret[i - 1] + diffList[i];
        }
        return ret;
    }

    public static int[] compress(int[] data) {
        int n = 1;
        for (int i = 1; i < data.length; i++) {
            if (data[i] != data[i - 1])
                n++;
        }
        int[] ret = new int[2 * n];
        ret[0] = data.length;
        if (data.length != 0)
            ret[1] = data[0];
        int k = 2;
        for (int i = 1; i < data.length; i++) {
            if (data[i] != data[i - 1]) {
                ret[k] = i;
                ret[k + 1] = data[i];
                k += 2;
            }
        }
        return ret;
    }

    public static int[] uncompress(int[] compressedData) {
        int[] ret = new int[compressedData[0]];
        int current = compressedData[1];
        int k = 2;
        for (int i = 0; i < ret.length; i++) {
            if (k < compressedData.length && i == compressedData[k]) {
                current = compressedData[k + 1];
                k += 2;
            }
            ret[i] = current;
        }
        return ret;
    }
}
